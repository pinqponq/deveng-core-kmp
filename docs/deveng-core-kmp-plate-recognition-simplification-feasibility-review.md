# Plaka Tanıma Pipeline Sadeleştirme Planı: Uygulanabilirlik Değerlendirmesi

> İncelenen öneri: `C:\Users\enesb\Downloads\Android Plaka Tanıma Pipeline Sadeleştirme ve Dinamik ONNX Model Sözleşmesi Planı.md`
>
> Kaynak durumu: Bu değerlendirme, çalışma ağacındaki Android kaynaklarını esas alır.
>
> Kapsam: Değerlendirme ve güvenli uygulama koşulları. Bu belge bir refactor uygulaması değildir.

## 1. Sonuç özeti

Önerilen yön **uygulanabilir ve değerli**: tüketici uygulama iki model dosyasını verip sonuç almalı; camera, detector, OCR ve sonuç doğrulama ise kendi doğru yaşam döngüleriyle korunmalıdır. ONNX metadata'sını merkezileştirmek, aynı input/output bilgisinin farklı config ve sınıflarda tekrarlanma riskini azaltır.

Planın bu haliyle uygulanmaması gereken dört noktası vardır:

1. ONNX metadata, modelin şekil ve tür bilgisini verir; NanoDet veya OCR semantiğini tek başına güvenilir biçimde vermez.
2. NanoDet için "dynamic shape" yalnızca input buffer boyutunu değiştirmek değildir. Prior sayısı, output boyutu ve DFL decode buna bağlıdır.
3. Paylaşılan runtime'ın aynı anda kaç analyzer/camera tarafından kullanılacağı ve hangi koşulda kapanacağı açık bir sözleşme olmadan manager katmanını kaldırmak kaynak yarışına yol açabilir.
4. Dosya sayısını azaltmak için test edilebilir algoritmik sınırlar kaybedilmemelidir.

Bu nedenle doğru hedef, "tek dosyalı generic ONNX okuyucu" değil; **tek public façade + tek resolved contract + model-family profile'ları + açık kaynak sahipliği** olmalıdır.

## 2. İnceleme tablosu

| Plan maddesi | Karar | Neden | Güvenli koşul |
| --- | --- | --- | --- |
| Camera'nın modelden bağımsız kalması | Uygulanabilir | Kamera frame/backpressure yaşam döngüsü model semantiğinden ayrıdır. | `CameraFrameAnalyzer` sözleşmesi korunur. |
| `KEEP_ONLY_LATEST` ve frame sahipliği | Uygulanabilir | Canlı kamera, klasör tabanlı batch akışından farklı olarak kısa ömürlü `ImageProxy` kullanır. | Her yol `ImageProxy`yi tam bir kez serbest bırakır. |
| Detector → source ROI → OCR | Uygulanabilir | OCR'ın küçültülmüş detector girdisi yerine kaynak ROI kullanması görüntü bilgisini korur. | Mapping, rotasyon ve ROI testleri korunur. |
| Session'ları frame'ler arasında tekrar kullanma | Uygulanabilir | Session açma maliyeti frame bazlı kabul edilemez. | Session sahipliği ve kapatma tek yerde tanımlanır. |
| Input/output metadata'yı ONNX'ten okuma | Uygulanabilir | Node adı, tür, rank ve shape için tekrar eden sabitleri azaltır. | Profile ile uyumluluk fail-fast doğrulanır. |
| Sabit input boyutunu config'ten kaldırma | Uygulanabilir | Sabit shape ONNX'te güvenilir biçimde bulunuyorsa ikinci kaynak gereksizdir. | Warm-up, buffer ve preprocess aynı resolved contract'ı kullanır. |
| Dynamic input shape desteği | Koşullu uygulanabilir | NanoDet output topology'si input boyutundan bağımsız değildir. | Profile somut H/W seçer; prior/output ilişkisi doğrulanır. |
| Pre/postprocess sınıflarını model sınıfına alma | Koşullu uygulanabilir | Fiziksel dosya sayısı azalır; fakat saf algoritmaların test sınırı kaybolabilir. | Private/internal test girişleri ve davranış testleri korunur. |
| Quality gate'i detector'a alma | Koşullu uygulanabilir | Minimum kutu boyutu source-frame koordinatında anlamlıdır. | Önce map, sonra source-space quality gate uygulanır. |
| `PlateRecognitionRuntime` ile manager/resources sadeleştirmesi | Koşullu uygulanabilir | Sahiplik zinciri sadeleşebilir. | Tek aktif kullanım, close, model değişimi ve düşük bellek kuralları yazılı olur. |
| Adapter mantığını CameraController'a taşıma | Önce ölçülmeli | Adapter generic analyzer izolasyonu ve frame kapanışını tutar. | Davranış eşdeğerliği ve diğer analyzer regresyonu doğrulanır. |
| Tracking'i temel pipeline'ın parçası sayma | Bu haliyle uygun değil | Tracking varsayılan olarak kapalı, deneysel bir optimizasyondur. | Ayrı opt-in özellik olarak kalır. |

## 3. Önce düzeltilmesi gereken gerçek sözleşme ayrımı

İncelenen plan 416×416 ve 3598 tahmin noktası örneklerini kullanır. Bu değerler `D:\AI Projects\nanodet-plus` altındaki ayrı NanoDet export'una uygundur; aktif Android çalışma ağacının mevcut sözleşmesi değildir.

| Kaynak | Detector girdi/çıktısı |
| --- | --- |
| Aktif Android `NanoDetModelContract` | `data: FLOAT[1,3,256,256]`, `output: FLOAT[1,1360,33]` |
| Dış Python NanoDet klasör testi | `data: FLOAT[1,3,416,416]`, `output: FLOAT[1,3598,33]` |

Her iki export da aynı model ailesinden gelse bile input boyutu değişince prior sayısı da değişir. Bu nedenle 416 bilgisi Android runtime'a "mevcut gerçek" gibi taşınmamalıdır. Bu fark, metadata merkezileştirmenin güçlü gerekçesidir; ama model semantiğini otomatik keşfetmenin kanıtı değildir.

## 4. Dinamik ONNX sözleşmesinin doğru sınırı

### 4.1 ONNX'ten güvenle alınabilecek bilgiler

Session metadata'sından aşağıdakiler alınmalı ve resolved contract'ta tek kez tutulmalıdır:

- input/output node adları;
- input/output sayısı;
- element türleri;
- rank ve shape;
- batch veya uzamsal boyutun dinamik olup olmadığı.

Bu bilgi tensor oluşturma, input feed adı, pinned output adı, buffer tahsisi ve warm-up için ortak kaynak olabilir. Uyumlu olmayan dosya, ilk kamera frame'ini beklemeden session açılışında reddedilmelidir.

### 4.2 ONNX'ten çıkarılamayacak bilgiler

Aşağıdakiler sadece shape'e bakılarak güvenilir biçimde anlaşılamaz:

- BGR/RGB kanal sırası ve normalize formülü;
- NanoDet stride listesi, `regMax`, score kanalının anlamı ve DFL decoder;
- output sınıf sayısı ile sınıf etiketlerinin anlamı;
- OCR karakter alfabesi, padding karakteri ve slot-major decoder;
- bbox'ın letterbox, rotate veya mirror altında nasıl taşınacağı;
- ürün eşiği: detector confidence, OCR confidence ve consensus politikası.

Bunlar `ModelProfile` içinde tek kaynak olmalıdır. Profile, modelin **ne anlama geldiğini**; ONNX metadata ise modelin **hangi tensörleri sunduğunu** söylemelidir.

### 4.3 Profile seçimi açık olmalıdır

Sadece `[1,3,H,W]` input ve `[1,N,33]` output gördüğü için runtime'ın modeli NanoDet sayması doğru değildir. Benzer shape'li fakat farklı normalize veya decoder kullanan bir model sessizce yanlış sonuç üretebilir.

```text
Tüketici uygulama: NanoDetProfile / FastPlateOcrProfile seçer
ONNX Runtime: metadata'yı okur
Runtime: profile beklentisi ile metadata'yı karşılaştırır
Uyumsuzluk: açık, fail-fast hata
Uyum: ResolvedModelContract
```

Model dosyasının hash'i veya güvenilir bir sürüm kimliği ayrıca saklanabiliyorsa profile–artifact eşleşmesi daha da güvenli hale gelir; ancak hash tek başına model semantiği yerine geçmez.

## 5. Dynamic shape için kabul edilebilir ilk kapsam

NanoDet postprocess bugün output aday sayısını, input boyutunu, stride'ları ve DFL dağılımlarını birlikte kullanır. Örneğin 256×256 için stride `8,16,32,64` toplamı 1360 prior üretir; 416×416 için karşılık gelen sayı 3598'dir. Bu ilişki doğrulanmadan yalnızca input buffer'ını büyütmek yanlış decode veya buffer taşmasına yol açar.

İlk refactor için güvenli kapsam:

1. Sabit H/W varsa ONNX'ten okuyup tüm buffer ve warm-up işlemlerinde onu kullanmak.
2. Dynamic H/W varsa profile'ın verdiği bir somut `preferredInputSize` seçmek.
3. Seçilen boyut için prior sayısını üretmek ve metadata/runtime output ile karşılaştırmak.
4. Tek input ve tek output dışındaki modeli desteklenmiyor diye fail-fast reddetmek.
5. Output'un gerçek anlamı veya boyutu çalışma anında değişebiliyorsa bunu ilk sürümün dışında bırakmak.

Bu sınırlama gereklidir çünkü mevcut çalışma yolu input ve output için önceden tahsis edilmiş doğrudan `FloatBuffer` ve pinned output tensor kullanır. Gerçekten dinamik output, farklı tensor tahsis ve sonuç okuma stratejisi gerektirir; yalnızca config temizliği değildir.

## 6. Sadeleştirilmiş dosya yapısının uygulanabilir şekli

```text
camera/
  CameraController.android.kt
  CameraFrameAnalyzer.kt
  CameraFrameAnalyzerAdapter.kt

plate/
  PlateRecognitionAnalyzer.kt
  PlateRecognitionRuntime.kt
  NanoDetDetector.kt
  FrameCoordinateMapper.kt
  FastPlateOcr.kt
  PlateResultValidator.kt
  PlateRecognitionTypes.kt
```

`CameraFrameAnalyzerAdapter`ın listede kalması önerilir. Camera controller'ın yalnızca CameraX binding bilgisi taşıması; adapter'ın generic analyzer, throttle, busy kontrolü ve frame-release eşlemesini taşıması daha net sınırdır. Adapter'ı controller'a taşımak dosya sayısını azaltır fakat CameraX ile analyzer sözleşmesini tekrar birbirine bağlar.

Model içi birleştirme yapılabilir:

- `NanoDetPreprocessor`, `NanoDetPostprocessor` ve basit quality kontrolü `NanoDetDetector` içinde private fonksiyonlar olabilir.
- `OcrPreprocessor`, ROI okuma ve decoder `FastPlateOcr` altında private/internal bölümler olabilir.
- `FrameCoordinateMapper` ayrı, saf ve test edilebilir matematik olarak kalmalıdır.
- `PlateResultValidator` ayrı kalmalıdır; modelden ve camera'dan bağımsız ürün politikasıdır.

Bu birleştirme sırasında "sınıfı silmek" başarı ölçütü değildir. Detector veya OCR sonucunu dosya/cihaz olmadan test eden girişler korunmalıdır. Aksi halde bugünün küçük dosyaları, yarının test edilmesi zor büyük dosyalarına dönüşür.

## 7. Mapping ve quality gate sırası

```text
Detector-space bbox
  -> FrameCoordinateMapper
  -> source-frame bbox
  -> confidence + source-space minimum boyut + sınır kontrolü
  -> OCR ROI
```

`FrameCoordinateMapper` detector tarafından kullanılabilir; bu onu detector algoritmasının içine gömmez. Mapper'ın ayrı kalmasının sebebi 0/90/180/270 derece, mirror, crop/padding ve edge-case matematiğinin bağımsız test edilmesidir. Quality gate ise en az genişlik/yükseklik eşiğini kaynak pikselde değerlendirmelidir; detector 256×256 uzayındaki 32 piksel ile kaynak frame'deki 32 piksel aynı kalite anlamını taşımaz.

## 8. Runtime sahipliği ve eşzamanlılık

`PlateRecognitionRuntime`, mevcut manager/resources/lease yapısını azaltmak için iyi bir adaydır. Fakat runtime aşağıdaki sorulara kodla değil, public sözleşmeyle cevap vermelidir:

| Durum | Gerekli karar |
| --- | --- |
| İki camera aynı runtime'ı isterse | İlk sürümde ikinci kaydı reddetmek veya açık seri çalışma kuralı tanımlamak. |
| Handle kapanırsa | File-backed modda session kapanır mı; shared runtime modunda yalnızca kullanım hakkı mı bırakılır? |
| Model dosyası değişirse | Eski session kapanmadan yenisi hazır edilmez; aktif analyzer davranışı açık olur. |
| Düşük bellek / uygulama arka planı | Runtime'ın explicit `close()` çağrısı ve tekrar hazırlama yolu olmalıdır. |
| Warm-up hata verirse | `READY` verilmez; hata hem state hem kayıt çağrısında görünür. |

Mevcut manager'ın lease ayrımı bu ihtiyaç için yazılmıştır: analyzer kullanım hakkını bırakır, manager sahibi olduğu native session'ı yalnızca kendisi kapatır. Yeni runtime bunu daha az sınıfla yapabilir; fakat bu sahiplik davranışı kaybolmamalıdır.

Tracking bu kapsamdan ayrı tutulmalıdır. Varsayılan olarak kapalıdır ve tracker başlatma/yenileme maliyeti gerçek cihazda net fayda olarak doğrulanmamıştır. Refactor'un "tek pipeline" kabul kriteri, tracking açıkken frame erken bırakma ile eşzamanlı çalışma olasılığını açıkça ele almalıdır.

## 9. Revize uygulama sırası

### Faz 1 — Davranışı dondur

- Aktif 256×256/1360/33 detector ve OCR sözleşmesini test verisiyle kaydet.
- Detector output, source mapping, OCR metni/confidence, 2/3 consensus ve callback davranışını unit testlerle koru.
- Hedef cihazda p50/p95 bütün çevrim süresini, detector/OCR sürelerini ve frame drop sayılarını ayrı kaydet.

### Faz 2 — Resolved contract

- Ortak session açılışında metadata'yı al.
- Profile + metadata ile `ResolvedModelContract` oluştur.
- Mevcut sabitleri hemen silmeden resolved değerlerle eşitliği test et.
- Tek input/output, `FLOAT` türü ve profile uyumsuzluğunu fail-fast reddet.

### Faz 3 — Warm-up ve buffer'lar

- Input/output tensor tahsisini yalnızca resolved contract'tan üret.
- Warm-up'ı aynı buffer ve gerçek input shape ile çalıştır.
- Dynamic output desteğini kapsam dışı tut veya ayrı tasarım/ölçüm fazına taşı.

### Faz 4 — Dosya birleştirme

- NanoDet private işlemlerini detector altında, OCR private işlemlerini OCR altında topla.
- Mapper, validator ve camera adapter'ı ayrı bırak.
- Her bir birleşimde önce testleri taşı, sonra eski dosyayı sil.

### Faz 5 — Runtime sadeleştirme

- `PlateRecognitionRuntime`ın tek sahiplik noktası olduğunu doğrula.
- Birden çok camera, close, model değişimi ve düşük bellek kurallarını test et.
- Manager/resources/state dosyalarını ancak aynı davranış kanıtlandıktan sonra kaldır.

### Faz 6 — Cihaz doğrulaması

- Preview akıcılığı, callback gecikmesi, ilk sonuç gecikmesi ve tekrar kamera açılışı ölçülür.
- Tracking kapalı ve açık modlar ayrı ölçülür; teorik detector çağrısı tasarrufu başarı sayılmaz.

## 10. Refactor kabul kriterleri

### Fonksiyonel eşdeğerlik

- Aynı referans frame'inde detector kutuları, mapping sonrası kutular ve OCR metni değişmez.
- Source ROI, dönüş yönleri ve mirror davranışı korunur.
- Varsayılan 2/3 rolling consensus, yüzde 90 OCR eşiği ve başarı callback'i korunur.

### Kaynak güvenliği

- Her `ImageProxy` kabul, atlama, hata ve iptal yollarında bir kez kapanır.
- Session frame başına açılmaz.
- Runtime kapanırken detector ve OCR native kaynakları yalnızca bir kez kapanır.

### Sözleşme güvenliği

- ONNX node adı, tür ve shape metadata'dan gelir.
- Model profile semantiği ile metadata uyumu açılışta doğrulanır.
- Uyumlu görünen ama desteklenmeyen model sessizce çalışmaz; açık hata verir.
- Warm-up inference ile aynı resolved contract'ı kullanır.

### Performans ve bakım

- Preview akıcılığı yalnızca pipeline toplam süresinden çıkarılmaz; gerçek cihazda ölçülür.
- Her hata alanı en fazla ilgili camera, detector, mapper, OCR, validator veya runtime dosyasına bakılarak anlaşılabilir.
- Dosya sayısı azalsa da test sayısı ve hata görünürlüğü gerilemez.

## 11. Nihai karar

Planın ana fikri doğru, fakat güvenli uygulama şu sınırlar içinde kalmalıdır:

```text
ONNX metadata + explicit model profile
              -> resolved contract
              -> fail-fast validation
              -> reusable session/buffers
              -> detector -> mapper -> quality gate -> source ROI OCR -> validator
```

Bu sınırlarla sadeleştirme, gerçek gereksinimleri saklamadan fiziksel parçalanmayı azaltır. Bu sınırlar olmadan dynamic metadata veya runtime birleştirme, yanlış modelin sessizce kabul edilmesi, buffer uyuşmazlığı veya native kaynak yaşam döngüsünün bozulması riskini taşır.

## Kaynaklar

- [İncelenen sadeleştirme planı](C:/Users/enesb/Downloads/Android%20Plaka%20Tan%C4%B1ma%20Pipeline%20Sadele%C5%9Ftirme%20ve%20Dinamik%20ONNX%20Model%20S%C3%B6zle%C5%9Fmesi%20Plan%C4%B1.md)
- [NanoDetConfig.kt](../deveng-core/src/androidMain/kotlin/core/domain/camera/analysis/nanodet/NanoDetConfig.kt)
- [NanoDetDetector.kt](../deveng-core/src/androidMain/kotlin/core/domain/camera/analysis/nanodet/NanoDetDetector.kt)
- [NanoDetPostprocessor.kt](../deveng-core/src/androidMain/kotlin/core/domain/camera/analysis/nanodet/NanoDetPostprocessor.kt)
- [FrameCoordinateMapper.kt](../deveng-core/src/androidMain/kotlin/core/domain/camera/analysis/nanodet/FrameCoordinateMapper.kt)
- [FastPlateOcr.kt](../deveng-core/src/androidMain/kotlin/core/domain/camera/analysis/ocr/FastPlateOcr.kt)
- [PlateRecognitionModelManager.kt](../deveng-core/src/androidMain/kotlin/core/domain/camera/analysis/plate/PlateRecognitionModelManager.kt)
- [CameraFrameAnalyzerAdapter.kt](../deveng-core/src/androidMain/kotlin/core/domain/camera/analysis/CameraFrameAnalyzerAdapter.kt)

**E. B.**
