package core.domain.camera.scanner

/**
 * Code formats recognized by [QrScannerPlugin].
 *
 * Only [QR_CODE] is implemented in v1. The enum exists as an extensibility hook — a future
 * iteration can add `EAN_13`, `CODE_128`, `PDF_417`, etc. without changing the public API
 * of [QrScannerConfig].
 */
enum class QrBarcodeFormat {
    QR_CODE,
}
