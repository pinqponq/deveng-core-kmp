package core.domain.camera.scanner

import org.bytedeco.javacv.Java2DFrameConverter
import org.bytedeco.javacv.OpenCVFrameConverter
import org.bytedeco.opencv.opencv_objdetect.QRCodeDetector
import java.awt.image.BufferedImage

/**
 * Thin wrapper around OpenCV's [QRCodeDetector] (bundled via JavaCV's `opencv_objdetect`).
 *
 * Instantiated once per plugin and reused across frames — the detector and the frame
 * converters hold native resources, so constructing new ones per frame would waste
 * work. Not thread-safe: [decode] must be called from a single thread (the plugin
 * does this via its single collector coroutine).
 */
internal class OpenCvQrCodeDecoder {
    private val detector = QRCodeDetector()
    private val frameConverter = Java2DFrameConverter()
    private val matConverter = OpenCVFrameConverter.ToMat()

    /**
     * Decodes the first QR code found in [image], or returns `null` if no code is visible.
     *
     * Per-frame failures are expected — the caller polls frame after frame — so an empty
     * decode result and any OpenCV exceptions are swallowed as `null` rather than propagated.
     */
    fun decode(image: BufferedImage): String? = try {
        val frame = frameConverter.convert(image) ?: return null
        val mat = matConverter.convert(frame) ?: return null
        val text = detector.detectAndDecode(mat)?.string
        if (text.isNullOrEmpty()) null else text
    } catch (_: Throwable) {
        null
    }
}
