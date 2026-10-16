package core.domain.camera.analysis

import android.graphics.Rect
import android.media.Image
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Read-only view of an Android CameraX frame.
 *
 * The instance is valid only while [CameraFrameAnalyzer.analyze] is running. The camera controller
 * closes the underlying frame after the analyzer returns.
 */
class AndroidCameraFrame internal constructor(
    private val imageProxy: ImageProxy,
    private val onReleased: (() -> Unit)? = null,
) {
    private val released = AtomicBoolean(false)

    val width: Int
        get() = imageProxy.width

    val height: Int
        get() = imageProxy.height

    val format: Int
        get() = imageProxy.format

    val rotationDegrees: Int
        get() = imageProxy.imageInfo.rotationDegrees

    val timestamp: Long
        get() = imageProxy.imageInfo.timestamp

    val cropRect: Rect
        get() = imageProxy.cropRect

    internal val planes: Array<ImageProxy.PlaneProxy>
        get() = imageProxy.planes

    /**
     * The underlying media image, when CameraX exposes one for this frame format.
     *
     * Consumers must read it before [CameraFrameAnalyzer.analyze] returns.
     */
    @OptIn(ExperimentalGetImage::class)
    val image: Image?
        get() = imageProxy.image

    /**
     * Releases the underlying CameraX reference after all required pixels have been copied.
     *
     * This is an internal pipeline hook. Generic analyzers should continue to let the adapter
     * release the frame in its finally block and must not read this frame after calling it.
     */
    internal fun releaseAfterRead() {
        if (released.compareAndSet(false, true)) {
            try {
                imageProxy.close()
            } finally {
                onReleased?.invoke()
            }
        }
    }
}
