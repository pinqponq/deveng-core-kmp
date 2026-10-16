# deveng-core-kmp Mevcut Plaka Okuma Pipeline'ı

> Durum: Faz 6 kapsamındaki mevcut Android pipeline'ı
>
> Amaç: Kamera komponentini kullanan uygulamalara plaka okuma hizmeti sunmak

Bu doküman, `deveng-core-kmp` içindeki mevcut kamera–plaka okuma akışını ve tüketici uygulamanın bu akışla nasıl etkileşeceğini özetler. Kütüphane; frame alma, model çalıştırma, plaka ROI'sini çıkarma, OCR doğrulama ve sonucu callback ile iletme işlerini üstlenir. Okunan plakayla ne yapılacağına tüketici uygulama karar verir.

## 1. Sorumluluk paylaşımı

| Tüketici uygulama | `deveng-core-kmp` |
| --- | --- |
| Detector ve OCR model dosyalarını `File` olarak sağlar. | Kamera frame akışını ve frame yaşam döngüsünü yönetir. |
| Kamera izni, ekran, model indirme ve özel depolamayı yönetir. | CameraX backpressure ve analiz zamanlamasını yönetir. |
| `onResult` geldiğinde UI, API, kayıt, ses veya navigasyon kararını verir. | NanoDet, source-frame bbox eşleme, kalite kapısı ve FastPlateOCR akışını çalıştırır. |
| Gerekirse `unregisterFrameAnalyzer` ile analizi durdurur. | Eşleşen OCR okumalarını doğrular ve `PlateRecognitionResult` üretir. |

Bu nedenle tüketici uygulamanın aldığı hizmet şudur: “Kamera akışında doğrulanmış bir plaka okundu.” Kütüphane, plaka okunduktan sonra araç açma, API çağrısı, kayıt veya ekran değişimi gibi iş kararlarını vermez.

## 2. Mevcut akış

```text
CameraX ImageAnalysis
        |
        v
KEEP_ONLY_LATEST
        |
        v
Frame analyzer adapter
  - frame seçimi ve minimum aralık
  - aynı anda en fazla bir analiz
        |
        v
NanoDet plaka tespiti
        |
        v
Source-frame bbox + kalite kapısı
        |
        v
Seçilen source-frame ROI
        |
        v
FastPlateOCR
        |
        v
2/3 rolling consensus doğrulaması
        |
        v
onResult(PlateRecognitionResult)
```

Akışın önemli noktaları:

1. CameraX analiz kuyruğu `KEEP_ONLY_LATEST` kullanır. Analiz yavaşladığında bütün eski frame'ler sıraya alınmaz.
2. Adapter; `analyzeEveryNthFrame` ve `minAnalysisIntervalMs` ayarlarını uygular. Analiz meşgulse gelen frame serbest bırakılır.
3. NanoDet frame üzerinde plaka adaylarını bulur. Adaylar source-frame koordinatlarına taşınır ve confidence, NMS ve minimum bbox boyutu kalite kontrollerinden geçirilir.
4. OCR, bütün frame üzerinde değil, kalite kapısından geçen plaka ROI'si üzerinde çalışır. ROI; kamera rotasyonuna göre düzeltilir, 64×128 boyutuna getirilir ve FastPlateOCR modeline verilir. Mevcut plate analyzer kamera analiz akışında `RGBA_8888` çıktı formatını bekler.
5. Yalnızca varsayılan `%90` OCR confidence eşiğini geçen, boş olmayan sonuçlar doğrulama penceresine girer. Son üç geçerli okumada aynı plaka en az iki kez bulunursa 2/3 rolling consensus sağlanır. Araya farklı veya düşük confidence değerli bir tahmin girmesi pencereyi sıfırlamaz; düşük confidence tahmini pencereye hiç eklenmez.
6. Sonuç, `plate` ve consensus'u destekleyen OCR okumalarının ortalama confidence değeriyle callback'e gönderilir.

## 3. 100 frame örneği

Aşağıdaki sayılar davranışı açıklamak için verilmiş temsili bir senaryodur; cihaz, çözünürlük, model ve analiz süresine göre gerçek sayılar değişir.

Örnek olarak kamera callback'ine 100 frame ulaştığını ve validation ekranındaki gibi şu ayarların kullanıldığını varsayalım:

```text
analyzeEveryNthFrame = 1
minAnalysisIntervalMs = 0
lockAfterSuccessfulRead = false  // validation ekranı
```

Adapter aynı anda yalnızca bir analiz çalıştırır. Örneğin 100 frame'in dağılımı şöyle olabilir:

| Aşama | Örnek sayı | Açıklama |
| --- | ---: | --- |
| Kamera callback'ine ulaşan frame | 100 | F1–F100 arası tüm callback'ler |
| Analize kabul edilen frame | 11 | Adapter'ın meşgul olmadığı frame'ler |
| Adapter meşgul olduğu için işlenmeyen frame | 89 | Model çağrısı yapılmadan kapatılır |
| NanoDet çağrısı | 11 | Her kabul edilen frame önce detector'a gider |
| Kalite kapısından geçen plaka adayı | 4 | Diğerlerinde plaka yoktur veya aday yeterli kalitede değildir |
| OCR çağrısı | 4 | Yalnızca seçilen ROI'ler OCR'a gider |
| Confidence eşiğini geçen OCR okuması | 3 | Örneğin bir okuma `%90` eşiğinin altında kaldığı için pencereye alınmaz |
| Consensus'u destekleyen okuma | 2 | Örneğin son üç geçerli okumada `34ABC123` iki kez okunur |
| `onResult` çağrısı | 1 | Tüketici uygulamaya doğrulanmış sonuç iletilir |

Bu örnekte 100 frame'in tamamında NanoDet ve OCR çalışmaz. Frame'ler sonuç üretmek için adaydır; pipeline, cihazı ve model oturumunu gereksiz tekrarlarla doldurmamak için yalnızca uygun frame'leri işler.

Eğer tüketici uygulama `analyzeEveryNthFrame = 5` seçerse ilk seçim aşamasında 100 frame'den yalnızca F1, F6, F11, …, F96 olmak üzere 20 frame analiz adayı olur. Minimum zaman aralığı ve “tek analiz” kuralı bu 20 adayın sayısını daha da azaltabilir.

Production varsayılanında `lockAfterSuccessfulRead = true` olduğu için 2/3 consensus sonrası sonuç gönderilir ve analyzer yeni inference kabul etmeyi bırakır. Kamera ekranından başka bir ekrana geçme kararını tüketici uygulama `onResult` içinde verir. Validation ekranında kilit `false` ayarlanmıştır; her consensus sonucundan sonra doğrulama penceresi sıfırlanır ve kamera akışı üzerinde yeni plaka sonuçları aranabilir. Model session'ları analyzer kapanana kadar korunur; navigation ile session yaşam döngüsünü ayrı bir config üzerinden yönetmek bu sürümün kapsamında değildir.

## 4. Tüketici uygulama için sonuç ucu

Model dosyaları tüketici uygulamanın private storage alanında tutulabilir ve analyzer'a `File` olarak verilir. Model oturumları analyzer oluşturulurken açılır ve frame'ler arasında yeniden kullanılır.

Kavramsal kullanım:

```kotlin
val analyzer = PlateRecognitionAnalyzer(
    detectorModelFile = detectorModelFile,
    ocrModelFile = ocrModelFile,
    config = PlateRecognitionConfig(
        lockAfterSuccessfulRead = true,
        requiredMatchingReads = 2,
        validationWindowSize = 3,
        ocrConfidenceThreshold = 0.90f,
    ),
)

cameraController.registerFrameAnalyzer(
    analyzer = analyzer,
    onResult = { result ->
        // Kararı tüketici uygulama verir:
        // UI güncelle, API çağır, kayıt yap, ses çal veya kamerayı durdur.
        val plate = result.plate
        val confidence = result.confidence
    },
    onError = { error ->
        // Model veya analiz hatasını tüketici uygulama ele alır.
    },
    outputImageFormat = analyzer.requiredOutputImageFormat,
)
```

Callback sonucu:

```kotlin
data class PlateRecognitionResult(
    val plate: String,
    val confidence: Float,
)
```

`onResult` ve `onError` controller'ın analiz executor'ı üzerinde çalışır. Tüketici uygulama sonucu Compose/UI state'ine aktaracaksa kendi UI thread/state yönetimini uygulamalıdır.

## 5. Yaşam döngüsü ve mevcut sınır

- `registerFrameAnalyzer` analyzer'ı başlatır.
- `unregisterFrameAnalyzer` analyzer'ı durdurur ve model oturumlarını kapatır.
- İşlenen, atlanan veya hata alan frame'lerin `ImageProxy` sahipliği pipeline tarafından sonlandırılır; tüketici uygulamanın frame'i kapatması gerekmez.
- `PlateRecognitionValidationScreen`, model seçimi, debug bbox overlay'i ve sürekli okuma davranışını gösteren sample/test ekranıdır. Tüketici uygulamanın sunacağı asıl servis callback ve sonuç modelidir.
- Plaka sonucundan sonra hangi iş aksiyonunun yapılacağı mevcut core pipeline'ın parçası değildir; bu karar `onResult` sonrasında uygulamaya bırakılmıştır.

## Kaynaklar

- [PlateRecognitionAnalyzer](../deveng-core/src/androidMain/kotlin/core/domain/camera/analysis/plate/PlateRecognitionAnalyzer.kt)
- [CameraFrameAnalyzerAdapter](../deveng-core/src/androidMain/kotlin/core/domain/camera/analysis/CameraFrameAnalyzerAdapter.kt)
- [CameraController.android.kt](../deveng-core/src/androidMain/kotlin/core/domain/camera/controller/CameraController.android.kt)
- [FastPlateOcr](../deveng-core/src/androidMain/kotlin/core/domain/camera/analysis/ocr/FastPlateOcr.kt)
- [PlateRecognitionValidationScreen](../sample/composeApp/src/androidMain/kotlin/global/deveng/core/PlateRecognitionValidationScreen.android.kt)
- [Ayrıntılı entegrasyon dokümanı](./deveng-core-kmp-plate-recognition-integration.md)

**E. B.**
