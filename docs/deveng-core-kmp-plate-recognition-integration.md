# Deveng Core KMP Camera
## Android Lokal Plaka Tespit ve OCR Entegrasyonu Teknik Tasarım Dokümanı

## 1. Amaç

Bu çalışmanın amacı, `deveng-core-kmp` içerisindeki mevcut kamera component'ini Android cihazlarda gerçek zamanlı görüntü analizi yapabilecek şekilde genişletmektir.

Kullanım senaryosu yol kenarı otopark uygulamasıdır.

Kullanıcı kamerayı açtığında sistem arka planda:

1. Kamera görüntüsünü takip edecek.
2. NanoDet-Plus modeli ile plakayı tespit edecek.
3. Plakanın görüntü içerisindeki konumunu belirleyecek.
4. Aynı kamera frame'i içerisindeki plaka bölgesini OCR için kullanacak.
5. `fast-plate-ocr` modeli ile plakayı okuyacak.
6. Sonucu doğrulayacak.
7. Başarılı sonuç alındığında kamera analizini sonlandıracak.
8. Okunan plakayı uygulamaya döndürecek.

Sistem tamamen Android cihaz üzerinde lokal çalışacaktır.

---

## 2. Mevcut Deveng Core Camera Yapısının Durumu

Yayımlanan `core-kmp-android` artifact'ı Android tarafında CameraX altyapısını kullanmaktadır. CameraX `camera-core`, `camera-lifecycle`, `camera-view` ve `camera-video` bağımlılıklarının kullanıldığı doğrulanabilmektedir.

Bu nedenle yeni bir kamera altyapısı yazılmamalıdır.

Mevcut yapı:

```text
Deveng Core Camera
        ↓
      CameraX
```

korunacak ve Android kamera implementation'ına yeni bir:

```text
ImageAnalysis
```

akışı eklenecektir.

CameraX `ImageAnalysis`, canlı kamera framelerinin gerçek zamanlı analiz edilmesi için sağlanan resmi use-case'tir.

---

## 3. Temel Mimari Karar

En önemli karar şudur:

> Camera component doğrudan NanoDet veya plaka tanıma sistemine bağımlı hale getirilmemelidir.

Camera'nın görevi:

```text
Frame üretmek
```

olmalıdır.

Frame'i analiz eden katman ayrı tutulmalıdır.

Dolayısıyla önerilen ilişki:

```text
Camera
   ↓
CameraFrameAnalyzer
   ↓
PlateRecognitionAnalyzer
   ↓
NanoDet + OCR
```

şeklindedir.

Şu tasarım önerilmez:

```text
Camera
   ↓
NanoDet
   ↓
OCR
```

Çünkü bu durumda genel amaçlı Deveng Camera component'i doğrudan plaka tanıma component'ine dönüşür.

---

## 4. Model Public Repository İçerisinde Bulunmayacak

Repository public olduğu için eğitilmiş:

```text
plate_detector.onnx
```

ve:

```text
fast_plate_ocr.onnx
```

dosyaları `deveng-core-kmp` repository'sine eklenmeyecektir.

Core yalnızca modeli çalıştırabilecek kodu içerecektir.

Modeli kullanan uygulama modeli kendi Android private storage alanında saklayacak ve core'a
`File` olarak sağlayacaktır.

Örneğin model:

```text
Application / filesDir
│
└── models
    ├── plate_detector.onnx
    └── fast_plate_ocr.onnx
```

Uygulama modelleri sunucudan indirebilir; indirme, bütünlük kontrolü ve atomik dosya güncelleme
consumer uygulamanın sorumluluğundadır.

---

## 5. Dosya Tabanlı Model Girdisi

Model girdisi için tek desteklenen kaynak Android private storage alanındaki normal dosyadır.
Tek varyantlı bir `ModelSource` abstraction'ı oluşturulmamalıdır.

Önerilen kavramsal kullanım:

```kotlin
val detectorModelFile = File(context.filesDir, "models/plate_detector.onnx")
val ocrModelFile = File(context.filesDir, "models/fast_plate_ocr.onnx")
```

`File`, ham path `String` yerine dosya varlığı, tipi, okunabilirliği ve boyutunun açıkça kontrol
edilebilmesi için tercih edilir.

Akış:

```text
Private storage .onnx File
             ↓
ONNX Runtime Session
```

şeklinde olur.

---

## 6. Model Doğrudan Camera'ya Verilmemeli

Şu API önerilmez:

```kotlin
DevengCamera(
    detectorModel = model
)
```

Bunun yerine:

```kotlin
DevengCamera(
    analyzer = analyzer
)
```

şeklinde generic bir API oluşturulmalıdır.

Plaka uygulaması kendi analyzer'ını:

```kotlin
val analyzer = PlateRecognitionAnalyzer(
    detectorModel = detectorModel,
    ocrModel = ocrModel
)
```

şeklinde oluşturabilir.

Sonra:

```kotlin
DevengCamera(
    analyzer = analyzer
)
```

olarak camera component'e aktarabilir.

Bu mimari sayesinde gelecekte aynı kamera:

```text
PlateRecognitionAnalyzer
QRAnalyzer
BarcodeAnalyzer
DocumentAnalyzer
FaceAnalyzer
```

gibi farklı analiz sistemleriyle kullanılabilir.

---

## 7. Önerilen Camera Analyzer Contract

Core içerisinde genel bir analyzer abstraction'ı oluşturulmalıdır.

Kavramsal olarak:

```kotlin
interface CameraFrameAnalyzer<R> {

    suspend fun analyze(
        frame: CameraFrame
    ): R?

    fun start() {}

    fun stop() {}

    fun close() {}
}
```

Buradaki amaç camera component'in model detaylarını bilmemesidir.

Camera sadece:

```text
frame geldi
↓
analyzer'a gönder
↓
sonuç varsa callback'e ilet
```

işlemini gerçekleştirir.

---

## 8. Android'e Özel Olması Konusu

Bu özelliğin ilk sürümü yalnızca Android için geliştirilecektir.

Bu nedenle KMP API tasarlanırken mevcut repository yapısına göre iki seçenek bulunmaktadır.

Eğer mevcut Camera API `expect/actual` yapısı kullanıyorsa generic analyzer contract common katmanda tanımlanabilir ancak gerçek frame analysis Android implementation'da gerçekleştirilmelidir.

Eğer Android camera component'i zaten ayrı bir API olarak kullanılabiliyorsa yeni analyzer desteğinin yalnızca Android API'sine eklenmesi daha temizdir.

Ama temel prensip değişmez:

```text
common/shared API
      ↓
platform implementation
      ↓
Android CameraX ImageAnalysis
```

Model inference kodu doğrudan `commonMain` içerisine taşınmamalıdır.

---

## 9. ONNX Runtime Bağımlılığının Core'a Etkisi

ONNX Runtime doğrudan ana `core-kmp` artifact'ına eklenirse, plaka taraması kullanmayan uygulamalar da bu bağımlılığı almak zorunda kalabilir.

Bu nedenle mümkünse repository içerisinde ayrı bir Android extension/module oluşturulması daha sağlıklıdır.

Örneğin:

```text
core-kmp
│
├── camera
│   └── generic camera + frame analysis
│
└── camera-onnx
    ├── ONNX Runtime
    ├── NanoDet detector
    └── OCR engine
```

veya mevcut repository mimarisine uygun benzer bir ayrım yapılabilir.

Bu mümkün değilse ONNX kodu en azından ayrı package altında tutulmalıdır.

Camera'nın temel API'si ONNX Runtime sınıflarını doğrudan expose etmemelidir.

---

## 10. Android CameraX ImageAnalysis Eklenmesi

Android Camera implementation'ına CameraX `ImageAnalysis` use-case'i eklenecektir.

Mevcut:

```text
CameraX
├── Preview
├── ImageCapture
└── VideoCapture
```

yapısına:

```text
CameraX
├── Preview
├── ImageCapture
├── VideoCapture
└── ImageAnalysis       ← YENİ
```

eklenecektir.

ImageAnalysis yalnızca bir analyzer verilmişse aktive edilebilir.

Analyzer verilmemişse mevcut Camera component davranışı aynen korunmalıdır.

Bu backward compatibility açısından önemlidir.

---

## 11. Backpressure Stratejisi

ImageAnalysis:

```text
STRATEGY_KEEP_ONLY_LATEST
```

ile çalıştırılmalıdır.

Bu modda analyzer'ın işleyemediği eski frameler kuyrukta biriktirilmez; mümkün olan en güncel frame analize verilir.

Böylece:

```text
Camera Preview → 30 FPS
```

çalışmaya devam ederken:

```text
ML Analysis → cihazın kaldırabildiği hız
```

seviyesinde çalışabilir.

Kamera görüntüsünün akıcılığı ML inference hızına bağlanmamalıdır.

---

## 12. Tek Bir Inference Aynı Anda Çalışmalı

Aynı anda:

```text
Frame 1 → NanoDet
Frame 2 → NanoDet
Frame 3 → NanoDet
```

çalıştırılmamalıdır.

Sistem:

```text
Frame
  ↓
Analyzer BUSY
  ↓
Yeni frameleri düşür
  ↓
Inference tamamlandı
  ↓
En güncel frame
```

mantığında çalışmalıdır.

Bu sayede:

- CPU contention azalır.
- RAM kullanımı kontrol altında kalır.
- Cihazın ısınması azaltılır.
- Gecikme birikmez.
- Kamera preview akışı korunur.

---

## 13. Ana Plate Recognition Pipeline

Tam çalışma akışı:

```text
CameraX
   ↓
ImageAnalysis
   ↓
ImageProxy / Source Frame
   ↓
Detector Preprocess
   ↓
NanoDet-Plus
   ↓
Plate Bounding Box
   ↓
Bounding Box Source Frame'e Map
   ↓
Quality Gate
   ↓
Original Frame ROI
   ↓
OCR Preprocess
   ↓
fast-plate-ocr
   ↓
Plate Result
   ↓
Validation
   ↓
SUCCESS
```

olacaktır.

---

## 14. NanoDet-Plus Modeli

Detection modeli:

```text
NanoDet-Plus
```

olacaktır.

Model kendi plaka dataset'imizle yalnızca:

```text
plate
```

sınıfı için eğitilecektir.

Eğitim sonrasında model ONNX formatına export edilecektir.

Deployment artifact:

```text
plate_detector.onnx
```

olacaktır.

---

## 15. NanoDet Model Contract

Core repository model dosyasını içermez; model Android private storage alanından `File` olarak
verilir. Faz 3 sırasında test uygulamasındaki gerçek `nanodet.onnx` dosyası incelenmiş ve aşağıdaki
contract doğrulanmıştır:

```text
Architecture:
NanoDet-Plus

Task:
Object Detection

Classes:
1

Class:
plate

Input:
name = data
type = FLOAT
shape = [1, 3, 416, 416]

Output:
name = output
type = FLOAT
shape = [1, 3598, 33]

Format:
ONNX

ONNX IR / opset:
10 / 18
```

Preprocessing contract'ı:

```text
CameraX YUV_420_888
↓ rotation correction
↓ 416 × 416 bilinear resize
↓ BGR channel order, CHW layout
↓ (pixel - mean) / std

mean = [103.53, 116.28, 123.675]
std  = [57.375, 57.12, 58.395]
```

Postprocessing contract'ı:

```text
class score channels = 1 (çıktı skoru hazır sigmoid değeridir)
reg_max = 7
box channels = 4 × 8 DFL logits
strides = [8, 16, 32, 64]
confidence threshold = 0.50
NMS IoU threshold = 0.60
detector coordinate range = 0..415
```

Android entegrasyonu model açılışında input/output node adı, veri tipi ve shape değerlerini doğrular.
Uyumsuz model fail-fast davranışıyla reddedilir. Threshold değerleri `NanoDetConfig` üzerinden
değiştirilebilir; varsayılanlar çalışan referans uygulamayla aynıdır.

```kotlin
NanoDetConfig(
    confidenceThreshold = 0.50f,
    nmsIouThreshold = 0.60f,
)
```

Bu varsayılanların nihai üretim değerleri olduğu varsayılmamalıdır; validation dataset sonuçlarına göre
ayarlanmalıdır.

---

## 16. Detector İçin Source Frame Kullanımı

Kameradan gelen görüntü önce detector'ın beklediği boyuta dönüştürülecektir.

Örneğin:

```text
Source Camera Frame
1920 × 1080

        ↓

Detector Preprocessing

        ↓

416 × 416

        ↓

NanoDet
```

Ancak burada önemli bir kural vardır:

> Detector için küçültülen 416×416 görüntü OCR için kullanılmamalıdır.

Çünkü OCR mümkün olan en kaliteli plaka görüntüsünü görmelidir.

---

## 17. Bounding Box'ın Source Frame'e Geri Dönüştürülmesi

NanoDet sonucu detector koordinat sisteminde üretilecektir.

Örneğin:

```text
416 × 416

x1
y1
x2
y2
```

Bu koordinatlar source camera frame'e geri dönüştürülecektir.

Bu aşamada preprocessing sırasında yapılan:

```text
resize
letterbox
padding
rotation
crop
mirror
```

işlemleri hesaba katılmalıdır.

Bunun için detector preprocessing sonucu yalnızca tensor değil ayrıca bir transform bilgisi üretmelidir.

Örneğin kavramsal olarak:

```text
DetectorInput
+
FrameTransform
```

oluşturulur.

Inference sonrasında:

```text
Detector BBox
        +
FrameTransform
        ↓
Source Frame BBox
```

hesaplanır.

Bu katman bağımsız unit testlerle doğrulanmalıdır.

Yanlış koordinat mapping'i detector doğru çalışsa bile OCR'a yanlış görüntü verilmesine neden olur.

---

## 18. Plaka Görüntüsü Ayrı Bir Dosya Olarak Crop Edilmeyecek

Pipeline'ın önemli performans optimizasyonlarından biri budur.

Şu akış kesinlikle yapılmamalıdır:

```text
Frame
↓
Bitmap
↓
Crop Bitmap
↓
JPEG Encode
↓
Dosyaya Kaydet
↓
Tekrar Oku
↓
OCR
```

Bunun yerine:

```text
Source Frame
+
Plate BBox
        ↓
ROI
        ↓
OCR Tensor
```

kullanılacaktır.

Yani sistem:

```text
cropped_plate.jpg
```

gibi bir intermediate image oluşturmayacaktır.

---

## 19. OCR Tensor'ı Doğrudan ROI'den Oluşturulmalı

NanoDet source frame üzerinde plakanın koordinatını verdikten sonra:

```text
Source Frame
       +
Plate Bounding Box
```

OCR preprocessing'e aktarılacaktır.

OCR preprocessing:

```text
ROI seç
↓
resize
↓
normalize
↓
OCR Tensor
```

işlemlerini gerçekleştirecektir.

Sonuç doğrudan:

```text
fast-plate-ocr
```

modeline verilecektir.

Ara aşamada `Bitmap`, JPEG veya fiziksel dosya oluşturulmamalıdır.

---

## 20. ImageProxy Ne Zaman Kapatılmalı?

Pipeline açısından önemli lifecycle sırası şöyledir:

```text
ImageProxy
   ↓
NanoDet preprocessing
   ↓
NanoDet inference
   ↓
BBox
   ↓
Source ROI
   ↓
OCR input tensor oluştur
   ↓
ImageProxy artık gerekli değil
   ↓
ImageProxy.close()
   ↓
OCR inference
```

OCR tensor'ı hazırlandıktan sonra source frame serbest bırakılabilir.

Böylece:

- cropped image oluşturulmaz,
- frame tekrar okunmaz,
- camera buffer gereğinden uzun tutulmaz.

---

## 21. Reusable Buffer Kullanımı

Her frame'de:

```text
new Bitmap
new ByteArray
new FloatArray
new Buffer
```

oluşturulmamalıdır.

Mümkün olduğunca:

```text
Detector Input Buffer
OCR Input Buffer
Temporary Conversion Buffer
```

bir kez allocate edilip tekrar kullanılmalıdır.

Amaç:

```text
Frame 1 → aynı buffers
Frame 2 → aynı buffers
Frame 3 → aynı buffers
```

şeklinde çalışmaktır.

Bu özellikle Android tarafında gereksiz GC baskısını ve latency spike'larını azaltacaktır.

---

## 22. Quality Gate

Her NanoDet detection sonucu doğrudan OCR'a gönderilmemelidir.

Detection sonrasında çok hızlı çalışan bir quality gate bulunmalıdır.

Kontrol edilebilecek kriterler:

- Detection confidence yeterli mi?
- Plaka görüntü içerisinde yeterince büyük mü?
- Bounding box frame sınırlarında mı?
- Plaka ROI'si OCR yapılabilecek kadar net mi?
- Bounding box geometrisi mantıklı mı?

Örneğin:

```text
NanoDet
   ↓
BBox
   ↓
Quality Gate
   ├── BAD → OCR çalıştırma
   │
   └── GOOD
          ↓
         OCR
```

Bu sayede gereksiz OCR inference sayısı azaltılabilir.

---

## 23. Bounding Box Padding

Detector'ın bounding box'ı doğrudan OCR'a vermeden önce küçük miktarda genişletilebilir.

Amaç detector'ın plakanın ilk veya son karakterini birkaç pixel kesme ihtimalini azaltmaktır.

Örneğin:

```text
Detector BBox
      ↓
Configurable Padding
      ↓
OCR ROI
```

yapılabilir.

Padding miktarı sabit bir varsayım olarak belirlenmemeli, validation dataset üzerinden test edilmelidir.

---

## 24. OCR Modeli

OCR modeli mevcut:

```text
fast-plate-ocr.onnx
```

modelidir.

Detection ve OCR modellerinin ikisi de ONNX olduğu için Android tarafında tek inference runtime kullanılması önerilir:

```text
ONNX Runtime
├── NanoDet
└── fast-plate-ocr
```

İlk implementation için non-quantized modellerde XNNPACK başlangıç seçeneği olarak değerlendirilebilir.

Ancak gerçek karar kullanılacak POS/Android cihazlarında benchmark sonucuna göre verilmelidir.

---

## 25. ONNX Session Lifecycle

Her frame geldiğinde model yeniden yüklenmemelidir.

Yanlış:

```text
Frame
↓
Model load
↓
Inference
↓
Dispose
```

Doğru:

```text
PlateRecognitionAnalyzer oluştur
↓
NanoDet Session oluştur
↓
OCR Session oluştur

------------------

Frame
↓
Inference

Frame
↓
Inference

Frame
↓
Inference

------------------

Analyzer dispose
↓
Sessions dispose
```

---

## 26. Analyzer Lifecycle

Analyzer'ın lifecycle'ı açıkça tanımlanmalıdır.

Önerilen durumlar:

```text
CREATED
↓
INITIALIZING
↓
READY
↓
ANALYZING
↓
SUCCESS
```

ve hata durumunda:

```text
ERROR
```

Camera kapatıldığında:

```text
STOPPED
↓
DISPOSED
```

durumları oluşabilir.

Model yüklemesi başarısızsa kamera crash olmamalıdır.

Uygulamaya anlamlı bir hata dönülmelidir.

---

## 27. Sonuç Modeli

Camera'nın uygulamaya döndürdüğü sonuç generic tutulabilir.

Plate analyzer tarafında örneğin:

```kotlin
data class PlateRecognitionResult(
    val plate: String,
    val confidence: Float
)
```

gibi bir yapı kullanılabilir.

İhtiyaç olursa daha sonra:

```text
detection confidence
OCR confidence
bounding box
timestamp
```

gibi debugging alanları eklenebilir.

---

## 28. OCR Sonucu Doğrulama

İlk başarılı OCR sonucu geldiği anda kamera ekranından ayrılmak yerine 2/3 rolling consensus doğrulaması uygulanır. Yalnızca minimum OCR confidence eşiğini geçen, boş olmayan sonuçlar geçerli okuma sayılır.

Örneğin:

```text
Geçerli okuma 1 → 34ABC123 (%95)
Geçerli okuma 2 → 34ABC124 (%93)
Geçerli okuma 3 → 34ABC123 (%97)
                       ↓
             34ABC123 = 2/3
                       ↓
                    SUCCESS
```

Eşik altındaki OCR sonucu pencereye eklenmez ve mevcut pencereyi sıfırlamaz. İlk üç geçerli okumada consensus oluşmazsa yeni geçerli okuma geldiğinde en eski sonuç çıkarılır ve son üç sonuç yeniden değerlendirilir.

Pencere, gerekli eşleşme sayısı ve OCR confidence eşiği configurable olmalıdır.

Örneğin:

```text
requiredMatchingReads = 2
validationWindowSize = 3
ocrConfidenceThreshold = 0.90f
```

gibi.

Doğrulanan sonucun confidence değeri, consensus'u destekleyen okumaların ortalamasıdır. Kullanıcı deneyimi açısından doğrulama süresinin gereksiz yere uzamaması gerekir.

---

## 29. Başarılı Sonuç Sonrası

Başarılı OCR sonucu oluştuğunda:

```text
PlateRecognition SUCCESS
        ↓
Yeni analysis durdur
        ↓
Camera callback
        ↓
"34ABC123"
        ↓
Camera UI kapat
        ↓
Uygulamadaki plaka alanını doldur
```

akışı uygulanacaktır.

---

## 30. Preview ve ML Birbirinden Ayrılmalı

Kullanıcının gördüğü kamera preview:

```text
Preview
```

ile;

arka plandaki:

```text
ImageAnalysis
```

pipeline'ı birbirinden bağımsız tutulmalıdır.

Ana kullanıcı deneyimi gereksinimi:

> Detector yavaşlasa bile kamera preview takılmamalıdır.

Bu nedenle blocking strateji yerine `STRATEGY_KEEP_ONLY_LATEST` kullanılmalıdır.

---

## 31. Detector'ın Kamera FPS'i Kadar Çalışması Gerekmiyor

Örneğin:

```text
Camera Preview
30 FPS
```

iken detector:

```text
8–15 inference/sec
```

çalışabilir.

Buradaki değer kesin requirement değildir; cihaz benchmark'ına göre belirlenecektir.

Plaka tanıma sisteminin bütün frameleri analiz etmesi gerekmez.

Amaç:

```text
ilk iyi frame'i mümkün olduğunca hızlı yakalamak
```

olmalıdır.

---

## 32. OCR Sürekli Çalışmayacak

Normal pipeline:

```text
NanoDet
NanoDet
NanoDet
NanoDet
```

şeklinde olacaktır.

OCR yalnızca:

```text
NanoDet
↓
Geçerli Plate Detection
↓
Quality Gate PASS
↓
OCR
```

olduğunda çalışacaktır.

Bu sayede OCR'ın sürekli CPU tüketmesi engellenir.

---

## 33. Thread / Executor Yapısı

En azından şu işler birbirinden ayrılmalıdır:

```text
UI Thread
    ↓
Compose / UI

CameraX
    ↓
Preview

Analysis Executor
    ↓
Frame Analysis

Inference
    ↓
NanoDet / OCR
```

ML inference kesinlikle UI thread üzerinde çalıştırılmamalıdır.

Aynı zamanda aşırı sayıda inference thread açılmamalıdır.

Thread sayısı gerçek POS/Android cihazlarında benchmark edilmelidir.

---

## 34. Önerilen Package Yapısı

Exact repository dosya yapısına bağlı olarak isimler değişebilir ancak mantıksal ayrım şu şekilde olmalıdır:

```text
camera/
│
├── Camera
├── CameraConfig
├── CameraState
├── CameraFrameAnalyzer
└── CameraAnalysisResult

androidMain/
│
├── camera/
│   ├── CameraX implementation
│   └── ImageAnalysis implementation
│
└── camera/analysis/
    ├── onnx/OnnxModelSession
    ├── PlateRecognitionAnalyzer
    ├── NanoDetDetector
    ├── NanoDetPreprocessor
    ├── NanoDetPostprocessor
    ├── FrameCoordinateMapper
    ├── PlateQualityGate
    ├── FastPlateOcr
    ├── OcrPreprocessor
    └── PlateResultValidator
```

Model dosyaları burada bulunmayacaktır.

---

## 35. Daha İyi Modüler Yapı

Repository yapısı izin veriyorsa daha iyi tasarım:

```text
core-camera
│
├── Camera
├── CameraX
├── ImageAnalysis
└── CameraFrameAnalyzer
```

ve ayrı:

```text
core-camera-onnx
│
├── ONNX Runtime
├── NanoDet
├── OCR
└── PlateRecognitionAnalyzer
```

şeklindedir.

Bunun avantajı normal Deveng Core kullanıcılarının ONNX bağımlılığını almak zorunda kalmamasıdır.

---

## 36. Consumer Uygulamanın Kullanımı

Uygulama tarafındaki hedef kullanım yaklaşık şu kadar basit olmalıdır:

```text
Detector Model
      +
OCR Model
      ↓
PlateRecognitionAnalyzer
      ↓
Deveng Camera
      ↓
Result
```

Kavramsal API:

```kotlin
val analyzer = PlateRecognitionAnalyzer(
    detectorModelFile = detectorModelFile,
    ocrModelFile = ocrModelFile
)

DevengCamera(
    analyzer = analyzer,
    onAnalysisResult = { result ->
        // Result: 34ABC123
    }
)
```

Core kullanan geliştirici:

- CameraX,
- NanoDet preprocessing,
- bbox mapping,
- OCR preprocessing,
- ONNX Runtime session yönetimi

ile uğraşmamalıdır.

Sadece modelleri sağlayıp sonucu almalıdır.

---

## 37. Configurable API

PlateRecognitionAnalyzer'ın temel davranışları config üzerinden yönetilebilir.

Örneğin:

```text
Detector Input Size
Detection Confidence
NMS Threshold
BBox Padding
Required OCR Match Count
Analysis Throttle
Quality Gate
Execution Provider
```

gibi değerler configurable olabilir.

Ancak kullanıcıyı onlarca zorunlu parametre ile karşılaştırmamak gerekir.

İyi default değerler olmalı ve sadece gelişmiş kullanım durumlarında override edilmelidir.

---

## 38. Error Handling

Şu durumlar ayrı ayrı ele alınmalıdır:

```text
Camera permission denied
Model bulunamadı
Model açılamadı
ONNX session oluşturulamadı
Detector inference hatası
OCR inference hatası
Geçersiz output shape
Frame conversion hatası
Camera kapandı
Analyzer dispose edildi
```

Model kaynaklı hata CameraX preview'ın crash olmasına neden olmamalıdır.

Analyzer hatası callback/state olarak uygulamaya döndürülmelidir.

---

## 39. Model Uyumluluk Kontrolü

Core'a rastgele bir ONNX modeli verilmesini engellemek için analyzer initialization aşamasında model contract doğrulanmalıdır.

Örneğin:

```text
Model açıldı
↓
Input kontrol
↓
Output kontrol
↓
Shape kontrol
↓
Supported?
```

uygulanmalıdır.

Uyumsuz model varsa uygulama ilk inference'a kadar beklememelidir.

Initialization aşamasında anlamlı hata vermelidir.

---

## 40. Test Stratejisi

Implementation sonrasında dört seviyeli test yapılmalıdır.

### 40.1 Unit Test

Özellikle:

```text
Bounding Box Mapping
Letterbox Reverse Mapping
Rotation Mapping
Padding
Plate Validation
OCR Result Validation
```

test edilmelidir.

### 40.2 Model Test

Sabit test görüntüleri:

```text
Image
↓
NanoDet
↓
Expected BBox
```

ve:

```text
Plate ROI
↓
OCR
↓
Expected Plate
```

olarak test edilmelidir.

### 40.3 Pipeline Test

```text
Frame
↓
Detection
↓
ROI
↓
OCR
↓
Result
```

uçtan uca test edilmelidir.

### 40.4 Gerçek Cihaz Testi

Son aşamada gerçek POS/Android cihazlarında:

```text
Preview FPS
Detection latency
OCR latency
Total recognition time
CPU
RAM
Temperature
```

ölçülmelidir.

---

## 41. Benchmark'ta Ölçülmesi Gereken Süreler

Debug/benchmark modunda şu sürelerin ayrı tutulması önerilir:

```text
frame_preprocess_ms
detector_inference_ms
detector_postprocess_ms
coordinate_mapping_ms
ocr_preprocess_ms
ocr_inference_ms
validation_ms
total_ms
```

Production modunda detaylı metric toplama kapatılabilir.

Bu ayrım darboğazın:

```text
Camera conversion mı?
NanoDet mi?
OCR mı?
Memory allocation mı?
```

olduğunu anlamamızı sağlar.

---

## 42. Kullanıcı Deneyimi Kabul Kriteri

En önemli acceptance kriteri yalnızca model latency değildir.

Ana kriter:

> Kullanıcı kamerayı kullanırken ML pipeline'ının çalıştığını hissetmemelidir.

Dolayısıyla:

```text
Camera preview akıcı
+
iyi açı yakalandığında hızlı sonuç
```

hedeflenmelidir.

ML inference bütün framelere yetişemiyorsa frame düşürmek kabul edilebilir.

Preview FPS düşürmek kabul edilmemelidir.

---

## 43. İlk Implementation'da Yapılmaması Gerekenler

İlk sürümde aşağıdaki yaklaşımlardan kaçınılmalıdır:

```text
Her frame'i kuyruğa almak

Main Thread'de inference

Her frame Bitmap oluşturmak

JPEG encode/decode yapmak

Plakayı fiziksel dosyaya kaydetmek

OCR için detector'ın 416×416 görüntüsünü kullanmak

Her frame'de ONNX session oluşturmak

NanoDet ve OCR'ı aynı anda birçok frame üzerinde paralel çalıştırmak

Model dosyalarını public core repository'ye commit etmek

Camera component'i NanoDet'e doğrudan bağlamak
```

---

## 44. Adım Adım Implementation Planı

### Adım 1 — Mevcut Camera API korunacak

Öncelikle mevcut camera component'in public API'si ve davranışı korunmalıdır.

Yeni analiz özelliği optional olmalıdır.

### Adım 2 — Generic Analyzer Contract oluşturulacak

Camera ile ML sistemi arasında:

```text
CameraFrameAnalyzer
```

abstraction'ı oluşturulacaktır.

### Adım 3 — Android ImageAnalysis eklenecek

CameraX Android implementation'a:

```text
ImageAnalysis
```

use-case'i eklenecektir.

Analyzer yoksa aktif edilmeyecektir.

### Adım 4 — KEEP_ONLY_LATEST uygulanacak

Frame queue oluşması engellenecektir.

Her zaman mümkün olan en güncel frame analiz edilecektir.

### Adım 5 — Dosya tabanlı model girdisi eklenecek

Consumer uygulamanın private storage alanındaki:

```text
plate_detector.onnx
fast_plate_ocr.onnx
```

dosyaları `File` olarak alınacaktır. Session oluşturulmadan önce dosyanın varlığı, normal dosya
olması, okunabilirliği ve boş olmaması doğrulanacaktır. `ByteArray`, `ByteBuffer` ve `ModelSource`
eklenmeyecektir.

### Adım 6 — ONNX Runtime entegrasyonu yapılacak

Detector ve OCR için ortak:

```text
OrtEnvironment
```

kullanılacak ve iki ayrı session oluşturulacaktır.

### Adım 7 — NanoDetDetector yazılacak

Sorumluluğu yalnızca:

```text
Frame
↓
Detector Tensor
↓
NanoDet
↓
Plate BBox
```

olacaktır.

Faz 3'te bu adım tamamlandı. `NanoDetDetector`, CameraX `YUV_420_888` frame'ini ara JPEG/Bitmap
üretmeden normalize BGR-CHW tensöre dönüştürür, `[1, 3598, 33]` çıktısını DFL ile çözer ve NMS
uygular. Sonuç bbox değerleri şimdilik 416×416 detector koordinat sistemindedir.

### Adım 8 — Coordinate Mapper eklendi

Detector'ın 416×416 koordinatları source camera frame koordinatlarına dönüştürülüyor.

Bu dönüşüm bağımsız `FrameCoordinateMapper` component'i ile yapılır.

`FrameTransform`, rotation, ölçek, padding ve mirror durumunu preprocessing sonucu ile birlikte taşır.
`FrameCoordinateMapper`, detector bbox'ını bu bilgilerle source frame bbox'ına çevirir. Faz 3'ün
raw `PlateDetection` sonucu detector-space debug ve NMS için korunur; normal NanoDet analyzer
sonucu `SourcePlateDetection` olarak source-frame koordinatlarını taşır.

### Adım 9 — Quality Gate eklenecek

Her detection'ın OCR'a gitmesi engellenecektir.

### Adım 10 — OCR Preprocessor yazılacak

Source frame ve bbox alacaktır.

Intermediate görüntü oluşturmadan:

```text
Frame ROI
↓
OCR Tensor
```

üretecektir.

### Adım 11 — fast-plate-ocr entegre edilecek

Hazırlanan tensor mevcut ONNX OCR modeline gönderilecektir.

### Adım 12 — Frame erken release edilecek

OCR tensor oluşturulduktan sonra source CameraX frame kapatılacaktır.

OCR inference source frame'e ihtiyaç duymadan devam edecektir.

### Adım 13 — Result Validator eklenecek

Bir veya birkaç OCR sonucu üzerinden final plaka belirlenecektir.

### Adım 14 — Doğrulanmış sonuç consumer application'a iletilecek

Final sonuç üretildiğinde `onResult(PlateRecognitionResult)` çağrılacaktır. Analyzer, başarı
kilidi etkinse yeni inference kabul etmez; kamera use-case'lerini kapatma, analizi unregister
etme, ekran değiştirme ve sonucu kullanma consumer application'ın kararındadır.

```text
SUCCESS
↓
onResult(PlateRecognitionResult)
↓
Consumer Application Decision
```

Bu aşamada library, sonuç-sonrası uygulama davranışını yönetmez.

### Adım 15 — Buffer reuse uygulanacak

Detector ve OCR input buffer'ları mümkün olduğunca tekrar kullanılacaktır.

### Adım 16 — Benchmark sistemi eklenecek

Pipeline'ın bütün aşamaları gerçek Android cihazında ölçülecektir.

### Adım 17 — Optimization yapılacak

Ancak baseline çalıştıktan sonra:

```text
FP16
INT8
XNNPACK
CPU thread tuning
analysis throttling
ROI optimizasyonu
```

gibi optimizasyonlar değerlendirilmelidir.

Başlangıçta sistemi gereksiz yere karmaşıklaştırmamak gerekir.

---

## 45. Nihai Mimari

```text
                    CONSUMER APPLICATION
                            │
              ┌─────────────┴─────────────┐
              │                           │
      NanoDet ONNX Model          OCR ONNX Model
              │                           │
              └─────────────┬─────────────┘
                            │
                            ▼
                  PlateRecognitionAnalyzer
                            │
                            ▼
                     DEVENG CAMERA
                            │
                            ▼
                         CameraX
                            │
                ┌───────────┴───────────┐
                │                       │
                ▼                       ▼
             Preview               ImageAnalysis
                │                       │
                │                       ▼
                │                  Source Frame
                │                       │
                │                       ▼
                │              NanoDet Preprocess
                │                       │
                │                       ▼
                │                  NanoDet ONNX
                │                       │
                │                       ▼
                │                   Plate BBox
                │                       │
                │                       ▼
                │               Coordinate Mapping
                │                       │
                │                       ▼
                │                  Quality Gate
                │                       │
                │                       ▼
                │              Source Frame + ROI
                │                       │
                │                       ▼
                │                OCR Preprocess
                │                       │
                │                OCR Tensor Ready
                │                       │
                │                 Release Frame
                │                       │
                │                       ▼
                │                fast-plate-ocr
                │                       │
                │                       ▼
                │                  "34ABC123"
                │                       │
                │                       ▼
                │                  Validation
                │                       │
                │                       ▼
                │                    SUCCESS
                │                       │
                └───────────────────────┤
                                        ▼
                    onResult(PlateRecognitionResult)
                                        │
                                        ▼
                        Consumer Application Decision
```

---

## 46. Sorumlulukların Nihai Ayrımı

### Deveng Camera

Sorumlu:

```text
Camera açma
Preview
ImageAnalysis
Frame lifecycle
Analyzer çağırma
Sonuç döndürme
```

Sorumlu değil:

```text
Plaka nedir?
NanoDet nedir?
OCR nedir?
```

### PlateRecognitionAnalyzer

Sorumlu:

```text
NanoDet
Detection
Bounding box
Coordinate mapping
Quality gate
OCR
Validation
```

### Consumer Application

Sorumlu:

```text
Eğitilmiş modelleri sağlamak
Camera'yı açmak
`onResult` sonrası sonucu kullanmak
Kendi uygulama akışına göre analizi veya kamerayı yönetmek
```

---

## 47. Nihai Kullanım Felsefesi

Public repository:

```text
KOD
```

sağlayacaktır.

Consumer:

```text
MODEL
```

sağlayacaktır.

Camera:

```text
FRAME
```

sağlayacaktır.

Analyzer:

```text
FRAME + MODEL → RESULT
```

işlemini gerçekleştirecektir.

Dolayısıyla sistemin temel prensibi:

```text
Public Core
    +
Private Storage Model Files
    =
On-device Plate Recognition
```

olacaktır.

---

## 48. Sonuç

Mevcut Deveng Core camera altyapısının CameraX tabanlı olması nedeniyle bu özellik için yeni bir kamera sistemi yazmaya gerek yoktur. Android tarafındaki mevcut camera implementation `ImageAnalysis` ile genişletilmelidir.

En önemli mimari karar model dosyalarının repository içerisinde tutulmaması ve camera component'in model mimarisine doğrudan bağlanmamasıdır.

Önerilen yapı:

```text
Deveng Camera
      ↓
Generic Frame Analyzer
      ↓
PlateRecognitionAnalyzer
      ↓
External NanoDet Model
      +
External OCR Model
```

şeklindedir.

Performans açısından en kritik pipeline kararı ise:

```text
NanoDet
↓
BBox
↓
Aynı Source Frame'in ROI'si
↓
Doğrudan OCR Tensor
```

yaklaşımıdır.

Bu sayede:

```text
crop image oluşturma
JPEG encode
dosyaya yazma
dosyadan tekrar okuma
Bitmap decode
```

gibi gereksiz aşamalar tamamen ortadan kaldırılabilir.

Sonuç olarak hedef sistem:

> Kullanıcının mevcut Deveng Camera component'ini normal şekilde açtığı, modellerini dışarıdan verdiği ve kamerayı plakaya doğrulttuğunda arka planda NanoDet + OCR pipeline'ının çalışarak sonucu otomatik döndürdüğü genel, tekrar kullanılabilir ve performans odaklı bir Android camera analysis altyapısı

olmalıdır.
