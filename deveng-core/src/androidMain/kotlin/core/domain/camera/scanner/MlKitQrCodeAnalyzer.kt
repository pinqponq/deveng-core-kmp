package core.domain.camera.scanner

import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

/**
 * CameraX [ImageAnalysis.Analyzer] that runs Google ML Kit barcode scanning on each frame
 * and forwards the first QR code's raw text to [onDetected].
 *
 * Uses the bundled ML Kit model (`com.google.mlkit:barcode-scanning`) so it works offline
 * without Google Play Services. Configured to scan QR codes only — other formats are
 * ignored at the ML Kit level, which avoids wasting CPU on them.
 */
internal class MlKitQrCodeAnalyzer(
    private val onDetected: (String) -> Unit,
) : ImageAnalysis.Analyzer {

    private val scanner: BarcodeScanner = BarcodeScanning.getClient(
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build(),
    )

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }
        val inputImage = InputImage.fromMediaImage(
            mediaImage,
            imageProxy.imageInfo.rotationDegrees,
        )
        scanner.process(inputImage)
            .addOnSuccessListener { barcodes ->
                barcodes.firstOrNull { it.rawValue != null }
                    ?.rawValue
                    ?.let(onDetected)
            }
            .addOnFailureListener { /* per-frame failures are expected; ignore */ }
            .addOnCompleteListener { imageProxy.close() }
    }

    /** Releases the underlying ML Kit client. Call from plugin `onDetach`. */
    fun close() {
        scanner.close()
    }
}
