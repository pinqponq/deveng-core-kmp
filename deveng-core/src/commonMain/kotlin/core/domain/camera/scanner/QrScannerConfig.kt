package core.domain.camera.scanner

import androidx.compose.runtime.Immutable

/**
 * Configuration for [QrScannerPlugin].
 *
 * @property onQrScanned Optional callback invoked for every scan that passes throttling and
 *   deduplication. The plugin also emits
 *   [core.domain.camera.state.CameraKEvent.QRCodeScanned] on the state holder's event flow
 *   regardless — this callback is a convenience for the single-handler case.
 * @property scanThrottleMs Minimum interval (ms) before the *same* code is re-emitted.
 *   Useful to prevent a single QR in the viewfinder from firing the handler 10× per second.
 *   Set to `0L` to disable throttling. Default 1500 ms.
 * @property pauseOnFirstScan When `true`, the plugin stops forwarding detections after the
 *   first successful scan. The caller must detach and re-attach (or rebuild a new plugin
 *   instance) to resume scanning. Default `false`.
 * @property acceptedFormats Reserved for future barcode expansion. In v1 this is always
 *   treated as `[QrBarcodeFormat.QR_CODE]` regardless of the supplied list.
 */
@Immutable
data class QrScannerConfig(
    val onQrScanned: ((String) -> Unit)? = null,
    val scanThrottleMs: Long = 1500L,
    val pauseOnFirstScan: Boolean = false,
    val acceptedFormats: List<QrBarcodeFormat> = listOf(QrBarcodeFormat.QR_CODE),
)
