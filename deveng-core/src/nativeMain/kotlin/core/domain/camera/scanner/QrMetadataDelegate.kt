package core.domain.camera.scanner

import platform.AVFoundation.AVCaptureConnection
import platform.AVFoundation.AVCaptureMetadataOutputObjectsDelegateProtocol
import platform.AVFoundation.AVCaptureOutput
import platform.AVFoundation.AVMetadataMachineReadableCodeObject
import platform.darwin.NSObject

/**
 * `AVCaptureMetadataOutputObjectsDelegate` bridge for [QrScannerPlugin] on iOS.
 *
 * The delegate receives metadata objects whenever the camera session detects a barcode
 * matching the configured types (see `updateMetadataObjectTypes` on the native
 * `CameraController`). We filter for [AVMetadataMachineReadableCodeObject] (the superclass
 * of all 1D/2D barcode metadata) and forward its `stringValue` to [onCode].
 *
 * The delegate is invoked on `dispatch_get_main_queue()` (set up in
 * `CameraController.setMetadataObjectsDelegate`) so [onCode] runs on the main thread and
 * can safely touch Kotlin/Compose state.
 */
internal class QrMetadataDelegate(
    private val onCode: (String) -> Unit,
) : NSObject(), AVCaptureMetadataOutputObjectsDelegateProtocol {

    override fun captureOutput(
        output: AVCaptureOutput,
        didOutputMetadataObjects: List<*>,
        fromConnection: AVCaptureConnection,
    ) {
        for (obj in didOutputMetadataObjects) {
            val readable = obj as? AVMetadataMachineReadableCodeObject ?: continue
            val value = readable.stringValue ?: continue
            onCode(value)
        }
    }
}
