package core.domain.camera.scanner

import core.domain.camera.state.CameraKPlugin
import core.domain.camera.state.CameraKStateHolder

/**
 * Scans QR codes from the live camera feed.
 *
 * The plugin attaches to a [CameraKStateHolder], waits for the camera to become
 * [core.domain.camera.state.CameraKState.Ready], hooks into the platform's frame/metadata
 * pipeline, and emits [core.domain.camera.state.CameraKEvent.QRCodeScanned] for each
 * successful decode (subject to [QrScannerConfig.scanThrottleMs] and
 * [QrScannerConfig.pauseOnFirstScan]).
 *
 * ## Platform backends
 *
 * - **Android** — CameraX `ImageAnalysis` + Google ML Kit Barcode Scanning (bundled).
 * - **iOS** — AVFoundation `AVCaptureMetadataOutput` with native `AVMetadataObjectTypeQRCode`.
 * - **Desktop** — JavaCV frame stream + OpenCV `QRCodeDetector` (bundled via
 *   `opencv_objdetect`).
 * - **WASM** — Browser `getUserMedia` + `HTMLVideoElement` + jsQR (loaded via `<script>` in
 *   your `index.html`).
 *
 * ## Usage
 *
 * Low-level — attach directly to any camera:
 * ```kotlin
 * val cameraState by rememberCameraKState(
 *     setupPlugins = { holder ->
 *         holder.attachPlugin(QrScannerPlugin(QrScannerConfig(pauseOnFirstScan = true)))
 *     },
 * )
 * ```
 *
 * High-level — use the convenience composable:
 * ```kotlin
 * QrScannerView(onQrScanned = { code -> viewModel.onScan(code) })
 * ```
 *
 * ## Permissions
 *
 * The plugin does not request camera permission. Callers must ensure the CAMERA permission
 * is granted (e.g. via `PermissionsController.RequestCameraPermission`) before the host
 * composable enters composition.
 *
 * ## WASM note
 *
 * On WASM the plugin depends on the global `jsQR` function. Consumers must add the
 * following to their `index.html`:
 *
 * ```html
 * <script src="https://cdn.jsdelivr.net/npm/jsqr@1.4.0/dist/jsQR.js"></script>
 * ```
 */
expect class QrScannerPlugin(config: QrScannerConfig = QrScannerConfig()) : CameraKPlugin {
    override fun onAttach(stateHolder: CameraKStateHolder)

    override fun onDetach()
}
