# Plaka Tanıma Model Runtime ve Warm-up Geliştirme Planı

- Tarih: 21 Ağustos 2026
- Durum: Planlandı
- Kapsam: Android `androidMain`

## Amaç

Plaka tespit ve OCR modellerinin hazırlanmasını kamera bileşeninin yaşam döngüsünden ayırmak; model session'larını uygulama kapsamında yaşatmak ve kullanıcı kamera ekranına gelmeden önce yükleme ile warm-up işlemlerini arka planda tamamlayabilmek.

Başarı ölçütleri:

- Model yükleme ve warm-up işlemleri kamera bileşeninden bağımsız başlatılabilmelidir.
- Tüketici uygulama model dosyalarını uygulama kapsamındaki bir model yöneticisine verebilmelidir.
- `prepareModels` çağrısı kullanıcı arayüzünü veya çağrıldığı fonksiyon zincirini bloke etmemelidir.
- Ekranlar arası geçiş model session'larını kapatmamalıdır.
- Kullanıcının gördüğü ilk gerçek detection mümkün olduğunca ısınmış session ile çalışmalıdır.
- Model değiştirme, düşük bellek durumu ve uygulama kapsamının kapanması için açık kaynak bırakma davranışları bulunmalıdır.
- Mevcut dosya tabanlı kamera API'si geriye dönük uyumluluğunu korumalıdır.

## Bloke Eden Sorular

Yok.

## Varsayımlar

1. İlk uygulama yalnızca Android `androidMain` hedefini kapsayacaktır.
2. Bir model yöneticisi aynı anda en fazla bir aktif kamera analiz kaydına session sağlayacaktır.
3. Aktif kamera kullanımı sırasında model değiştirme isteği reddedilecek; örtük olarak çalışan session değiştirilmeyecektir.
4. Warm-up için detector ve OCR başına birer sahte çıkarım yeterli kabul edilecek; 100 tekrar yapılmayacaktır.
5. Gerçek ONNX dosyaları repoya eklenmeyecek; birim testlerinde sahte yükleyiciler, cihaz doğrulamasında tüketici uygulamanın verdiği modeller kullanılacaktır.

## Hedef Mimari

```text
Herhangi bir ViewModel
        │
        └── prepareModels(modelPaths) ──► Model Manager
                                           │
                                           ├── Detector session
                                           ├── OCR session
                                           └── Warm-up durumu

Kamera ekranı
        │
        └── registerPlateRecognition(manager)
                    │
                    └── Hazır session'ları geçici olarak kullanır

Kamera ekranından çıkış
        └── Yalnızca kamera analizi kapanır

Uygulama kapsamının kapanması
        └── Manager ve model session'ları kapanır
```

Bu yapıda ekranlar yalnızca model yöneticisini kullanır. Session sahipliği ekranda veya kamera bileşeninde değil, uygulama kapsamındaki yöneticidedir.

## Önerilen Genel API

```kotlin
data class PlateRecognitionModelSpec(
    val detectorModelFile: File,
    val ocrModelFile: File,
    val detectionConfidenceThreshold: Float,
    val nmsIouThreshold: Float,
    val performanceProfile: PlateRecognitionPerformanceProfile,
)

sealed interface PlateRecognitionPreparationState {
    data object Idle : PlateRecognitionPreparationState
    data object Loading : PlateRecognitionPreparationState
    data object Warming : PlateRecognitionPreparationState

    data class Ready(
        val metrics: PlateRecognitionPreparationMetrics,
    ) : PlateRecognitionPreparationState

    data class Failed(
        val error: Throwable,
    ) : PlateRecognitionPreparationState

    data object Closed : PlateRecognitionPreparationState
}

class PlateRecognitionModelManager : AutoCloseable {
    val state: StateFlow<PlateRecognitionPreparationState>

    fun prepareModels(spec: PlateRecognitionModelSpec)

    suspend fun awaitReady(): PlateRecognitionPreparationMetrics

    override fun close()
}
```

Kamera tarafına hazırlanmış model yöneticisini alan yeni bir overload eklenir:

```kotlin
fun CameraController.registerPlateRecognition(
    preparedModels: PlateRecognitionModelManager,
    config: PlateRecognitionConfig = PlateRecognitionConfig(),
    onPlateRecognized: (PlateRecognitionResult) -> Unit,
    onError: (Throwable) -> Unit = {},
): PlateRecognitionHandle
```

`prepareModels` işi başlatıp hemen döner. Hazırlığın tamamlanmasını gerçekten beklemesi gereken kod `state` akışını gözlemler veya `awaitReady()` çağırır. Böylece bloklama davranışı fonksiyonu kullanan her ViewModel'in doğru coroutine kurmasını gerektiren dağınık bir sorumluluk hâline gelmez.

## Faz 0 — Mevcut Soğuk Başlangıç Ölçümü

### Amaç

Yaklaşık yedi saniyelik ilk açılış süresinin model session oluşturma, warm-up, kamera açılışı ve ilk frame bekleme parçalarını birbirinden ayırmak.

### Yapılacaklar

- Detector session açılış süresi ölçülecek.
- OCR session açılış süresi ölçülecek.
- Detector ve OCR ilk çıkarım süreleri ayrı kaydedilecek.
- İlk detection ile kararlı durumdaki detection süreleri karşılaştırılacak.
- Kamera ekranından çıkıp yeniden girildiğinde session oluşturma sayısı kaydedilecek.
- Model hazırlama süresi ile kamera hazırlama süresi ayrı raporlanacak.

### Kabul Kriterleri

- En az beş ayrı soğuk uygulama süreci ölçülmüş olmalıdır.
- İlk uygulama açılışı ile yalnızca navigation sonrası kamera açılışı ayrı raporlanmalıdır.
- Model hazırlığı ve kamera hazırlığı tek bir toplam süre altında gizlenmemelidir.

## Faz 1 — Asenkron Model Yöneticisi ve Durum Makinesi

### Dosyalar

- Yeni: `deveng-core/src/androidMain/kotlin/core/domain/camera/analysis/plate/PlateRecognitionModelManager.kt`
- Yeni: `deveng-core/src/androidMain/kotlin/core/domain/camera/analysis/plate/PlateRecognitionPreparationState.kt`
- Yeni: ilgili `androidUnitTest` test dosyaları

### Yapılacaklar

- Uygulama kapsamında yaşayabilen `PlateRecognitionModelManager` oluşturulacak.
- Yönetici kendi arka plan çalışma kapsamına sahip olacak.
- Model yükleme düşük öncelikli, tek iş parçacıklı bir worker üzerinde yürütülecek.
- Detector ve OCR session'ları ilk aşamada ardışık hazırlanacak; düşük bellekli cihazlarda tepe bellek tüketimi kontrolsüz artırılmayacak.
- Aynı model tanımıyla gelen eşzamanlı hazırlama istekleri tek işe birleştirilecek.
- `Idle`, `Loading`, `Warming`, `Ready`, `Failed` ve `Closed` durumları dışarı açılacak.
- Başarısız hazırlama sonrasında aynı veya yeni model tanımıyla kontrollü tekrar deneme mümkün olacak.
- `close()` tekrarlı çağrılabilir ve güvenli olacak.

### Kabul Kriterleri

- Kapılı bir sahte yükleyiciyle `prepareModels` çağrısının yükleme tamamlanmadan geri döndüğü kanıtlanmalıdır.
- Aynı anda yapılan on hazırlama çağrısı yalnızca bir yükleme işi üretmelidir.
- Çağrıyı yapan ekranın kapanması hazırlama işini iptal etmemelidir.
- Arka plan hataları kontrolsüz coroutine hatasına dönüşmemelidir.
- `Failed` durumundan tekrar hazırlama yapılabilmelidir.

## Faz 2 — Session Sahipliği ile Kamera Analizinin Ayrılması

### Dosyalar

- Değişecek: `PlateRecognitionAnalyzer.kt`
- Yeni: `PlateRecognitionModelResources.kt`
- Değişecek: analyzer birim testleri

### Yapılacaklar

- Detector ve OCR session sahipliği model yöneticisine taşınacak.
- Analyzer yalnızca kamera oturumuna ait geçici durumu yönetecek:
  - frame işleme,
  - tracking,
  - OCR consensus,
  - sonuç kilidi.
- Her kamera kaydında yeni tracker ve doğrulama durumu oluşturulacak.
- Analyzer kapanınca yalnızca model kaynaklarına ait kullanım hakkını bırakacak; session'ları kapatmayacak.
- Aynı model yöneticisinin aynı anda iki analyzer tarafından kullanılmasını engelleyen açık bir kullanım hakkı mekanizması eklenecek.

### Kabul Kriterleri

- Analyzer kapatıldığında detector ve OCR session'ları açık kalmalıdır.
- Model yöneticisi kapatıldığında session'lar tam bir kez kapanmalıdır.
- Kamera ekranına yeniden girildiğinde tracking ve OCR consensus temiz başlamalıdır.
- Eski ekrandan frame veya doğrulama durumu yeni ekrana taşınmamalıdır.

## Faz 3 — Gerçek Model Yükleme ve Warm-up

### Dosyalar

- Değişecek: `NanoDetDetector.kt`
- Değişecek: `FastPlateOcr.kt`
- Değişecek: `PlateRecognitionModelManager.kt`
- Değişecek: ilgili birim testleri

### Yapılacaklar

- Detector ve OCR sınıflarına dahili `warmUp()` davranışı eklenecek.
- NanoDet için gerçek sözleşmeye uygun `[1, 3, 256, 256]` dummy giriş kullanılacak.
- OCR için `[1, 64, 128, 3]` dummy giriş kullanılacak.
- Çıktılar işlenmesi gereken minimum seviyede alınacak ve sonuç olarak yayınlanmadan atılacak.
- Her model için bir warm-up çıkarımı çalıştırılacak.
- Kısmi başarısızlıkta daha önce açılmış kaynaklar kapatılacak.
- Hazırlama metrikleri kaydedilecek:

```kotlin
data class PlateRecognitionPreparationMetrics(
    val detectorLoadNanos: Long,
    val ocrLoadNanos: Long,
    val detectorWarmUpNanos: Long,
    val ocrWarmUpNanos: Long,
    val totalNanos: Long,
)
```

### Kabul Kriterleri

- Her session ve warm-up aynı hazırlama döngüsünde tam bir kez çalışmalıdır.
- Yükleme ve warm-up ana iş parçacığında çalışmamalıdır.
- Detector veya OCR hazırlığı hata verirse kısmi session sızıntısı oluşmamalıdır.

## Faz 4 — Kamera API'si ve Geriye Dönük Uyumluluk

### Dosyalar

- Değişecek: `PlateRecognitionRegistration.android.kt`
- Değişecek: ilgili kayıt testleri
- Değişecek: `PlateRecognitionAnalyzer.kt`

### Yapılacaklar

- Hazırlanmış model yöneticisini alan yeni `registerPlateRecognition` overload'u eklenecek.
- Mevcut model dosyalarını alan overload korunacak ve mevcut davranışını sürdürecek.
- Yönetici tabanlı kayıt yalnızca `Ready` durumunda başlayacak; hazır değilse açık ve anlaşılır hata üretecek.
- `PlateRecognitionHandle.close()` kamera analizini ve kullanım hakkını bırakacak, model session'larını kapatmayacak.
- İlk sürümde yönetici tabanlı yol `ModelSessionReleasePolicy.AFTER_SUCCESS` seçeneğini reddedecek. Başarılı sonuçtan sonra kamerayı kapatma sorumluluğu tüketici uygulamada handle üzerinden kalacak.

### Kabul Kriterleri

- Eski API testleri değişmeden geçmelidir.
- Kamera handle'ı kapatıldıktan sonra model yöneticisi `Ready` kalmalıdır.
- İkinci kamera kaydı session'ları yeniden açmamalıdır.
- Aynı yöneticiyle ikinci eşzamanlı kayıt açık hata vermelidir.

## Faz 5 — ViewModel ve Örnek Uygulama Entegrasyonu

### Dosyalar

- Yeni: `sample/composeApp/src/androidMain/kotlin/global/deveng/core/PlateRecognitionModelsViewModel.android.kt`
- Değişecek: `sample/composeApp/src/androidMain/kotlin/global/deveng/core/App.android.kt`
- Değişecek: `sample/composeApp/src/androidMain/kotlin/global/deveng/core/PlateRecognitionValidationScreen.android.kt`

### Yapılacaklar

- Activity veya navigation-root kapsamındaki bir ViewModel model yöneticisinin sahibi olacak.
- Kullanıcı model seçtiğinde ViewModel `prepareModels` çağrısını yapacak.
- Kamera ekranı yeni yönetici oluşturmayacak; aynı yöneticiyi alacak.
- Kamera kaydı yalnızca hazırlık durumu `Ready` olduğunda başlayacak.
- Kamera ekranından çıkışta yalnızca `PlateRecognitionHandle` kapatılacak.
- Hazırlık sürerken diğer ekranın etkileşimleri devam edecek; durum gerekirse arayüzde gösterilebilecek.

### Kabul Kriterleri

- Model hazırlığı kamera dışındaki bir ekrandan başlatılabilmelidir.
- Hazırlığı başlatan ekranın kapanması işi sonlandırmamalıdır.
- Kamera ekranı hazır session'ları kullanmalıdır.
- Kamera ekranından çıkıp geri gelmek model yükleme ve warm-up işlemlerini tekrarlamamalıdır.

## Faz 6 — Kapatma, Model Değiştirme ve Düşük Bellek Politikası

### Dosyalar

- Değişecek: `PlateRecognitionModelManager.kt`
- Değişecek: model yöneticisi testleri
- Değişecek: örnek uygulama ViewModel'i

### Yapılacaklar

- Normal navigation model yöneticisini kapatmayacak.
- Uygulama kapsamı açıkça sonlandırıldığında `close()` hazırlama işini güvenli biçimde iptal edip session'ları kapatacak.
- Aktif analyzer varken model değiştirme isteği açık hata verecek.
- Analyzer kapatıldıktan sonra yeni model tanımıyla yeniden hazırlama yapılabilecek.
- Düşük bellek durumunda tüketici uygulamanın session'ları açıkça bırakabileceği bir yol sağlanacak.
- İşletim sistemi process'i sonlandırdığında ek uygulama callback'ine güvenilmeyecek; kaynak temizliği process yaşam döngüsüne bırakılacak.

### Kabul Kriterleri

- Hazırlama sırasında kapatma kısmi kaynak sızıntısı oluşturmamalıdır.
- Eski ve yeni model session'ları aynı analyzer içinde karışmamalıdır.
- Tekrarlı `close()` çağrıları güvenli olmalıdır.
- Kapatılmış yönetici yeniden kullanılamamalı ve açık hata üretmelidir.

## Faz 7 — Cihaz Doğrulaması ve Dokümantasyon

### Dosyalar

- Değişecek: `docs/deveng-core-kmp-plate-recognition-integration.md`
- Değişecek: `docs/deveng-core-kmp-plate-recognition-pipeline.md`
- Değişecek: `docs/deveng-core-kmp-android-plate-recognition-detailed-work-plan.md`

### Otomatik Doğrulama

```powershell
.\gradlew.bat :deveng-core:testDebugUnitTest --no-daemon
.\gradlew.bat :sample:composeApp:assembleDebug --no-daemon
```

### Cihaz Senaryoları

1. Uygulamanın soğuk açılışı.
2. Kamera dışındaki ekrandan model hazırlama.
3. Hazırlama sürerken başka ekrana geçiş.
4. Hazırlık tamamlandıktan sonra kamera ekranına giriş.
5. Kamera ekranından çıkış ve tekrar giriş.
6. Model dosyalarının değiştirilmesi.
7. Uygulamanın arka plana alınıp geri getirilmesi.
8. Process'in işletim sistemi tarafından sonlandırılması ve yeniden açılış.

### Kabul Kriterleri

- İkinci kamera girişinde detector ve OCR session açılış sayısı sıfır olmalıdır.
- Model hazırlığı sırasında kullanıcı arayüzü bloke olmamalıdır.
- İlk kullanıcıya görünür detection süresi kararlı durum p95 değerine yakın olmalıdır.
- Model warm-up sonrasında preprocessing hâlâ belirgin ölçüde soğuksa, ilk gerçek kamera frame'inin sonucu yalnızca ölçüme dayanarak atılmalıdır.
- PSS ve native heap etkisi raporlanmalıdır.
- Process yeniden başladığında modellerin yeniden hazırlanacağı dokümante edilmelidir.

## Reddedilen Alternatifler

- Model yöneticisini kamera ekranının ViewModel'inde tutmak: ekran kapsamı kapandığında session ömrü de sona erebilir.
- Kütüphane içinde gizli global singleton kullanmak: sahiplik, test edilebilirlik ve birden fazla model yapılandırması belirsizleşir.
- Her ViewModel'in kendi coroutine'iyle hazırlık başlatması: iptal ve hata yönetimi tüketici uygulamalarda dağılır.
- 100 warm-up çıkarımı yapmak: başlangıç maliyetini ve enerji tüketimini gereksiz artırır.
- Detector ve OCR yüklemesini doğrudan paralel başlatmak: düşük bellekli cihazlarda tepe bellek ve CPU baskısını artırabilir.
- Genel `unregisterFrameAnalyzer` davranışını değiştirmek: model session sahipliğiyle ilgisiz analyzer'ların yaşam döngüsünü bozabilir.

## Faz Geçiş Kuralı

Her faz kendi kabul kriterleriyle uygulanıp doğrulanacaktır. Bir varsayım kod tarafından yanlışlanırsa veya plan mevcut mimariye uymuyorsa sonraki tasarıma sessizce geçilmeyecek; plan güncellenip yeniden onay alınacaktır.

## Referanslar

- [PlateRecognitionAnalyzer.kt](../deveng-core/src/androidMain/kotlin/core/domain/camera/analysis/plate/PlateRecognitionAnalyzer.kt)
- [PlateRecognitionRegistration.android.kt](../deveng-core/src/androidMain/kotlin/core/domain/camera/controller/PlateRecognitionRegistration.android.kt)
- [NanoDetDetector.kt](../deveng-core/src/androidMain/kotlin/core/domain/camera/analysis/nanodet/NanoDetDetector.kt)
- [FastPlateOcr.kt](../deveng-core/src/androidMain/kotlin/core/domain/camera/analysis/ocr/FastPlateOcr.kt)
- [PlateRecognitionValidationScreen.android.kt](../sample/composeApp/src/androidMain/kotlin/global/deveng/core/PlateRecognitionValidationScreen.android.kt)

Yazar: **OpenAI Codex**
