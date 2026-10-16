# Plate Recognition - Production Temizlik ve Sadeleştirme Kontrol Listesi (Production Cleanup Checklist)

Bu doküman, geliştirme ve test (Phase 0 / Benchmarking) aşamasında analiz, hata ayıklama ve süre takibi amacıyla eklenen; ancak **canlıya (Production) çıkış aşamasında** kod tabanını sadeleştirmek ve log kirliliğini önlemek amacıyla kaldırılacak bileşenlerin listesini içerir.

---

## 1. Kaldırılacak Telemetri ve Loglama Bileşenleri

### A. `PlateRuntimePhase0` Logları (`PHASE0_LOG_TAG`)
Geliştirme aşamasında her karenin süresini, oturum açılışlarını ve ilk çıkarım maliyetini Logcat'e basan yapılar:

- [ ] **[FastPlateOcr.kt](file:///c:/Users/enesb/StudioProjects/deveng-core-kmp/deveng-core/src/androidMain/kotlin/core/domain/camera/analysis/ocr/FastPlateOcr.kt)**:
  - `openPreferredSessionWithPhase0Timing` wrapper fonksiyonu kaldırılacak.
  - `firstInferenceLogged` ve `PHASE0_LOG_TAG` logları temizlenecek.
- [x] **[PlateRecognitionEngines.kt](file:///c:/Users/enesb/StudioProjects/deveng-core-kmp/deveng-core/src/androidMain/kotlin/core/domain/camera/analysis/plate/engine/PlateRecognitionEngines.kt)**:
  - Model hazırlık ve açılışındaki `Log.i` (`PHASE0_LOG_TAG`) ve tüm `System.nanoTime()` süre sayaçları temizlendi.
- [ ] **[PlateRecognitionValidationScreen.android.kt](file:///c:/Users/enesb/StudioProjects/deveng-core-kmp/sample/composeApp/src/androidMain/kotlin/global/deveng/core/PlateRecognitionValidationScreen.android.kt)**:
  - `PHASE0_LOG_TAG` ile kare kare Logcat'e yazılan frame debug logları temizlenecek.
- [ ] **[App.android.kt](file:///c:/Users/enesb/StudioProjects/deveng-core-kmp/sample/composeApp/src/androidMain/kotlin/global/deveng/core/App.android.kt)**:
  - Model kopyalama ve hazırlık sırasındaki `PlateRuntimePhase0` logları temizlenecek.

### B. Bitmap Render Süre Ölçümü (Benchmarking / Telemetri)
Geliştirme ve doğrulama ekranında görüntü oluşturma maliyetini takip etmek için eklenen süre ölçümleri:

- [ ] **[CapturedCameraFrame.kt](file:///c:/Users/enesb/StudioProjects/deveng-core-kmp/deveng-core/src/androidMain/kotlin/core/domain/camera/analysis/ocr/CapturedCameraFrame.kt)**:
  - `bitmapRenderNanos` alanı ve `lazyBitmap` içerisindeki `startedNanos` / `System.nanoTime()` süre ölçümü kaldırılacak (`lazy(LazyThreadSafetyMode.SYNCHRONIZED) { renderBitmap() }` olarak sadeleştirilecek).
- [ ] **[PlateRecognitionValidationScreen.android.kt](file:///c:/Users/enesb/StudioProjects/deveng-core-kmp/sample/composeApp/src/androidMain/kotlin/global/deveng/core/PlateRecognitionValidationScreen.android.kt)**:
  - UI üzerindeki `MetricText("Bitmap render time", ...)` metrik satırı temizlenecek.
- [ ] **[CapturedCameraFrameTest.kt](file:///c:/Users/enesb/StudioProjects/deveng-core-kmp/deveng-core/src/androidUnitTest/kotlin/core/domain/camera/analysis/ocr/CapturedCameraFrameTest.kt)**:
  - Testlerdeki `bitmapRenderNanos` kontrolleri güncellenecek/temizlenecek.

### C. OCR Aşama Süre Ölçümleri (Benchmarking / Telemetri)
Geliştirme ve doğrulama aşamasında OCR hattının alt adımlarının sürelerini takip etmek için eklenen telemetri alanları:

- [ ] **[FastPlateOcrResult.kt](file:///c:/Users/enesb/StudioProjects/deveng-core-kmp/deveng-core/src/androidMain/kotlin/core/domain/camera/analysis/ocr/FastPlateOcrResult.kt)**:
  - `frameCopyNanos`, `preprocessNanos` ve `inferenceNanos` alanları veri modelinden kaldırılacak.
- [ ] **[FastPlateOcr.kt](file:///c:/Users/enesb/StudioProjects/deveng-core-kmp/deveng-core/src/androidMain/kotlin/core/domain/camera/analysis/ocr/FastPlateOcr.kt)**:
  - `recognize()` fonksiyonu içindeki `frameCopyStartedNanos`, `preprocessStartedNanos`, `inferenceStartedNanos` ve `System.nanoTime()` hesaplama değişkenleri temizlenecek.
- [ ] **[PlateRecognitionAnalyzer.kt](file:///c:/Users/enesb/StudioProjects/deveng-core-kmp/deveng-core/src/androidMain/kotlin/core/domain/camera/analysis/plate/PlateRecognitionAnalyzer.kt)**:
  - Snapshot ve debug veri modellerindeki `frameCopyNanos`, `preprocessNanos`, `inferenceNanos` alanları temizlenecek.
- [ ] **[PlateRecognitionValidationScreen.android.kt](file:///c:/Users/enesb/StudioProjects/deveng-core-kmp/sample/composeApp/src/androidMain/kotlin/global/deveng/core/PlateRecognitionValidationScreen.android.kt)**:
  - UI üzerindeki `Accumulated frame copy time`, `Accumulated OCR preprocess time`, `Accumulated OCR time` metrik satırları temizlenecek.

### D. `PlateRecognitionDebugSnapshot` ve Debug Callback Mekanizması
Doğrulama ve geliştirme ekranında kutu çizimi ve anlık model sürelerini göstermek amacıyla Analyzer'a eklenen debug altyapısı:

- [ ] **[PlateRecognitionAnalyzer.kt](file:///c:/Users/enesb/StudioProjects/deveng-core-kmp/deveng-core/src/androidMain/kotlin/core/domain/camera/analysis/plate/PlateRecognitionAnalyzer.kt)**:
  - Yapıcılardaki (constructors) `onDebugSnapshot: ((PlateRecognitionDebugSnapshot) -> Unit)? = null` parametreleri kaldırılacak.
  - `private val debugLock = Any()` senkronizasyon nesnesi tamamen silinecek.
  - `publishDebugSnapshot(...)` fonksiyonu temizlenecek.
  - `analyze(...)`, `resolveCandidate(...)` ve `runOcr(...)` içindeki tüm `System.nanoTime()` süre ölçümleri (`processStartNanos`, `detectorStartNanos`, `detectorNanos`, `ocrStartNanos`, `ocrTotalNanos`) tamamen kaldırılacak.
  - Sadece süre taşımak için oluşturulmuş olan uydurma `FrameCandidate` private veri modeli silinecek; `candidate` ismi yerine doğrudan `detectedPlate: PlateDetectionResult?` kullanılacak.
  - `resolveCandidate(...)` fonksiyonunun adı belirsiz olduğu için (hangi adayın çözümlendiği açık değil); doğrudan işlevi ifade eden `detectAcceptablePlate(frame, session): PlateDetectionResult?` veya `detectPlateBox(...)` olarak yeniden adlandırılacak ve sadeleştirilecek.
  - `runOcr(...)` fonksiyonundaki gereksiz `try-catch` bloğu kaldırılacak; fonksiyon bağlaç içermeyen tekil bir sorumluluk ismiyle (`recognizePlate(...)`) adlandırılacak.
  - [x] `acceptOcrResult(...)` fonksiyonu `validateConsensus(session, ocrResult)` olarak yeniden adlandırıldı ve doğrudan `PlateRecognitionResult?` dönecek şekilde sadeleştirildi; `ConsensusValidation` ara sınıfı silindi.
  - Dosya sonundaki debug veri modelleri (`PlateRecognitionDebugSnapshot`, `PlateRecognitionDebugOcr`, `PlateRecognitionDebugDetection`, `PlateRecognitionDebugOcrStatus`) kaldırılacak (~150 satır sadeleşme).
- [ ] **[PlateRecognitionRegistration.android.kt](file:///c:/Users/enesb/StudioProjects/deveng-core-kmp/deveng-core/src/androidMain/kotlin/core/domain/camera/controller/PlateRecognitionRegistration.android.kt)**:
  - `registerPlateRecognition` metodlarındaki `onDebugSnapshot` parametreleri kaldırılacak.
- [ ] **[PlateRecognitionAnalyzerTest.kt](file:///c:/Users/enesb/StudioProjects/deveng-core-kmp/deveng-core/src/androidUnitTest/kotlin/core/domain/camera/analysis/plate/PlateRecognitionAnalyzerTest.kt)**:
  - Debug snapshot ve durumlarına bağımlı olan test assertion'ları güncellenecek/temizlenecek.
- [ ] **[Sample Uygulama (ComposeApp)](file:///c:/Users/enesb/StudioProjects/deveng-core-kmp/sample/composeApp/src/androidMain/kotlin/global/deveng/core/)**:
  - `PlateRecognitionValidationScreen.android.kt` ve `PlateRecognitionPhase6Metrics.kt` içerisindeki `onDebugSnapshot` dinleyicileri ve snapshot kullanım bağımlılıkları temizlenecek.

---

## 2. Model Profilleme ve Doğrulama Sadeleştirmeleri

Model eğitimi ve mimarisi bizzat kontrol altında olduğu için aşırı korumacı/savunmacı doğrulama katmanları kaldırılabilir:

- [ ] **[NanoDetModelProfile.kt](file:///c:/Users/enesb/StudioProjects/deveng-core-kmp/deveng-core/src/androidMain/kotlin/core/domain/camera/analysis/nanodet/NanoDetModelProfile.kt)**:
  - `calculatePredictionCount` ve `calculateGridDimension` fonksiyonları silinecek.
  - `resolveImageInputShape` ve `resolveOutputShape` gibi 10+ katmanlı defensive shape doğrulamaları kaldırılarak doğrudan `metadata.inputs.first()` ve `outputs.first()` boyutları okunacak.
  - (Opsiyonel) `NanoDetModelProfile` dosyası tamamen silinip `NanoDetDetector` `init` bloğuna 3 satırda bağlanabilir.
- [ ] **[FastPlateOcrModelProfile.kt](file:///c:/Users/enesb/StudioProjects/deveng-core-kmp/deveng-core/src/androidMain/kotlin/core/domain/camera/analysis/ocr/FastPlateOcrModelProfile.kt)**:
  - `resolveImageInputShape` (NHWC layout/rank kontrolleri) ve `resolveOutputShape` (slot/sınıf eşleme kontrolleri) gibi defensive model sözleşmesi katmanları kaldırılacak.
  - `FastPlateOcr.kt` `init` bloğunda `ResolvedModelContract` ara modeli yerine, doğrudan `modelSession.metadata.inputs.first()` ve `outputs.first()` üzerinden tensör ve tamponlar oluşturulacak.
  - (Opsiyonel) `FastPlateOcrModelProfile.kt` dosyası tamamen silinip `FastPlateOcr` içerisine inline edilebilir.
- [ ] **[OnnxModelMetadata.kt](file:///c:/Users/enesb/StudioProjects/deveng-core-kmp/deveng-core/src/androidMain/kotlin/core/domain/camera/analysis/onnx/OnnxModelMetadata.kt)** & **[OnnxModelContract.kt](file:///c:/Users/enesb/StudioProjects/deveng-core-kmp/deveng-core/src/androidMain/kotlin/core/domain/camera/analysis/onnx/OnnxModelContract.kt)**:
  - `profileName: String` parametreleri kaldırılacak (`requireSingleInput()`, `requireSingleOutput()`).
  - `ResolvedModelContract` ve `ResolvedTensorContract` ara veri modelleri sadeleştirilecek.

---

## 3. İç Fonksiyon, Postprocessor ve Wrapper Sadeleştirmeleri

- [ ] **[NanoDetPostprocessor.kt](file:///c:/Users/enesb/StudioProjects/deveng-core-kmp/deveng-core/src/androidMain/kotlin/core/domain/camera/analysis/nanodet/NanoDetPostprocessor.kt)**:
  - Yalnızca birim testler için yazılmış olan `fun decode(predictions: Array<FloatArray>)` fonksiyonu ve içerisindeki `invalidOutput` boyut/kanal kontrolleri (`predictions.size != predictionCount`, `channel != 33`) kaldırılacak.
  - Testler için yazılan kopyalanmış `internal fun distributionToDistance(prediction: FloatArray, offset: Int)` fonksiyonu kaldırılacak.
  - `decodeValues` fonksiyonu doğrudan üretimde kullanılan tek metod olan `fun decode(predictions: FloatBuffer)` içerisine inline edilecek.
  - `invalidOutput` yardımcı fonksiyonu ve `OnnxModelErrorCode.INVALID_MODEL_OUTPUT` bağımlılığı temizlenecek.
- [x] **[FastPlateOcr.kt](file:///c:/Users/enesb/StudioProjects/deveng-core-kmp/deveng-core/src/androidMain/kotlin/core/domain/camera/analysis/ocr/FastPlateOcr.kt)**:
  - `runWarmUpInference` ve `runInference` içindeki gereksiz `try-catch` blokları `NanoDetDetector`'da olduğu gibi yalın hale getirildi ve kullanılmayan exception import'ları temizlendi.
- [x] **[OcrDecoder.kt](file:///c:/Users/enesb/StudioProjects/deveng-core-kmp/deveng-core/src/androidMain/kotlin/core/domain/camera/analysis/ocr/OcrDecoder.kt)**:
  - Yalnızca birim testler için yazılmış olan `decode(FloatArray)` overload'ı ve `invalidOutput` yardımcı metodu kaldırıldı. `OcrDecoderTest`, `FloatBuffer.wrap` ile doğrudan prod fonksiyonunu test edecek şekilde sadeleştirildi.
- [x] **[OcrPreprocessor.kt](file:///c:/Users/enesb/StudioProjects/deveng-core-kmp/deveng-core/src/androidMain/kotlin/core/domain/camera/analysis/ocr/OcrPreprocessor.kt)**:
  - Yanıltıcı varsayılan boyutlar (`= 128`, `= 64`) kaldırılarak `inputWidth` ve `inputHeight` zorunlu parametre yapıldı. Yalnızca testler için yazılmış olan `preprocessRgbPixels(...): FloatArray` heap allocation metodu temizlendi.
- [ ] **[PlateRecognitionAnalyzer.kt](file:///c:/Users/enesb/StudioProjects/deveng-core-kmp/deveng-core/src/androidMain/kotlin/core/domain/camera/analysis/plate/PlateRecognitionAnalyzer.kt)**:
  - Eşzamanlılık (concurrency) ve durum değişkenleri sadeleştirilecek:
    - `stateLock = Any()` -> `sessionLock = Any()` olarak netleştirilecek.
    - `sessionGeneration: AtomicLong` -> `activeSessionId: Long` düz değişkene dönüştürülecek (zaten `sessionLock` altında korunuyor).
    - `successful: AtomicBoolean` -> `isPlateRecognized: Boolean` düz değişkene dönüştürülecek.
    - `acceptingFrames: AtomicBoolean` -> `isAcceptingFrames: Boolean` düz değişkene dönüştürülecek.
    - `closed: AtomicBoolean` -> `isClosed: Boolean` veya atomik kapanış yapısına uyarlanacak.
    - `ocrInFlight: AtomicBoolean` -> `isOcrBusy: AtomicBoolean` olarak daha anlaşılır adlandırılacak.

---

## 4. Prod Öncesi Doğrulama Adımları

Temizlik işlemleri tamamlandığında çalıştırılacak doğrulama komutları:

```bash
# Core kütüphane derleme ve birim testleri
./gradlew.bat :deveng-core:compileDebugKotlinAndroid :deveng-core:testDebugUnitTest

# Compose örnek uygulama derlemesi
./gradlew.bat :sample:composeApp:compileDebugKotlinAndroid

# Tüm platform testleri
./gradlew.bat :deveng-core:allTests
```
