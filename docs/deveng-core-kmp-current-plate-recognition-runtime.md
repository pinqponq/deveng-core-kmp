# Deveng Core KMP Mevcut Plaka Okuma Çalışma Zamanı

> Durum: 25 Ağustos 2026 itibarıyla Android kaynak kodundaki mevcut akış
>
> Kapsam: Kamera ekranı, Compose bileşenleri ve sample UI hariçtir. Bu belge yalnızca bir
> uygulamanın verdiği ONNX model dosyalarıyla çalışan arka plan plaka tanıma yolunu açıklar.

## 1. Amaç ve kısa cevap

Bu belge, `deveng-core-kmp` içindeki mevcut plaka okuma kodunun hangi parçadan oluştuğunu,
her parçanın neden var olduğunu ve veri akışının nerede başladığını açıklar. Hedef; sonraki
sadeleştirme kararında "bu sınıf model çalıştırmak için gerekli mi, yoksa yalnızca kamera,
doğrulama veya gözlemlenebilirlik için mi var?" sorusunu kaynak koduna dayanarak yanıtlamaktır.

Kullanıcı tarafındaki ana kullanım, iki model dosyasını verip doğrulanmış sonucu almaktır:

```kotlin
val handle = controller.registerPlateRecognition(
    detectorModelFile = detectorModelFile,
    ocrModelFile = ocrModelFile,
    onPlateRecognized = { result ->
        // result.plate ve result.confidence
    },
    onError = { error -> /* uygulama kararı */ },
)

// İş bittiğinde yalnızca bu kaydı kapatır.
handle.close()
```

`registerPlateRecognition` kamera preview'ını kapatmaz, ekranda gezinmez, API çağrısı veya
kayıt yapmaz. Bunlar çağıran uygulamanın sorumluluğudur. Callback, camera controller'ın analiz
executor'ında gelir; UI durumunu güncelleyecek uygulama bunu kendi UI bağlamına taşır.

## 2. Sistem sınırı

```text
Tüketici uygulama
  iki ONNX File + çalışma ayarı + sonuç/hata callback'i
                         |
                         v
CameraController.registerPlateRecognition(...)
                         |
                         v
CameraX ImageAnalysis / frame yaşam döngüsü
                         |
                         v
PlateRecognitionAnalyzer
  NanoDet detector -> source-frame plaka kutusu -> kalite kapısı
  -> OCR ROI -> FastPlateOCR -> rolling consensus
                         |
                         v
PlateRecognitionResult(plate, confidence)
```

Bir kamera frame'i modele girmeden önce iki ek gerçek vardır:

1. `ImageProxy` kısa ömürlü bir kamera kaynağıdır. Her kabul edilen frame'in tam olarak bir kez
   serbest bırakılması gerekir; aksi halde kamera akışı kilitlenebilir.
2. OCR, tam görüntü üzerinde değil detector'ın bulduğu plaka ROI'sinde çalışır. Bu nedenle iki
   farklı model ve aralarında koordinat/rotasyon işlemi vardır.

Bu iki konu, klasörden tek tek görsel okuyan Python betiğinde bulunmaz; kod yapısının önemli bir
kısmı model matematiğinden değil, canlı kamera kaynağı ve iki aşamalı plaka tanımadan doğar.

## 3. Kullanıcıya açık girişler ve yaşam döngüsü

### 3.1 Dosya tabanlı giriş

`CameraController.registerPlateRecognition(detectorModelFile, ocrModelFile, ...)` doğrudan iki
`File` kabul eder. Dosyalar uygulamanın sahipliğindedir; core bunları kopyalamaz veya repository'ye
katmaz. Bu overload kendi detector/OCR oturumlarını açar.

`PlateRecognitionHandle.close()` kaydı kaldırır. Varsayılan `MANUAL` politikada native model
oturumları da bu sırada kapanır. İsteğe bağlı `AFTER_SUCCESS` ise yalnızca başarılı sonuç teslim
edildikten sonra kaydı kapatır; bunun için `lockAfterSuccessfulRead = true` gerekir.

### 3.2 Önceden hazırlanan giriş

`PlateRecognitionModelManager` ikinci, performans odaklı giriş yoludur. `prepareModels(spec)`
detector ve OCR session'larını arka planda açar, birer boş warm-up inference çalıştırır ve
`Idle`, `Loading`, `Warming`, `Ready`, `Failed`, `Closed` durumlarını yayınlar.

Manager ile kayıt yapılınca analyzer native session'ların sahibi olmaz; geçici bir lease alır.
Handle kapatılınca lease biter, ancak manager kapanana kadar aynı hazırlanmış session'lar kalır.
Bu yol ilk kamera analizinde model açma maliyetini taşımamak içindir; model sonucunun anlamını
değiştirmez. Manager destekli kullanımda release politikası `MANUAL` olmalıdır.

## 4. Frame kabulü: modelden önce çalışan zorunlu kamera katmanı

`CameraFrameAnalyzerAdapter`, suspend tabanlı analyzer'ı CameraX callback'ine bağlar.

| Davranış | Neden var? |
| --- | --- |
| `analyzeEveryNthFrame` | Her frame'i modele vermek zorunda olmayan uygulamanın analiz sıklığını belirler. |
| `minAnalysisIntervalMs` | İki model çalıştırması arasında en az süre bırakır. |
| Tek frame okuma kilidi | Aynı `ImageProxy` kaynaklarının çakışmamasını ve kuyruk büyümemesini sağlar. |
| `ImageProxy.close()` / `releaseAfterRead()` | Kabul edilmeyen, hata alan veya tamamlanan frame'in kamera kaynağını mutlaka serbest bırakır. |
| Tanılama sayaçları | Kuyruk bekleme, atlanan frame ve işlem süresini gözlemlemek içindir; sonuç üretme kuralını değiştirmez. |

CameraX tarafında `KEEP_ONLY_LATEST` kullanılır: analyzer meşgulse eski frame'ler birikmek yerine
atılır. Bir analiz tamamlanana kadar yeni analiz başlamaz. OCR için gereken ROI pikselleri
frame'den kopyalandıktan hemen sonra kamera buffer'ı serbest bırakılır.

Bu katman, Python'daki klasör döngüsünün kamera karşılığıdır; ancak Python'da dosyanın ömrünü
`cv2.imread` yönetirken Android'de `ImageProxy` ömrünü kodun yönetmesi gerekir.

## 5. Uçtan uca tanıma akışı

`PlateRecognitionAnalyzer` akışın orkestratörüdür. Kendisi yeni bir model türü değildir; detector,
OCR, kalite kontrolü, doğrulama ve isteğe bağlı takip bileşenlerini sıraya koyar.

```text
RGBA_8888 AndroidCameraFrame
  -> NanoDetDetector
  -> detector koordinatındaki adayları source-frame koordinatına eşleme
  -> PlateQualityGate
  -> SourceRoiReader + OcrPreprocessor
  -> FastPlateOcr
  -> PlateResultValidator
  -> onPlateRecognized(PlateRecognitionResult)
```

1. **NanoDet** frame'i detector girişine dönüştürür ve plaka adaylarını çıkarır.
2. **Koordinat eşleme** detector'a yeniden boyutlandırılmış görüntüdeki kutuyu özgün kamera
   frame'ine taşır.
3. **Kalite kapısı**, düşük detector confidence'lı veya çok küçük kutuları OCR'a göndermeden eler.
4. **ROI okuyucu**, kaynak frame'den seçilmiş kutunun piksellerini alır ve kamera rotasyonunu
   düzeltir.
5. **OCR ön işleme**, ROI'yi OCR modelinin beklediği tensöre çevirir.
6. **OCR decoder**, her karakter slotu için en yüksek olasılığı seçer.
7. **Validator**, yeterli confidence'taki tekrarları rolling pencerede doğrular; yalnızca consensus
   oluşursa sonuç üretir.

## 6. Model çalışma zamanı ve model sözleşmeleri

### 6.1 Ortak ONNX katmanı

`OnnxModelSession`, Android'deki file-backed ONNX Runtime session'ının küçük sahiplik sarmalayıcısıdır.
Session açarken CPU veya XNNPACK execution provider'ını ayarlar; kapanmayı tek seferli yapar.
Modelin giriş/çıkış adı, tipi ve şekli burada değil, modeli bilen detector/OCR sarmalayıcısında
doğrulanır. Böylece ortak katmanın NanoDet veya OCR sözleşmesini bilmesi gerekmez.

### 6.2 Detector: NanoDet

| Konu | Mevcut sözleşme |
| --- | --- |
| Girdi | `input`, `FLOAT[1, 3, 416, 416]`, RGB, NCHW, 0–1 normalize değerler |
| Çıktı | `output`, `FLOAT[1, 3598, 33]` |
| Çıktı çözümü | score + dört adet 8-bin dağılım için DFL decode |
| Priorlar | stride `8, 16, 32, 64` ile üretilen 3598 merkez |
| Son eleme | confidence eşiği ve IoU NMS |

`NanoDetDetector` şunları bir arada tutar: model dosyası doğrulama, session açma/fallback,
doğrudan `FloatBuffer` tensörleri, RGBA ön işleme, ONNX çağrısı, DFL postprocess ve detector
koordinatını kaynak frame'e taşıma. `NanoDetPreprocessor` ve `NanoDetPostprocessor` bu iki
algoritmik kısmı ayırır.

Bu sözleşme modelden yalnızca input boyutunu okumaktan daha fazlasını gerektirir. Paylaşılan
Python örneği de input yüksekliği/genişliğini ONNX metadatasından okur; fakat `STRIDES`,
`REG_MAX`, beklenen 33 kanal, DFL decode ve NMS eşiklerini kodda sabit taşır. Android kodundaki
`NanoDetConfig` aynı model-aile bilgilerini tip güvenli yapılandırma olarak taşır.

### 6.3 OCR: FastPlateOCR v5

| Konu | Mevcut sözleşme |
| --- | --- |
| Girdi | `input`, `FLOAT[-1, 64, 128, 3]`, RGB, NHWC, ham 0–255 float değerler |
| Çıktı | `plate`, `FLOAT[-1, 10, 37]` |
| Çözüm | CTC yok; 10 slotun her birinde argmax |
| Metin | Alfabe eşleme, sondaki `_` padding karakterlerini kırpma |
| Confidence | 10 slot confidence değerinin ortalaması |

`FastPlateOcr` session, yeniden kullanılabilir tensör buffer'ları ve inference'ı yönetir.
`SourceRoiReader` piksel okuma/rotasyon sorumluluğunu; `OcrPreprocessor` 64×128 lineer resize
ve RGB NHWC tensor hazırlamayı; `OcrDecoder` slot-major çıktının metne çevrilmesini yapar.
Frame, OCR için gereken pikseller kopyalandıktan hemen sonra serbest bırakılır; ONNX inference
kamera buffer'ını elde tutmaz.

## 7. Sonuç kalitesi ve durum yönetimi

`PlateRecognitionConfig` varsayılanları aşağıdadır:

| Ayar | Varsayılan | Etkisi |
| --- | ---: | --- |
| `detectionConfidenceThreshold` | `0.50` | NanoDet aday eşiği |
| `nmsIouThreshold` | `0.60` | Birbiriyle çakışan kutuları eleme |
| minimum kutu | `32×8 px` | OCR için çok küçük ROI'leri eleme |
| `ocrConfidenceThreshold` | `0.90` | Consensus'a girecek OCR okuması eşiği |
| `requiredMatchingReads` | `2` | Aynı plakanın gerekli tekrar sayısı |
| `validationWindowSize` | `3` | Rolling pencerenin geçerli okuma kapasitesi |
| `lockAfterSuccessfulRead` | `true` | Başarılı sonuçtan sonra yeni inference kabulünü durdurur |

`PlateResultValidator`, boş, sonlu olmayan veya eşik altı OCR sonuçlarını pencereye eklemez.
Pencere dolduğunda aynı metin en az iki kez görünüyorsa `PlateRecognitionResult` döndürür;
confidence bu metni destekleyen okumaların ortalamasıdır. Varsayılan kilit, kamera preview'ını
kapatmaz ve model session'ını otomatik kapatmak zorunda değildir; bunlar ayrı politikalardır.

`PlateRecognitionDebugSnapshot`, detector/OCR süreleri, aday kutular ve OCR'ın `COMPLETED`,
`FAILED` veya `SKIPPED_IN_FLIGHT` durumunu taşır. Bu yalnızca tanılama içindir; production sonucu
kurallarını değiştirmez.

## 8. Dosya haritası

| Dosya veya grup | Sorumluluk | Sadeleştirme değerlendirmesindeki yeri |
| --- | --- | --- |
| `analysis/CameraFrameAnalyzer.kt`, `AndroidCameraFrame.kt`, `CameraFrameAnalyzerAdapter.kt` | CameraX frame sözleşmesi, sahiplik, hız sınırlama | Canlı kamera için zorunlu altyapı; plaka modeline özgü değildir. |
| `controller/PlateRecognitionRegistration.android.kt` | İki model dosyasını/manager'ı tek public API'de birleştirir | "Modeli ver, çalışsın" deneyiminin mevcut façade'ı. |
| `analysis/plate/PlateRecognitionAnalyzer.kt` | Detector→OCR→validator orkestrasyonu | İki aşamalı tanıma için gerekli, fakat bazı debug/tracking ayrıntıları ayrıştırılabilir. |
| `analysis/plate/PlateRecognitionModelManager.kt`, `...Resources.kt`, `...PreparationState.kt` | Ön yükleme, warm-up, session paylaşımı | İlk kullanım gecikmesini azaltan isteğe bağlı performans katmanı. |
| `analysis/onnx/OnnxModelSession.kt`, `OnnxModelException.kt` | Ortak ONNX session açma/kapatma ve hata türleri | İki modelin native kaynak sahipliği için küçük ortak katman. |
| `analysis/nanodet/NanoDetDetector.kt` | NanoDet ONNX çağrısı ve uçtan uca detector işlemi | Model-aileye özgü zorunlu sarmalayıcı. |
| `analysis/nanodet/NanoDetConfig.kt`, `NanoDetPreprocessor.kt`, `NanoDetPostprocessor.kt`, `PlateDetection.kt` | Tensor biçimi, resize/normalize, DFL/NMS, kutu tipleri | Model sözleşmesinin algoritmik ayrıntıları; model tamamen değişirse değişir. |
| `analysis/ocr/FastPlateOcr.kt`, `FastPlateOcrModelContract.kt` | OCR ONNX session ve sözleşmesi | OCR modeline özgü zorunlu sarmalayıcı. |
| `analysis/ocr/SourceRoiReader.kt`, `OcrPreprocessor.kt`, `OcrDecoder.kt` | ROI alma/rotasyon, tensor hazırlama, slot decode | Detector + OCR arasındaki veri doğruluğu için zorunlu algoritmik katman. |
| `analysis/plate/PlateQualityGate.kt`, `PlateResultValidator.kt` | OCR çağrı maliyetini azaltma, tekrar doğrulaması | Ürün güvenilirliği politikası; model çalışması için teknik olarak zorunlu değildir. |
| `analysis/plate/PlateRecognitionPerformanceProfile.kt` | XNNPACK/CPU ve thread profil seçimi | Cihaz hedeflerine göre ayarlanabilir performans katmanı. |

## 9. Python örneğiyle doğru karşılaştırma

`D:\AI Projects\nanodet-plus\test.py` dosyası tek bir detector modelini bir klasördeki bağımsız
görüntülere uygular. Sade ve kullanışlıdır; `session.get_inputs()[0]` ile input adını ve 416 gibi
boyutları modelden alır. Ancak betik yalnızca input boyutunu otomatik keşfeder, modelin tüm
anlamını otomatik keşfetmez: NanoDet postprocess bilgileri açıkça kodun içindedir.

| Python betiği | Android canlı pipeline |
| --- | --- |
| Bir ONNX detector | Detector + ayrı OCR ONNX modeli |
| `cv2.imread` ile kalıcı görüntü | Kapatılması gereken geçici `ImageProxy` |
| Tam görüntü resize → detector | Frame → detector → kaynak ROI → OCR |
| Klasör sıralı, doğal backpressure | 30 FPS civarı producer; frame atlama/tek analiz denetimi gerekir |
| Düşük confidence görüntüyü txt'ye yazma | Kalite kapısı + OCR confidence + rolling consensus + callback |
| Bir dosyada uygulanabilir | Kütüphane sorumluluklarını test edilebilir ve yeniden kullanılabilir sınıflara ayırır |

Dolayısıyla hedef kullanıcı deneyimi açısından "iki modeli ver, sonuç al" zaten public façade ile
sağlanmıştır. İç kodun tek dosyaya indirilmesi ise otomatik sadeleşme değildir: canlı frame
sahipliği, NanoDet DFL algoritması ve OCR ROI/rotasyon sözleşmesi tek dosyada kalırsa daha az
dosya olur, fakat hata sınırları ve test edilebilirlik zayıflar.

## 10. Sonraki sadeleştirme çalışması için karar çerçevesi

Bu belge bir sadeleştirme önerisi veya kod değişikliği değildir. Mevcut kaynak yapısından çıkan
karar noktaları şunlardır:

1. **Public API zaten daraltılmıştır.** Uygulama `NanoDetDetector`, `FastPlateOcr` veya
   `CameraFrameAnalyzer` sınıflarını kullanmak zorunda değildir; public girşte iki `File` ve
   callback yeterlidir.
2. **Modelden otomatik okunabilecek bilgiler sınırlıdır.** Input/output isimleri ve şekilleri
   okunabilir; fakat NanoDet DFL/stride, OCR alphabet/slot anlamı, normalize biçimi ve ROI
   politikası ONNX genel metadatasından güvenilir biçimde çıkarılamaz.
3. **Zorunlu karmaşıklık üç köktedir.** CameraX kaynak ömrü, NanoDet'e özel decode ve OCR'a
   doğru ROI/rotasyon taşınması.
4. **İsteğe bağlı katmanlar ayrıştırılabilir.** Model manager warm-up, tracking, performans
   profilleri ve debug snapshot'lar temel dosya-tabanlı akıştan ayrı bir mod veya opsiyon olarak
   yeniden değerlendirilebilir.
5. **Doğrulama ürün tercihi olarak ele alınmalıdır.** 2/3 consensus ve yüzde 90 eşiği modelin
   zorunluluğu değil, yanlış okumaya karşı koruma politikasıdır. Tek sonuç istenen senaryoda bu
   açıkça yapılandırılabilir; varsayılanı değiştirmek cihaz verisiyle değerlendirilmelidir.

Sadeleştirme yapılacaksa güvenli başlangıç noktası, önce public girişin tek bir `PlateRecognition`
servis sözleşmesinde kalıp kalmayacağını; sonra manager/tracking/debug katmanlarının temel
çalışma zamanından fiziksel olarak ayrılmasının tüketici API'sini ve gerçek cihaz performansını
nasıl etkileyeceğini ölçmektir. NanoDet ve OCR model sözleşmelerini doğrulamadan generic
"her modeli otomatik çalıştır" davranışı eklenmemelidir.

## Kaynaklar

- [PlateRecognitionRegistration.android.kt](../deveng-core/src/androidMain/kotlin/core/domain/camera/controller/PlateRecognitionRegistration.android.kt)
- [PlateRecognitionAnalyzer.kt](../deveng-core/src/androidMain/kotlin/core/domain/camera/analysis/plate/PlateRecognitionAnalyzer.kt)
- [CameraFrameAnalyzerAdapter.kt](../deveng-core/src/androidMain/kotlin/core/domain/camera/analysis/CameraFrameAnalyzerAdapter.kt)
- [PlateRecognitionModelManager.kt](../deveng-core/src/androidMain/kotlin/core/domain/camera/analysis/plate/PlateRecognitionModelManager.kt)
- [OnnxModelSession.kt](../deveng-core/src/androidMain/kotlin/core/domain/camera/analysis/onnx/OnnxModelSession.kt)
- [NanoDetDetector.kt](../deveng-core/src/androidMain/kotlin/core/domain/camera/analysis/nanodet/NanoDetDetector.kt)
- [FastPlateOcr.kt](../deveng-core/src/androidMain/kotlin/core/domain/camera/analysis/ocr/FastPlateOcr.kt)
- [Python NanoDet klasör testi](D:/AI%20Projects/nanodet-plus/test.py)

**E. B.**
