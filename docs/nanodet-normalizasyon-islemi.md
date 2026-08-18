
# NanoDet normalizasyon işlemi

## 1. Amaç ve kapsam

Bu doküman, mevcut Android NanoDet akışında bir RGBA görüntünün ONNX Runtime'a gönderilmeden önce nasıl normalize edildiğini açıklar. Açıklama özellikle RGBA_NATIVE_BITMAP ön işleme yolunu, kaynak kodu ve gerçek cihaz benchmark logunu temel alır.

Kapsanan konular:

- normalizasyonun pipeline içindeki konumu,
- ARGB pikselinin BGR-CHW tensöre dönüştürülmesi,
- mean ve standard deviation değerlerinin uygulanması,
- 2x2 görüntü üzerinden uçtan uca örnek,
- ölçülen sürelerin aşamalara dağılımı,
- doğruluk kontrolleri ve darboğazın nedeni,
- uygulanan LUT + reusable FloatArray + bulk copy optimizasyonu,
- native NEON ve model grafiği gibi sonraki optimizasyon seçenekleri.

Bu sürümde LUT + reusable FloatArray + bulk copy Kotlin optimizasyonu uygulanmıştır. Native NEON, rotate/resize birleşimi ve model grafiğine normalizasyon taşıma uygulanmamıştır.

## 2. Kısa sonuç

Mevcut işlem 416x416 boyutundaki Bitmap piksellerini [1, 3, 416, 416] şekilli FLOAT girdiye çevirir. Her piksel için:

1. ARGB Int içinden R, G ve B kanalları çıkarılır.
2. Kanal sırası RGB'den BGR'ye çevrilir.
3. B, G ve R kanalları başlangıçta oluşturulan 256 elemanlı LUT'lar ile normalize edilir.
4. Sonuçlar yeniden kullanılan FloatArray'e B düzlemi, ardından G düzlemi, ardından R düzlemi olarak yazılır.
5. FloatArray, mevcut direct FloatBuffer'a tek bulk kopya ile aktarılır ve buffer konumu sıfırlanır.

Optimizasyon öncesi son gerçek cihaz ölçümünde ilk 100 benchmark örneği üzerinden normalizasyonun ortalaması 55,463 ms, p50 değeri 53,577 ms, p95 değeri 57,746 ms ve maksimumu 133,309 ms olarak hesaplanmıştır. Aynı örneklerde toplam pipeline ortalaması 135,077 ms olmuştur. Bu değerler yeni implementasyon için baseline'dır; optimizasyon kazanımı cihaz üzerinde aynı benchmark protokolüyle ayrıca ölçülmelidir.

## 3. Pipeline içindeki konumu

Mevcut akış aşağıdaki sırayı izler:

    CameraX RGBA 640x480
        |
        v
    Yeniden kullanılan kaynak Bitmap'e kopyalama
        |
        v
    Rotate + scale ile 416x416 detector Bitmap
        |
        v
    Bitmap.getPixels ile IntArray'e piksel okuma
        |
        v
    ARGB -> normalize edilmiş BGR-CHW
        |
        v
    [1, 3, 416, 416] FloatBuffer
        |
        v
    ONNX Runtime session.run
        |
        v
    Postprocess

**Şekil 1.** RGBA_NATIVE_BITMAP ön işleme ve inference akışı.

Normalizasyon, rotate/scale ve Bitmap.getPixels adımlarından sonra, ONNX Runtime çağrısından hemen önce yapılır. Bu ayrım önemlidir: benchmark'taki normalizationNs yalnızca ARGB piksel dönüştürme ve standardizasyon döngüsünü; onnxNs ise model çalıştırma süresini temsil eder.

İlgili kaynak kod: [NanoDetPreprocessor.kt](C:/Users/enesb/StudioProjects/deveng-core-kmp/deveng-core/src/androidMain/kotlin/core/domain/camera/analysis/nanodet/NanoDetPreprocessor.kt:188).

## 4. Girdi ve çıktı sözleşmesi

### 4.1 Girdi görüntü

- Kaynak kamera karesi: 640x480 RGBA.
- Rotate/scale sonrasında kullanılan detector Bitmap: 416x416.
- Piksel okuma: Bitmap.getPixels ile yeniden kullanılan IntArray.
- Piksel gösterimi: Android ARGB biçiminde 0xAARRGGBB.
- Alpha kanalı okunur gibi görünse de hesaplamada kullanılmaz; yalnızca R, G ve B kullanılır.

416x416 görüntüde toplam piksel sayısı:

    planeSize = 416 x 416 = 173.056 piksel

### 4.2 Model girdisi

NanoDet model sözleşmesi:

- şekil: [1, 3, 416, 416],
- veri tipi: FLOAT,
- düzen: CHW,
- kanal sırası: BGR,
- batch: 1.

Bu, modelin önce bütün B kanalını, sonra bütün G kanalını, sonra bütün R kanalını beklediği anlamına gelir. Her kanalın düzlem boyutu 173.056 elemandır.

Modelin kullandığı sabitler:

| Kanal | Mean | Standard deviation |
|---|---:|---:|
| B | 103,53 | 57,375 |
| G | 116,28 | 57,12 |
| R | 123,675 | 58,395 |

**Tablo 1.** NanoDet BGR normalizasyon sabitleri.

Sabitler [NanoDetConfig.kt](C:/Users/enesb/StudioProjects/deveng-core-kmp/deveng-core/src/androidMain/kotlin/core/domain/camera/analysis/nanodet/NanoDetConfig.kt:44) içinde tanımlıdır.

## 5. Bir piksel üzerinde yapılan işlem

Güncel sıcak döngünün özeti şöyledir. LUT'lar bu döngüden önce bir kez oluşturulur:

    val pixel = pixels[index]

    val pixel = pixels[index]

    output[index] = blueLut[pixel and 0xFF]
    output[gOffset + index] = greenLut[(pixel ushr 8) and 0xFF]
    output[rOffset + index] = redLut[(pixel ushr 16) and 0xFF]

    destination.position(0)
    destination.put(output, 0, planeSize * 3)
    destination.position(0)

### 5.1 ARGB içinden kanal çıkarma

Örneğin piksel 0xFFC86432 olsun:

- AA = FF, alpha = 255,
- RR = C8, red = 200,
- GG = 64, green = 100,
- BB = 32, blue = 50.

Alpha yok sayılır. R, G ve B değerleri 0 ile 255 arasındaki ham kanal yoğunluklarıdır.

### 5.2 Standardizasyon formülü

Model girdisindeki her kanal için formül:

    normalized = (rawChannel - mean) / standardDeviation

Örnek pikselin normalize edilmiş değerleri:

    B = (50  - 103,53)  / 57,375 = -0,933
    G = (100 - 116,28)  / 57,12  = -0,285
    R = (200 - 123,675) / 58,395 = 1,307

Bu işlem rawChannel / 255 değildir. Ayrıca mevcut kod değerleri 0 ile 1 arasına kırpmaz; sonuçlar negatif veya 1'den büyük olabilir.

### 5.3 CHW yerleşimi

planeSize = 173.056 olduğundan aynı pikselin üç kanal değeri önce reusable FloatArray'in üç farklı bölgesine, ardından aynı düzen korunarak direct FloatBuffer'a yazılır:

- B değeri: destination[index],
- G değeri: destination[173.056 + index],
- R değeri: destination[346.112 + index].

Bu nedenle bellek sırası tek piksel için B,G,R,B,G,R şeklinde değildir. Önce tüm B düzlemi, sonra tüm G düzlemi, sonra tüm R düzlemi gelir.

İlgili uygulama: [NanoDetPreprocessor.kt](C:/Users/enesb/StudioProjects/deveng-core-kmp/deveng-core/src/androidMain/kotlin/core/domain/camera/analysis/nanodet/NanoDetPreprocessor.kt:599).

## 6. 2x2 görüntü örneği

Aşağıdaki 2x2 görüntünün her hücresinde (R,G,B) değerleri olduğunu varsayalım:

    [ (200,100, 50), (  0,128,255) ]
    [ ( 64,192, 32), (255,255,255) ]

Her hücre aynı BGR formülü ile işlenir. Yaklaşık sonuçlar:

| Konum | B normalize | G normalize | R normalize |
|---|---:|---:|---:|
| Sol üst (200,100,50) | -0,933 | -0,285 | 1,307 |
| Sağ üst (0,128,255) | 2,640 | 0,205 | -2,118 |
| Sol alt (64,192,32) | -1,247 | 1,326 | -1,022 |
| Sağ alt (255,255,255) | 2,640 | 2,429 | 2,249 |

**Tablo 2.** 2x2 örnekte piksel başına normalize edilmiş BGR değerleri.

CHW düzlemleri ayrı ayrı yazılır:

    B düzlemi = [-0,933,  2,640, -1,247,  2,640]
    G düzlemi = [-0,285,  0,205,  1,326,  2,429]
    R düzlemi = [ 1,307, -2,118, -1,022,  2,249]

Dolayısıyla FloatBuffer içindeki örnek sıra şöyledir:

    [-0,933, 2,640, -1,247, 2,640,
     -0,285, 0,205, 1,326, 2,429,
      1,307, -2,118, -1,022, 2,249]

Model bu sıralama sayesinde ilk 4 elemanı B kanalı, sonraki 4 elemanı G kanalı, son 4 elemanı R kanalı olarak yorumlar.

## 7. İşlem maliyeti

### 7.1 İşlenen veri miktarı

416x416 giriş için:

- piksel sayısı: 173.056,
- kanal değeri sayısı: 3 x 173.056 = 519.168,
- yalnızca 32-bit float çıktı kapasitesi: yaklaşık 2.076.672 byte yani 1,98 MiB.

Mevcut sıcak döngüde her piksel için:

- ARGB Int içinden üç kanal maskelenir,
- üç LUT erişimi yapılır,
- üç FloatArray elemanı atanır.

Mean çıkarma, standard deviation bölmesi ve float dönüşümü frame döngüsünden çıkarılmıştır. Döngü tamamlandıktan sonra 519.168 elemanlık çıktı mevcut direct FloatBuffer'a tek bulk kopya ile aktarılır. Böylece önceki 519.168 indexed FloatBuffer.put çağrısı kaldırılmış, yerine ardışık FloatArray yazımı ve tek toplu buffer kopyası konmuştur.

LUT'lar yalnızca 0–255 aralığındaki kanal değerleri için bir kez oluşturulur. Reusable FloatArray de preprocessor yaşam döngüsü boyunca tutulur; normalizasyon sırasında frame başına 2 MiB civarında yeni FloatArray allocation'ı yapılmaz.

### 7.2 Gerçek cihaz benchmark dağılımı

Aşağıdaki değerler kullanıcının sağladığı logdaki ilk 100 ölçümden (analysis=6..105) yeniden hesaplanmıştır. Logda benchmark özeti sonrasında dört ek pipeline satırı daha bulunduğu için bu satırlar resmi 100 örneğe dahil edilmemiştir.

| Aşama | Ortalama (ms) | p50 (ms) | p95 (ms) | Maksimum (ms) |
|---|---:|---:|---:|---:|
| Frame transfer | 0,244 | 0,219 | 0,347 | 0,805 |
| Rotate + scale | 2,233 | 2,051 | 2,444 | 11,329 |
| Pixel read | 1,063 | 1,003 | 1,182 | 3,383 |
| Normalizasyon | 55,463 | 53,577 | 57,746 | 133,309 |
| Preprocess toplam | 59,068 | 57,019 | 61,184 | 136,866 |
| ONNX inference | 75,246 | 67,560 | 112,761 | 274,652 |
| Postprocess | 0,721 | 0,581 | 1,670 | 3,260 |
| Total | 135,077 | 125,113 | 171,561 | 412,079 |

**Tablo 3.** RGBA_NATIVE_BITMAP, BACKGROUND, XNNPACK benchmark dağılımı.

Kullanıcının log özetindeki p50/p95/maksimum değerleriyle uyumludur:

- preprocess: 57,02 / 61,18 / 136,87 ms,
- ONNX: 67,56 / 112,76 / 274,65 ms,
- total: 125,11 / 171,56 / 412,08 ms.

### 7.3 Normalizasyonun preprocess içindeki payı

Ortalama süre üzerinden:

    55,463 / 59,068 x 100 = %93,9

Yani preprocess ortalamasının yaklaşık %93,9'u normalizasyon döngüsünde harcanmaktadır. Diğer alt aşamaların yaklaşık payları:

- frame transfer: %0,4,
- rotate + scale: %3,8,
- pixel read: %1,8,
- normalizasyon: %93,9.

Bu ölçüm, “ön işleme pahalı” ifadesini daha net bir sonuca dönüştürür: bu RGBA yolunda asıl hedef normalizasyon olmalıdır; rotate/scale veya frame transfer ilk öncelik değildir.

### 7.4 Aykırı örnekler

Maksimum değerler tipik sıcak yol davranışını tek başına temsil etmez:

| Analysis | Normalizasyon | ONNX | Total |
|---:|---:|---:|---:|
| 16 | 133,309 ms | 274,652 ms | 412,079 ms |
| 17 | 61,759 ms | 191,889 ms | 259,365 ms |
| 71 | 109,932 ms | 202,097 ms | 328,134 ms |

Bu üç aykırı örnek çıkarıldığında kalan 97 örnekte normalizasyon ortalaması yaklaşık 54,034 ms olur. Bu değer, normalizasyonun sürekli maliyetinin yaklaşık 54–55 ms bandında olduğunu; 133 ms maksimumun ise tek başına genel davranış olmadığını gösterir. Aykırı örnekler yine de toplam tail latency ve kullanıcı deneyimi açısından önemlidir.

## 8. Normalizasyona dahil olmayan işlemler

Aşağıdaki aşamalar normalizasyonun parçası değildir:

- frame transfer: kamera belleğinden yeniden kullanılan kaynak Bitmap'e kopyalama,
- rotate + scale: görüntüyü yönlendirme ve 416x416'ya sığdırma,
- pixel read: detector Bitmap'ten IntArray oluşturma/okuma,
- ONNX inference: modelin session.run çağrısı,
- postprocess: model çıktısının decode, eşikleme ve NMS işlemleri.

Giriş doğrudan RGBA olduğundan YUV->RGB dönüşümü benchmark normalizasyon süresine dahil değildir. NanoDet ön işleme artık yalnızca CameraX `RGBA_8888` çıktısını kabul eder; önceki YUV referans yolu kaldırılmıştır.

## 9. Mevcut yaklaşımın güçlü ve zayıf yönleri

### Güçlü yönler

- Modelin beklediği BGR-CHW sözleşmesine doğrudan uyar.
- Mean ve standard deviation değerleri tek bir merkezi konfigürasyondan gelir.
- Kaynak Bitmap, detector Bitmap, IntArray, reusable normalize FloatArray ve FloatBuffer yeniden kullanılabildiği için sürekli allocation baskısı azaltılır.
- 256 elemanlı B/G/R LUT'lar sayesinde frame başına mean/std aritmetiği yapılmaz.
- Alpha kanalı doğru şekilde yok sayılır.
- Birim ve instrumented testlerle kanal sırası ve sayısal doğruluk kontrol edilir.

### Zayıf yönler

- 173.056 pikselin tamamı Kotlin/JVM döngüsünde işlenir.
- Her pikselde üç LUT erişimi ve üç FloatArray yazımı yapılmaya devam eder.
- FloatArray'den direct FloatBuffer'a her frame tek yaklaşık 2 MiB bulk kopya yapılır; bunun doğrudan buffer yazımından hızlı olup olmadığı cihaz benchmarkı ile doğrulanmalıdır.
- ARGB Int içinden kanal çıkarma ve üç düzleme yazma Kotlin/JVM sıcak döngüsünde kalır; native SIMD uygulanmış değildir.
- İşlem adı RGBA_NATIVE_BITMAP olsa da bu normalizasyon fonksiyonu kendi başına C++/NEON native kodu değildir; Android Bitmap kullanımı native olsa da sıcak normalizasyon döngüsü Kotlin'dedir.

## 10. Doğruluk ve testler

Normalizasyonun doğru kabul edilmesi için şu sözleşmeler korunmalıdır:

1. B kanalında mean[0] ve standardDeviation[0] kullanılmalıdır.
2. G kanalında mean[1] ve standardDeviation[1] kullanılmalıdır.
3. R kanalında mean[2] ve standardDeviation[2] kullanılmalıdır.
4. Çıktı sırası B düzlemi, G düzlemi, R düzlemi olmalıdır.
5. Alpha kanalının değişmesi BGR çıktısını değiştirmemelidir.
6. Çıktı kapasitesi en az 3 x width x height float olmalıdır.
7. Değerler otomatik olarak 0–1 aralığına kırpılmamalıdır.

2x2 piksel ve kanal değerleri ile bulk direct `FloatBuffer` yazımından sonra buffer konumunun sıfırlanması [NanoDetPreprocessorTest.kt](C:/Users/enesb/StudioProjects/deveng-core-kmp/deveng-core/src/androidUnitTest/kotlin/core/domain/camera/analysis/nanodet/NanoDetPreprocessorTest.kt) içinde kontrol edilir. RGBA byte sırasının BGR-CHW tensörüne doğru çevrildiği cihaz testiyle doğrulanır: [NanoDetRgbaNativePreprocessorInstrumentedTest.kt](C:/Users/enesb/StudioProjects/deveng-core-kmp/deveng-core/src/androidInstrumentedTest/kotlin/core/domain/camera/analysis/nanodet/NanoDetRgbaNativePreprocessorInstrumentedTest.kt).

Performans optimizasyonu yapılırken bu testler ve BGR-CHW sözleşmesi korunmalıdır. Daha hızlı olmak, kanal sırası veya mean/std eşlemesi yanlışsa kabul edilebilir bir sonuç değildir.

## 11. Uygulanan ve planlanan optimizasyonlar

### 11.1 Uygulanan Kotlin optimizasyonu: LUT + reusable FloatArray

Mevcut implementasyonda her kanal için aynı 256 ham değer tekrar tekrar normalize edilmek yerine üç LUT oluşturulur:

    bLut[value] = (value - meanB) / standardDeviationB
    gLut[value] = (value - meanG) / standardDeviationG
    rLut[value] = (value - meanR) / standardDeviationR

Bu tablolar 0–255 arasındaki her değer için bir kez hazırlanır. Frame başına çalışan döngü yalnızca ARGB bit maskeleri, LUT erişimleri ve FloatArray atamalarını yapar:

    output[index] = bLut[pixel and 0xFF]
    output[gOffset + index] = gLut[(pixel ushr 8) and 0xFF]
    output[rOffset + index] = rLut[(pixel ushr 16) and 0xFF]

output, NanoDetPreprocessor içinde yeniden kullanılır. Döngü bittiğinde bu dizi mevcut direct FloatBuffer'a bulk olarak kopyalanır. Kopya öncesi ve sonrası buffer konumu sıfırlanır; böylece OnnxTensor'ın her frame aynı başlangıç konumundan okuması sağlanır.

Bu yöntem aritmetik maliyetini ve indexed `FloatBuffer.put` sayısını düşürür; karşılığında her frame yaklaşık 2 MiB veri için bir bulk kopya ekler. Kullanıcının gerçek cihaz karşılaştırmasında alternatif doğrudan buffer yöntemlerinden daha iyi sonuç verdiği için kalıcı normalizasyon yolu olarak seçilmiştir.

Test ekranındaki normalizasyon seçimi kaldırılmıştır. Ekran sabit olarak RGBA Native + LUT/FloatArray yolunu kullanır; CPU/XNNPACK ve worker priority karşılaştırmaları devam eder.

### 11.2 Native C++/NEON yaklaşımı

5–10 ms hedefine en yakın aday, ARGB/RGBA okuma, kanal çıkarma, mean/std standardizasyonu ve BGR düzlemlerine yazmayı tek native döngüde birleştirmektir. NEON vektörleştirmesiyle aynı anda birden fazla piksel işlenebilir.

İdeal sıcak yol:

    Bitmap/RGBA bellek
        -> native ARGB/RGBA load
        -> kanal unpack
        -> mean çıkarma + reciprocal ile çarpma
        -> B, G, R düzlemlerine ardışık yazma
        -> doğrudan model input buffer

Bu tasarım mevcut IntArray, reusable FloatArray ve Kotlin sıcak döngüsünü tek native akışla değiştirebilir. Yine de gerçek kazanım cihazın CPU çekirdeği, bellek bant genişliği, JNI sınırı ve ORT input buffer entegrasyonuna bağlıdır; 5–10 ms hedefi ölçüm yapılmadan garanti edilemez.

### 11.3 Rotate/scale ile birleştirme

Rotate/scale ortalaması yaklaşık 2,233 ms olduğu için ilk optimizasyon hedefi değildir. Native dönüşüm sırasında yeniden boyutlandırma ve yönlendirmeyi de birleştirmek ek kazanç sağlayabilir; ancak tasarım karmaşıklığı ve doğrulama yükü daha yüksek olduğundan normalizasyon döngüsü hızlandırıldıktan sonra değerlendirilmelidir.

## 12. Sonuç

Mevcut normalizasyon işlemi sayısal olarak doğru bir BGR-CHW standardizasyonudur. LUT + reusable FloatArray + bulk copy optimizasyonu uygulanmış ve cihaz karşılaştırmasının ardından tek normalizasyon yolu olarak bırakılmıştır. Karşılaştırma baseline'ı şu tabloyu ortaya koyar:

- normalizasyon p50: 53,577 ms,
- preprocess p50: 57,019 ms,
- ONNX p50: 67,560 ms,
- total p50: 125,113 ms.

Dolayısıyla optimizasyon öncesinde normalizasyon, preprocess süresinin neredeyse tamamını oluşturan açık darboğazdı. Bundan sonraki ilerleme sırası:

1. sabit RGBA Native + LUT/FloatArray yolunu aynı 5 warmup + 100 örnek protokolüyle izlemek,
2. kazanım yetersizse ARGB/RGBA -> normalize BGR-CHW işlemini C++/NEON native katmanına taşımayı değerlendirmek,
3. her aşamada aynı model sözleşmesi ve doğruluk testlerini korumak.

## Referanslar

1. [NanoDetPreprocessor.kt — RGBA Bitmap ön işleme ve normalizasyon](C:/Users/enesb/StudioProjects/deveng-core-kmp/deveng-core/src/androidMain/kotlin/core/domain/camera/analysis/nanodet/NanoDetPreprocessor.kt:188)
2. [NanoDetPreprocessor.kt — LUT ve bulk buffer normalizasyonu](C:/Users/enesb/StudioProjects/deveng-core-kmp/deveng-core/src/androidMain/kotlin/core/domain/camera/analysis/nanodet/NanoDetPreprocessor.kt)
3. [NanoDetValidationAnalyzer.kt — sabit pipeline benchmark logu](C:/Users/enesb/StudioProjects/deveng-core-kmp/deveng-core/src/androidMain/kotlin/core/domain/camera/analysis/nanodet/NanoDetValidationAnalyzer.kt)
4. [NanoDetValidationScreen.android.kt — CPU/XNNPACK ve priority benchmark ekranı](C:/Users/enesb/StudioProjects/deveng-core-kmp/sample/composeApp/src/androidMain/kotlin/global/deveng/core/NanoDetValidationScreen.android.kt)
5. [NanoDetConfig.kt — model input shape, mean ve standard deviation](C:/Users/enesb/StudioProjects/deveng-core-kmp/deveng-core/src/androidMain/kotlin/core/domain/camera/analysis/nanodet/NanoDetConfig.kt:44)
6. [NanoDetPreprocessorTest.kt — 2x2 BGR-CHW ve direct buffer doğrulama testleri](C:/Users/enesb/StudioProjects/deveng-core-kmp/deveng-core/src/androidUnitTest/kotlin/core/domain/camera/analysis/nanodet/NanoDetPreprocessorTest.kt:47)
7. [NanoDetRgbaNativePreprocessorInstrumentedTest.kt — referansla sayısal karşılaştırma](C:/Users/enesb/StudioProjects/deveng-core-kmp/deveng-core/src/androidInstrumentedTest/kotlin/core/domain/camera/analysis/nanodet/NanoDetRgbaNativePreprocessorInstrumentedTest.kt:108)
8. [Gerçek cihaz benchmark logu](C:/Users/enesb/.codex/attachments/cbe0fc5e-119a-44ee-b7c4-ed594aabdb61/pasted-text.txt)

**OpenAI Codex**
