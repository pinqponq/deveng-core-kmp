# Deveng Core KMP Camera
# Android Plaka Tespit + OCR Entegrasyonu — Detaylı İş Planı

> **Kapsam:** Modeller hazırdır. Bu plan yalnızca Android/Kotlin tarafındaki entegrasyon, CameraX frame akışı, ONNX Runtime inference, bellek/lifecycle yönetimi, `deveng-core-kmp` camera component entegrasyonu, test ve performans doğrulamasını kapsar.

---

# 1. Hedef

Amaç, `deveng-core-kmp` içerisindeki mevcut Android camera component'ini gerçek zamanlı plaka analizi yapabilecek şekilde genişletmektir.

Kullanıcı açısından hedef akış:

```text
Kamera açılır
    ↓
Preview akıcı şekilde gösterilir
    ↓
Arka planda güncel camera frame'leri analiz edilir
    ↓
NanoDet-Plus plakayı bulur
    ↓
BBox source frame koordinatlarına çevrilir
    ↓
Aynı source frame'in yalnızca plaka ROI'sinden OCR tensor oluşturulur
    ↓
fast-plate-ocr çalışır
    ↓
Sonuç doğrulanır
    ↓
Analiz durur
    ↓
Kamera kapanır
    ↓
Plaka uygulamaya döner
```

Temel kullanıcı deneyimi hedefi:

> ML pipeline çalışırken kullanıcı preview tarafında gözle görülür FPS kaybı yaşamamalıdır.

---

# 2. Ana Mimari Kararlar

Bu çalışma başlamadan önce aşağıdaki kararlar sabit kabul edilmelidir.

## 2.1 Camera component modelden habersiz olacak

`deveng-core-kmp` camera component:

- NanoDet bilmeyecek.
- OCR bilmeyecek.
- ONNX model formatını bilmek zorunda olmayacak.
- Plaka domain'ine bağımlı olmayacak.

Camera'nın görevi:

```text
Camera aç
↓
Preview üret
↓
Frame üret
↓
Frame'i analyzer'a ver
↓
Analyzer sonucunu consumer'a ilet
```

olacaktır.

---

## 2.2 Model dosyaları public repository'ye eklenmeyecek

Aşağıdaki dosyalar core repository içerisinde tutulmayacaktır:

```text
plate_detector.onnx
fast_plate_ocr.onnx
```

Consumer application modelleri kendi private storage alanında dosya olarak tutacak ve core'a
`File` olarak verecektir.

Örneğin:

```text
Consumer App / filesDir
│
└── models/
    ├── plate_detector.onnx
    └── fast_plate_ocr.onnx
```

---

## 2.3 Camera API'ye model değil analyzer verilecek

Önerilen yaklaşım:

```kotlin
DevengCamera(
    analyzer = plateRecognitionAnalyzer
)
```

Önerilmeyen yaklaşım:

```kotlin
DevengCamera(
    detectorModel = plateDetector,
    ocrModel = ocrModel
)
```

Böylece camera component genel amaçlı kalır.

---

# 3. Önerilen Katmanlar

Sistem mantıksal olarak dört ana katmana ayrılmalıdır.

```text
┌────────────────────────────┐
│ Consumer Application       │
└─────────────┬──────────────┘
              │
              ▼
┌────────────────────────────┐
│ Deveng Camera API          │
│ Preview + Analysis Bridge  │
└─────────────┬──────────────┘
              │
              ▼
┌────────────────────────────┐
│ PlateRecognitionAnalyzer   │
│ Detection + OCR Pipeline   │
└─────────────┬──────────────┘
              │
              ▼
┌────────────────────────────┐
│ ONNX Runtime               │
│ NanoDet + fast-plate-ocr   │
└────────────────────────────┘
```

---

# 4. Repo İçerisinde Önerilen Sorumluluk Ayrımı

Exact dosya adları mevcut repository yapısına göre uyarlanmalıdır ancak sorumluluklar şu şekilde ayrılmalıdır.

```text
camera/
├── CameraConfig
├── CameraState
├── CameraFrame
├── CameraFrameAnalyzer
├── CameraAnalysisResult
└── CameraError

androidMain/
├── camera/
│   ├── CameraXController
│   ├── CameraXPreview
│   ├── CameraXImageAnalysis
│   └── AndroidCameraFrame
│
└── analysis/
    ├── PlateRecognitionAnalyzer
    ├── onnx/OnnxModelSession
    ├── NanoDetDetector
    ├── NanoDetPreprocessor
    ├── NanoDetPostprocessor
    ├── FrameCoordinateMapper
    ├── PlateQualityGate
    ├── OcrPreprocessor
    ├── FastPlateOcr
    ├── PlateResultValidator
    └── AnalysisMetrics
```

Mümkünse ONNX ile ilgili sınıflar ayrı module/package altında tutulmalıdır.

---

# 5. Public API Tasarımı

## 5.1 Generic analyzer contract

Önerilen contract:

```kotlin
interface CameraFrameAnalyzer<R> {
    suspend fun analyze(frame: CameraFrame): R?

    fun start() {}
    fun stop() {}
    fun close() {}
}
```

Amaç:

- Camera yalnızca frame sağlar.
- Analyzer sonucu üretir.
- Analyzer lifecycle'ı açıkça yönetilir.

---

## 5.2 Camera kullanım şekli

Kavramsal kullanım:

```kotlin
val analyzer = PlateRecognitionAnalyzer(
    detectorModelFile = detectorModelFile,
    ocrModelFile = ocrModelFile
)

DevengCamera(
    analyzer = analyzer,
    onAnalysisResult = { result ->
        // final plate result
    },
    onAnalysisError = { error ->
        // handle error
    }
)
```

---

# 6. Dosya Tabanlı Model Girdisi

Modeller consumer uygulamanın Android private storage alanında normal dosya olarak bulunmalıdır.
Core API modeli `java.io.File` ile almalıdır.

Önerilen kullanım:

```kotlin
val detectorModelFile = File(context.filesDir, "models/plate_detector.onnx")
val ocrModelFile = File(context.filesDir, "models/fast_plate_ocr.onnx")
```

Amaç:

- Tek ve açık model sahipliği sağlamak.
- Büyük modelleri JVM heap'e `ByteArray` olarak yüklememek.
- Model güncelleme ve sürüm yönetimini consumer uygulamada bırakmak.
- Gereksiz `ModelSource`, `ByteArray` ve `ByteBuffer` abstraction'larından kaçınmak.

Core; model indirme, hash doğrulama veya dosyayı private storage'a atomik olarak taşıma
sorumluluğunu üstlenmez. Session oluşturulmadan önce dosyanın varlığı, okunabilirliği ve boş
olmaması doğrulanmalıdır.

---

# 7. Analyzer Initialization Planı

`PlateRecognitionAnalyzer` oluşturulduğunda inference session'ları hemen veya lazy olarak hazırlanmalıdır.

Önerilen lifecycle:

```text
CREATED
   ↓
INITIALIZING
   ↓
NanoDet session create
   ↓
OCR session create
   ↓
Input/output contract validation
   ↓
Reusable buffers allocate
   ↓
READY
```

Başarısız durumda:

```text
INITIALIZING
   ↓
ERROR
```

Camera açılmadan önce analyzer'ın hazır olması tercih edilir.

Amaç kullanıcının kamerayı açtıktan sonra model yüklenmesini beklememesidir.

---

# 8. ONNX Runtime Session Yönetimi

Tek bir analyzer instance için:

```text
OrtEnvironment
├── NanoDet Session
└── OCR Session
```

oluşturulmalıdır.

Kurallar:

- Session her frame için oluşturulmayacak.
- Session her inference sonunda dispose edilmeyecek.
- Analyzer yaşam döngüsü boyunca tekrar kullanılacak.
- Camera kapatılırken analyzer yeniden kullanılacaksa session açık kalabilir.
- Analyzer tamamen dispose edildiğinde session'lar kapatılmalıdır.

---

# 9. Android Camera Başlatma Akışı

Kullanıcı kamera ekranını açtığında:

```text
Camera screen enter
    ↓
Camera permission check
    ↓
Analyzer READY mi?
    ↓
CameraProvider al
    ↓
Preview oluştur
    ↓
ImageAnalysis oluştur
    ↓
Lifecycle'a bind et
    ↓
Preview kullanıcıya göster
    ↓
Analysis frame üretmeye başla
```

Ana prensip:

> Preview ve ImageAnalysis iki ayrı use-case gibi davranmalıdır.

---

# 10. CameraX Use-Case Yapısı

Android tarafında hedef yapı:

```text
CameraX
├── Preview
└── ImageAnalysis
```

Mevcut component gerekiyorsa şu use-case'leri de içerebilir:

```text
CameraX
├── Preview
├── ImageCapture
├── VideoCapture
└── ImageAnalysis
```

Ancak plate scanning kullanımında yalnızca gerekli use-case'ler aktif edilmelidir.

Gereksiz `ImageCapture` veya `VideoCapture` binding'leri kaynak kullanımını artırıyorsa scanner modunda devre dışı bırakılmalıdır.

---

# 11. ImageAnalysis Configuration

Temel configuration:

```text
Backpressure Strategy:
KEEP_ONLY_LATEST
```

Amaç:

- Frame backlog oluşmaması.
- Eski frame'lerin işlenmemesi.
- Kullanıcı kamerayı hareket ettirdiğinde güncel görüntüye hızlı geçilmesi.

---

# 12. Frame Akışının Ana Prensibi

Camera 30 FPS çalışıyor olabilir.

Bu yaklaşık:

```text
Frame 1
Frame 2
Frame 3
Frame 4
...
```

şeklinde sürekli veri üretir.

Ancak detector'ın 30 FPS inference yapması zorunlu değildir.

Hedef:

```text
Preview:
30 FPS

Analysis:
cihazın kaldırabildiği hız
```

olmalıdır.

Örnek:

```text
Frame 1  → analyze
Frame 2  → drop
Frame 3  → drop
Frame 4  → analyze
Frame 5  → drop
...
```

Kullanıcı yine 30 FPS preview görür.

---

# 13. Tek Inference Kuralı

Aynı anda birden fazla detector inference çalıştırılmamalıdır.

Önerilen durum:

```text
isAnalyzing = false
      ↓
frame geldi
      ↓
isAnalyzing = true
      ↓
pipeline çalıştı
      ↓
frame release
      ↓
isAnalyzing = false
```

Yeni frame geldiğinde:

```text
if (isAnalyzing)
    frame.close()
```

benzeri davranış uygulanmalıdır.

CameraX `KEEP_ONLY_LATEST` zaten bu davranışın önemli kısmını sağlar ancak analyzer tarafında da double-processing engellenmelidir.

---

# 14. Frame Ownership Kuralı

En kritik kurallardan biri:

> `ImageProxy` yalnızca tek pipeline'ın kontrolünde olmalıdır.

Bir frame alındığında:

```text
ImageProxy received
    ↓
owner = analyzer
```

Pipeline tamamlandığında veya hata oluştuğunda:

```text
ImageProxy.close()
```

mutlaka çağrılmalıdır.

`finally` bloğu veya eşdeğer güvenli lifecycle yapısı kullanılmalıdır.

---

# 15. Frame Pipeline — Detaylı Akış

Bir `ImageProxy` geldiği anda tam akış:

```text
ImageProxy
    ↓
Frame metadata oku
    ↓
rotation / width / height bilgilerini kaydet
    ↓
Detector preprocessing
    ↓
NanoDet inference
    ↓
NanoDet postprocess
    ↓
plate detection var mı?
    ├── NO
    │    ↓
    │  frame.close()
    │
    └── YES
         ↓
     detector bbox
         ↓
     source coordinate mapping
         ↓
     quality gate
         ├── FAIL
         │    ↓
         │  frame.close()
         │
         └── PASS
              ↓
          source ROI
              ↓
          OCR tensor hazırla
              ↓
          frame.close()
              ↓
          OCR inference
              ↓
          OCR decode
              ↓
          result validator
              ↓
          final result?
              ├── NO → next frame
              └── YES → SUCCESS
```

Bu planın temel frame yaşam döngüsü budur.

---

# 16. Detector Preprocessing Planı

Source frame örneğin:

```text
1920 × 1080
```

olabilir.

NanoDet input:

```text
416 × 416
```

olacaktır.

Faz 3 preprocessor'ı şu çıktıyı üretir:

```text
DetectorInputTensor
```

Çalışan referans modelin preprocessing contract'ına uygun olarak CameraX `YUV_420_888` verisi,
ara JPEG veya Bitmap oluşturmadan rotation uygulanmış 416×416 BGR-CHW tensöre dönüştürülür.
Normalizasyon değerleri şöyledir:

```text
mean = [103.53, 116.28, 123.675]
std  = [57.375, 57.12, 58.395]
```

`FrameTransform` Faz 4'te eklendi ve aşağıdaki bilgileri taşıyor:

```text
sourceWidth
sourceHeight
rotatedWidth
rotatedHeight
inputWidth
inputHeight
scaleX
scaleY
paddingX
paddingY
rotation
mirror state
```

bilgileri tutulmalıdır.

Amaç inference sonucu gelen bbox'ı source frame'e doğru map etmektir.

---

# 17. Frame Transform Saklama

Detector'a verilen görüntüye uygulanan her geometrik dönüşüm kayıt altında olmalıdır.

Örnek:

```text
Source Frame
1920×1080
    ↓
rotation correction
    ↓
resize
    ↓
letterbox
    ↓
416×416
```

Bu işlem sırasında elde edilen scale/padding değerleri kaydedilir.

Sonrasında:

```text
BBox @ 416×416
    ↓
remove padding
    ↓
inverse scale
    ↓
inverse rotation
    ↓
BBox @ source frame
```

hesaplanır.

Bu mapping bağımsız bir sınıfta yapılmalıdır.

---

# 18. Bounding Box Mapping Testleri

Bu kısım ayrı unit test kapsamına alınmalıdır.

Test durumları:

```text
0° rotation
90° rotation
180° rotation
270° rotation
portrait
landscape
front camera mirror
letterbox horizontal padding
letterbox vertical padding
bbox edge cases
```

Plaka ROI yanlış map edilirse detector başarılı olsa bile OCR başarısız olur.

Bu nedenle bu component kritik kabul edilmelidir.

---

# 19. Quality Gate Planı

Her detection doğrudan OCR'a gönderilmemelidir.

Quality gate şu kriterleri değerlendirebilir:

```text
Detection confidence
BBox width
BBox height
BBox area
BBox frame sınırında mı?
Aspect ratio
Blur / sharpness
Plate ROI yeterince büyük mü?
```

İlk sürümde quality gate basit tutulmalıdır.

Önerilen minimum:

```text
confidence threshold
minimum bbox width
minimum bbox height
bbox frame bounds validation
```

Daha ileri blur/quality hesapları ancak benchmark sonucu ihtiyaç varsa eklenmelidir.

---

# 20. Source Frame ROI Kullanımı

Detection başarılı olduğunda plaka görüntüsü ayrı bir fiziksel image olarak oluşturulmamalıdır.

Yanlış:

```text
Frame
↓
Bitmap oluştur
↓
Crop Bitmap
↓
JPEG
↓
Dosyaya yaz
↓
Dosyayı oku
↓
OCR
```

Doğru:

```text
Source Frame
+
BBox
↓
ROI
↓
OCR Preprocessor
↓
OCR Tensor
```

Bu pipeline'da:

- Disk I/O yoktur.
- JPEG encode/decode yoktur.
- Intermediate image file yoktur.

---

# 21. OCR İçin Detector Input'u Kullanılmayacak

NanoDet'e verilen:

```text
416×416
```

küçültülmüş görüntünün ROI'si OCR'a verilmemelidir.

OCR:

```text
source camera frame
```

üzerinden çalışmalıdır.

Akış:

```text
NanoDet bbox @ 416
        ↓
source bbox @ original frame
        ↓
source ROI
        ↓
OCR
```

olmalıdır.

Bunun nedeni OCR'a mümkün olan en yüksek karakter detayını vermektir.

---

# 22. OCR Preprocessor Planı

OCR preprocessor'ın girdisi:

```text
Source Frame
+
Plate Bounding Box
```

olmalıdır.

Çıktı:

```text
OCR Input Tensor
```

olmalıdır.

İçeride:

```text
ROI read
↓
optional bbox padding
↓
resize
↓
normalize
↓
channel conversion
↓
tensor
```

işlemleri yapılır.

Kritik nokta:

> OCR preprocessor image döndürmemeli; mümkünse doğrudan model input buffer'ını doldurmalıdır.

---

# 23. Bounding Box Padding Stratejisi

OCR öncesi bbox küçük bir miktar genişletilebilir.

Amaç:

- İlk karakterin kesilmesini önlemek.
- Son karakterin kesilmesini önlemek.
- Detector'ın birkaç pixel dar tahminini tolere etmek.

Padding configurable olmalıdır.

Örnek config:

```text
horizontalPaddingRatio
verticalPaddingRatio
```

Değerler saha testinden sonra belirlenmelidir.

---

# 24. ImageProxy Release Noktası

Önerilen release noktası:

```text
ImageProxy
↓
Detector
↓
BBox
↓
Source ROI → OCR Tensor hazırlandı
↓
ImageProxy.close()
↓
OCR inference
```

Yani OCR modelinin inference süresi boyunca kamera frame buffer'ı tutulmamalıdır.

OCR inference artık bağımsız tensor üzerinde devam eder.

Bu sayede camera pipeline daha hızlı yeni frame alabilir.

---

# 25. OCR Inference Akışı

OCR tensor hazır olduktan sonra:

```text
OCR Tensor
↓
fast-plate-ocr ONNX session
↓
raw OCR output
↓
decode
↓
plate string
↓
OCR confidence
```

elde edilir.

Örnek:

```text
34ABC123
```

---

# 26. Result Validation Planı

İlk OCR sonucu doğrudan final kabul edilmemelidir.

Önerilen baseline, yalnızca confidence eşiğini geçen OCR sonuçlarını son üç geçerli okumadan oluşan kayan pencerede değerlendirmektir:

```text
requiredMatchingReads = 2
validationWindowSize = 3
ocrConfidenceThreshold = 0.90f
```

Akış:

```text
Geçerli okuma 1 → 34ABC123
Geçerli okuma 2 → 34ABC124
Geçerli okuma 3 → 34ABC123
                       ↓
             34ABC123 = 2/3
                       ↓
SUCCESS
```

Alternatif ileride:

```text
high OCR confidence → single read acceptance
medium confidence → 2 read validation
```

uygulanabilir.

İlk sürümde basit ve deterministik davranış tercih edilmelidir.

---

# 27. Validation State Yönetimi

Validator son birkaç sonucu küçük bir buffer'da tutabilir.

Örneğin:

```text
ReadHistory
├── 34ABC123
├── 34ABC123
└── ...
```

Kurallar:

- Maksimum birkaç sonuç tutulmalı.
- Uzun geçmiş tutulmamalı.
- Camera session değişince history temizlenmeli.
- Başarılı sonuç sonrası validator kilitlenmeli.

---

# 28. Success Lock

Final sonuç üretildikten sonra yeni frame'lerin inference'a girmesi engellenmelidir.

Örnek:

```text
state = ANALYZING
↓
valid plate found
↓
state = SUCCESS
↓
acceptNewFrames = false
```

Bu sayede aynı başarı için ikinci sonuç callback'i gelmez.

---

# 29. Başarılı Sonuç Sonrası Akış

```text
SUCCESS
↓
Yeni inference'ları kabul etmeme
↓
onResult(PlateRecognitionResult)
↓
Consumer application kararı
```

Kütüphane, doğrulanmış sonucu `onResult` ile iletir. Kamera use-case'lerini unbind etme,
analizi kapatma, ekran değiştirme ve sonucu kullanma kararı consumer application'a aittir.

---

# 30. Error Akışı

Pipeline herhangi bir noktada hata verirse:

```text
try
↓
pipeline
↓
catch
↓
error callback
↓
finally
↓
frame.close()
```

uygulanmalıdır.

Hata türleri:

```text
MODEL_NOT_FOUND
MODEL_INITIALIZATION_FAILED
INVALID_MODEL_INPUT
INVALID_MODEL_OUTPUT
FRAME_CONVERSION_FAILED
DETECTOR_INFERENCE_FAILED
OCR_INFERENCE_FAILED
CAMERA_PERMISSION_DENIED
CAMERA_BIND_FAILED
ANALYZER_DISPOSED
```

---

# 31. Camera Preview FPS Koruma Stratejisi

Preview tarafını korumak için:

- ML UI thread üzerinde çalıştırılmayacak.
- Frame backlog oluşturulmayacak.
- Tek inference aynı anda çalışacak.
- OCR yalnızca detection olduğunda çalışacak.
- OCR tensor hazırlandıktan sonra `ImageProxy` erken kapatılacak.
- Her frame için ağır allocation yapılmayacak.
- Camera preview ML sonucunu beklemeyecek.

Kabul kriteri:

```text
ML yavaşlarsa → analysis FPS düşebilir
Preview FPS → düşmemeli
```

---

# 32. Analysis Throttle

İlk benchmark'ta her uygun ImageAnalysis frame'i analiz edilebilir.

Eğer gereksiz CPU yükü oluşursa configurable throttle eklenebilir.

Örneğin:

```text
maxAnalysisFps = 10
```

Bu durumda:

```text
Preview 30 FPS
Analysis max 10 FPS
```

olabilir.

Throttle ilk günden zorunlu yapılmamalıdır.

Önce gerçek cihaz benchmark'ı alınmalıdır.

---

# 33. Thread / Executor Planı

Önerilen mantıksal ayrım:

```text
Main Thread
└── UI / Compose

Camera Executor
└── CameraX callbacks

Analysis Executor
└── Frame pipeline orchestration

ONNX Runtime
├── NanoDet inference
└── OCR inference
```

Inference sonucunu UI'a döndürürken main dispatcher kullanılmalıdır.

---

# 34. Aşırı Paralellikten Kaçınma

Şu yapı önerilmez:

```text
NanoDet frame 1
NanoDet frame 2
NanoDet frame 3
OCR frame 1
OCR frame 2
```

aynı anda.

Tercih edilen yapı:

```text
Frame
↓
NanoDet
↓
Quality Gate
↓
OCR gerekirse OCR
↓
Pipeline tamam
↓
Next latest frame
```

Amaç throughput değil düşük latency'dir.

---

# 35. Reusable Buffer Planı

İlk çalışan sürüm oluşturulduktan sonra şu buffer'lar reuse edilmelidir:

```text
Detector input buffer
OCR input buffer
RGB/YUV conversion working buffer
Detector output scratch buffer
```

Amaç:

- Garbage Collector baskısını azaltmak.
- Per-frame allocation azaltmak.
- p95/p99 latency spike azaltmak.

---

# 36. YUV / RGB Stratejisi

İlk sürümde basit ve güvenilir yol seçilmelidir.

Başlangıç:

```text
Camera frame
↓
required color conversion
↓
detector tensor
```

OCR için aynı source frame'in ROI'si kullanılır.

Daha ileri optimizasyon aşamasında:

```text
Full frame YUV
├── detector için küçük RGB/float conversion
└── detection sonrası yalnız ROI conversion
```

yaklaşımı değerlendirilebilir.

Bu optimizasyon baseline performansı yetersiz kalırsa yapılmalıdır.

---

# 37. State Machine

Analyzer için önerilen state machine:

```text
CREATED
   ↓
INITIALIZING
   ↓
READY
   ↓
ANALYZING
   ↓
READY
   ↓
ANALYZING
   ↓
SUCCESS
```

Diğer yollar:

```text
INITIALIZING → ERROR
ANALYZING → ERROR
READY → STOPPED
SUCCESS → STOPPED
STOPPED → DISPOSED
```

State değişimleri thread-safe olmalıdır.

---

# 38. Camera Session State

Camera tarafında ayrı state tutulabilir:

```text
IDLE
OPENING
ACTIVE
CLOSING
CLOSED
ERROR
```

Analyzer state ile camera state birbirine karıştırılmamalıdır.

---

# 39. Consumer API'de Hedef Basitlik

Consumer geliştiricinin bilmesi gerekenler minimum olmalıdır.

İdeal kullanım:

```kotlin
val analyzer = PlateRecognitionAnalyzer(
    detectorModel = detector,
    ocrModel = ocr
)

DevengCamera(
    analyzer = analyzer,
    onAnalysisResult = { plate ->
        // use plate
    }
)
```

Consumer geliştirici şu konularla uğraşmamalıdır:

```text
ImageProxy
YUV
NanoDet preprocess
BBox mapping
OCR tensor
ONNX session
Frame release
Backpressure
Threading
```

---

# 40. Configuration Nesnesi

Önerilen config:

```kotlin
data class PlateRecognitionConfig(
    val detectorInputWidth: Int = 416,
    val detectorInputHeight: Int = 416,
    val detectionConfidenceThreshold: Float,
    val nmsThreshold: Float,
    val requiredMatchingReads: Int = 2,
    val validationWindowSize: Int = 3,
    val ocrConfidenceThreshold: Float = 0.90f,
    val bboxPaddingHorizontal: Float,
    val bboxPaddingVertical: Float,
    val maxAnalysisFps: Int? = null
)
```

İlk sürümde config API'si küçük tutulmalıdır.

---

# 41. Logging ve Metrics

Debug build için şu süreler ölçülmelidir:

```text
frame_received_at
detector_preprocess_ms
detector_inference_ms
detector_postprocess_ms
bbox_mapping_ms
quality_gate_ms
ocr_preprocess_ms
ocr_inference_ms
validation_ms
total_pipeline_ms
```

Ek ölçümler:

```text
frames_received
frames_dropped
frames_analyzed
detections_found
quality_gate_pass
ocr_runs
successful_reads
```

---

# 42. Performance KPI'ları

İlk kabul kriterleri:

```text
Preview:
gözle görülür takılma olmamalı

Frame backlog:
0

Concurrent detector inference:
maksimum 1

Intermediate JPEG/file:
0

Model reload per frame:
0

Source-frame ROI OCR:
aktif
```

Latency hedefleri gerçek POS cihazı benchmark'ından sonra kesinleştirilmelidir.

---

# 43. Benchmark Senaryoları

## Senaryo A — Plaka yok

Kamera 30 saniye açık tutulur.

Ölç:

```text
Preview FPS
Detector inference rate
CPU
RAM
temperature
```

Beklenti:

- OCR hiç veya neredeyse hiç çalışmamalı.
- Preview takılmamalı.

---

## Senaryo B — İyi plaka açısı

Kamera doğrudan okunabilir plakaya tutulur.

Ölç:

```text
camera open → first detection
camera open → first OCR
camera open → validated result
```

---

## Senaryo C — Kötü açıdan iyi açıya geçiş

Kullanıcı kamerayı hareket ettirir.

Beklenti:

- Eski frame backlog'u olmamalı.
- İyi açı oluşunca sistem güncel frame üzerinde hızlı tepki vermeli.

---

## Senaryo D — Uzun kullanım

Arka arkaya yüzlerce scanning session çalıştırılır.

Ölç:

```text
RAM trend
latency trend
temperature
preview FPS
session initialization time
```

---

# 44. Memory Leak Testi

Her camera session sonrası kontrol:

```text
ImageProxy açık kaldı mı?
ONNX tensor dispose edildi mi?
OrtSession result dispose edildi mi?
Camera executor kapanması gerekiyor mu?
Analyzer history temizlendi mi?
```

Uzun testte RAM sürekli artmamalıdır.

---

# 45. Unit Test Planı

Unit test önceliği:

1. Frame coordinate mapping.
2. Letterbox inverse mapping.
3. Rotation mapping.
4. Bounding box padding.
5. Quality gate.
6. OCR result normalization.
7. Multi-frame result validation.
8. Analyzer state transitions.

---

# 46. Integration Test Planı

Sabit bir camera frame benzeri giriş üzerinden:

```text
source frame
↓
NanoDet
↓
expected bbox
↓
OCR ROI
↓
expected plate
```

uçtan uca test oluşturulmalıdır.

Modeller deterministic çalışıyorsa regression test olarak kullanılabilir.

---

# 47. Android Instrumentation Test Planı

Gerçek CameraX lifecycle için:

```text
camera open
camera close
screen rotate
app background
app foreground
permission denied
permission granted
rapid open/close
successful result callback
```

senaryoları test edilmelidir.

---

# 48. Uygulama Geliştirme Fazları

## Faz 1 — Camera Analysis Infrastructure

Amaç: Modelsiz olarak frame analiz altyapısını kurmak.

Yapılacaklar:

- `CameraFrameAnalyzer` contract.
- Camera API'ye optional analyzer.
- Android `ImageAnalysis`.
- `KEEP_ONLY_LATEST`.
- Frame lifecycle.
- Dummy analyzer ile test.

Çıktı:

```text
Camera frame → DummyAnalyzer → Result
```

çalışmalı.

---

## Faz 2 — ONNX Runtime Infrastructure

Amaç: Model session yönetimini kurmak.

Yapılacaklar:

- Private storage model dosyası için `File` girdisi.
- ONNX Runtime dependency/module.
- Session lifecycle.
- Genel tensor input/output contract validation.
- Error handling.

NanoDet ve OCR'a özgü input/output adları, tipleri ve shape doğrulamaları sırasıyla Faz 3 ve
Faz 5'te gerçek model contract'larıyla eklenmelidir.

Çıktı:

```text
Private storage .onnx File → OrtSession
```

çalışmalı.

---

## Faz 3 — NanoDet Integration

Durum: tamamlandı.

Eklenenler:

- CameraX `YUV_420_888` → rotation uygulanmış, bilinear resize edilmiş, normalize BGR-CHW tensor.
- Dosya tabanlı ONNX session kullanan NanoDet inference wrapper.
- Model açılışında tam input/output contract doğrulaması.
- `[1, 3598, 33]` output parser, `reg_max=7` DFL decode ve stride `8/16/32/64` center prior üretimi.
- Confidence `0.50` ve IoU `0.60` varsayılanlarıyla NMS.
- 416×416 detector koordinat sisteminde bbox ve confidence taşıyan result DTO.
- Config, YUV stride/rotation, DFL, prior, bbox ve NMS birim testleri.

Doğrulanan model contract'ı:

```text
input  : data   / FLOAT / [1, 3, 416, 416]
output : output / FLOAT / [1, 3598, 33]
ONNX   : IR 10 / opset 18
```

Çıktı:

```text
Frame → Plate Bounding Box
```

çalışmalı.

Source frame coordinate mapping ve preview mirror düzeltmesi Faz 4 kapsamındaydı ve tamamlandı.
Faz 3 raw sonucu detector koordinat sisteminde tutulmaya devam eder; normal analyzer sonucu
source-frame koordinatlarını taşır.

---

## Faz 4 — Coordinate Mapping (Tamamlandı)

Yapılanlar:

- `FrameTransform` ile kaynak frame, rotation, detector input, ölçek, padding ve mirror bilgisi
  preprocessing sonucuna eklendi.
- Bağımsız `FrameCoordinateMapper` ile detector bbox source frame bbox'a dönüştürülüyor.
- `0°`, `90°`, `180°` ve `270°` rotation dönüşümleri uygulanıyor.
- Padding/letterbox ters dönüşümü ve koordinat sınırlandırması uygulanıyor.
- `NanoDetDetector.analyze()` source-frame koordinatlı `SourcePlateDetection` döndürüyor.
- Validation ekranındaki mevcut detector-space debug çıktısı korunuyor.
- Portrait/landscape, mirror, padding, ters koordinat ve edge-case unit testleri eklendi.

Mevcut preprocessing yolları doğrudan resize kullandığı için Faz 4 implementasyonunda padding değeri
şimdilik sıfırdır; mapper padding içeren transform'ları da destekler.

Çıktı:

```text
Detector BBox
↓
Correct Source BBox
```

garanti altına alınmalı.

---

## Faz 5 — OCR Direct ROI Pipeline

Yapılacaklar:

- Source frame ROI reader.
- Optional bbox padding.
- OCR preprocessor.
- Direct OCR tensor generation.
- `ImageProxy` early release.
- FastPlateOCR wrapper.

Çıktı:

```text
Source Frame + BBox
↓
OCR Tensor
↓
Plate Text
```

çalışmalı.

---

## Faz 6 — End-to-End Analyzer

Yapılacaklar:

```text
NanoDet
↓
BBox
↓
Mapping
↓
Quality Gate
↓
OCR
↓
Validation
```

tek `PlateRecognitionAnalyzer` altında birleştirilmeli.

---

## Faz 8 — Performance Baseline

Henüz optimizasyon yapmadan ölç:

```text
Preview FPS
Detector latency
OCR latency
Total latency
CPU
RAM
temperature
```

Bu baseline kaydedilmelidir.

---

## Faz 9 — Memory / Allocation Optimization

Gerekirse:

- reusable buffers,
- less color conversion,
- direct buffers,
- temporary object azaltma,
- history reuse

uygulanır.

---

## Faz 10 — Runtime Optimization

Gerekirse:

```text
XNNPACK
thread tuning
analysis throttle
FP16
INT8
```

test edilir.

Her değişiklik A/B benchmark ile doğrulanmalıdır.

---

# 49. Önerilen Geliştirme Sırası

Pratik uygulama sırası:

1. Mevcut camera component'i bozmadan `ImageAnalysis` ekle.
2. Dummy analyzer ile canlı frame akışını doğrula.
3. `KEEP_ONLY_LATEST` ve frame release'i doğrula.
4. `CameraFrameAnalyzer` public abstraction'ını sabitle.
5. Private storage model dosyası doğrulamasını ekle.
6. ONNX Runtime session lifecycle'ını kur.
7. NanoDet'i tek image/frame üzerinde çalıştır.
8. NanoDet'i canlı camera frame'ine bağla.
9. BBox mapping'i doğrula.
10. Source ROI'den OCR tensor üret.
11. fast-plate-ocr'ı bağla.
12. Result validation ekle.
13. Success sonrası analyzer/camera kapatma akışını ekle.
14. Benchmark instrumentation ekle.
15. Gerçek cihaz baseline al.
16. Darboğazları sırayla optimize et.
17. Uzun süreli saha/stress testi yap.
18. API ve documentation finalizasyonu yap.

---

# 50. PR'lara Bölme Önerisi

Bu çalışma tek büyük PR yerine bölünmelidir.

## PR 1 — Generic Camera Frame Analysis

```text
CameraFrameAnalyzer
ImageAnalysis
KEEP_ONLY_LATEST
Frame lifecycle
Dummy analyzer test
```

NanoDet veya OCR içermez.

---

## PR 2 — ONNX Analysis Infrastructure

```text
File-based model loading
ONNX lifecycle
Generic tensor input/output validation
Error model
```

---

## PR 3 — Plate Recognition Analyzer

```text
NanoDet
BBox mapping
Quality gate
OCR
Validation
```

Model dosyaları repository'ye eklenmez.

---

## PR 4 — Performance / Benchmark

```text
Metrics
Buffer reuse
Throttle
Thread tuning
```

---

# 51. Definition of Done

Çalışma tamamlanmış sayılmadan önce:

- [ ] Mevcut camera API geriye dönük uyumlu.
- [ ] Analyzer verilmezse mevcut kamera davranışı değişmiyor.
- [ ] `ImageAnalysis` Android tarafında çalışıyor.
- [ ] `KEEP_ONLY_LATEST` aktif.
- [ ] Aynı anda maksimum bir inference var.
- [ ] Tüm `ImageProxy` nesneleri güvenli şekilde release ediliyor.
- [ ] Model dosyaları public repo'da yok.
- [ ] Consumer modelleri dışarıdan verebiliyor.
- [ ] NanoDet canlı frame üzerinde plaka bbox üretiyor.
- [ ] BBox source frame'e doğru map ediliyor.
- [ ] OCR detector'ın küçültülmüş inputundan değil source frame ROI'sinden çalışıyor.
- [ ] JPEG/disk round-trip yok.
- [ ] OCR tensor hazır olduğunda source frame erken release ediliyor.
- [ ] OCR sonucu doğrulanıyor.
- [ ] Başarılı sonuçta yeni frame kabul edilmiyor.
- [ ] Başarılı sonuç `onResult` ile consumer application'a iletiliyor.
- [ ] Preview'da gözle görülür FPS kaybı yok.
- [ ] Uzun kullanım testinde memory leak yok.
- [ ] Gerçek POS/Android cihazında benchmark tamamlandı.

---

# 52. Nihai Frame Yaşam Döngüsü

Projenin en kritik teknik akışı özetle:

```text
CameraX ImageAnalysis
        ↓
     ImageProxy
        ↓
 Detector Preprocess
        ↓
 NanoDet Inference
        ↓
 Detector Postprocess
        ↓
     Plate BBox
        ↓
 Source Coordinate Mapping
        ↓
    Quality Gate
        ↓
 Source Frame ROI
        ↓
 OCR Tensor Creation
        ↓
  ImageProxy.close()
        ↓
 fast-plate-ocr
        ↓
Result Validation
        ↓
      SUCCESS
        ↓
 Yeni inference'ları kabul etmeme
        ↓
onResult(PlateRecognitionResult)
        ↓
Consumer Application Decision
```

Bu akışın temel performans prensipleri:

```text
NO disk I/O
NO JPEG round-trip
NO model reload
NO frame backlog
NO concurrent detector inference
NO OCR on every frame
NO OCR from 416x416 detector input
```

---

# 53. Sonuç

Modeller hazır olduğu için bu projenin ana mühendislik problemi artık model eğitimi değil:

```text
Camera frame lifecycle
+
efficient preprocessing
+
correct coordinate mapping
+
zero-intermediate-image OCR
+
threading
+
memory management
+
user experience
```

konularıdır.

En doğru implementasyon sırası:

```text
Generic Camera Analysis
        ↓
Live Frame Flow
        ↓
NanoDet
        ↓
BBox Mapping
        ↓
Source ROI
        ↓
OCR
        ↓
Validation
        ↓
onResult
        ↓
Benchmark
        ↓
Optimization
```

olmalıdır.

Özellikle ilk hedef maksimum optimizasyon değil, doğru çalışan ve ölçülebilen bir baseline üretmek olmalıdır. Bundan sonra hangi aşamanın gerçek darboğaz olduğu ölçülerek yalnızca gerekli optimizasyonlar uygulanmalıdır.
