package core.domain.camera.analysis.ocr.roi

import android.graphics.Bitmap
import android.graphics.Matrix
import core.domain.camera.analysis.AndroidCameraFrame

/**
 * A complete camera frame copied before CameraX releases its backing buffer.
 *
 * RGBA bytes are retained while consensus is pending. Pixel conversion, rotation, and Bitmap
 * allocation are deferred until [bitmap] is requested for the winning frame.
 */
class CapturedCameraFrame internal constructor(
    internal val packedRgba: ByteArray,
    val sourceWidth: Int,
    val sourceHeight: Int,
    val rotationDegrees: Int,
    val timestampNanos: Long
) {
    @Volatile
    var bitmapRenderNanos: Long = 0L
        private set

    private val lazyBitmap = lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        val startedNanos = System.nanoTime()
        val rendered = renderBitmap()
        bitmapRenderNanos = System.nanoTime() - startedNanos
        rendered
    }

    val bitmap: Bitmap
        get() = lazyBitmap.value

    internal val hasMaterializedBitmap: Boolean
        get() = lazyBitmap.isInitialized()

    private fun renderBitmap(): Bitmap {
        val pixels = packedRgbaToArgbPixels(packedRgba, sourceWidth, sourceHeight)
        val source = Bitmap.createBitmap(
            pixels,
            sourceWidth,
            sourceHeight,
            Bitmap.Config.ARGB_8888
        )

        return if (rotationDegrees == 0) {
            source
        } else {
            Bitmap.createBitmap(
                source,
                0,
                0,
                source.width,
                source.height,
                Matrix().apply { postRotate(rotationDegrees.toFloat()) },
                true
            ).also { source.recycle() }
        }
    }
}

/**
 * Converts packed RGBA byte array into ARGB_8888 IntArray suitable for [Bitmap.createBitmap].
 */
internal fun packedRgbaToArgbPixels(packedRgba: ByteArray, width: Int, height: Int): IntArray {
    val pixels = IntArray(width * height)
    var sourceIndex = 0

    for (pixelIndex in pixels.indices) {
        val red = packedRgba[sourceIndex].toInt() and 0xFF
        val green = packedRgba[sourceIndex + 1].toInt() and 0xFF
        val blue = packedRgba[sourceIndex + 2].toInt() and 0xFF
        val alpha = packedRgba[sourceIndex + 3].toInt() and 0xFF
        pixels[pixelIndex] = (alpha shl 24) or (red shl 16) or (green shl 8) or blue
        sourceIndex += 4
    }

    return pixels
}

/** Copies packed RGBA bytes without retaining the CameraX buffer or creating a Bitmap. */
internal object FullFrameCapture {

    fun capture(frame: AndroidCameraFrame): CapturedCameraFrame {
        val plane = frame.planes.single()
        val packedRowBytes = frame.width * 4
        val totalBytes = packedRowBytes * frame.height
        val buffer = plane.buffer.duplicate()
        val startPosition = buffer.position()
        val packedRgba = ByteArray(totalBytes)

        if (plane.rowStride == packedRowBytes) {
            buffer.limit(startPosition + totalBytes)
            buffer.get(packedRgba)
        } else {
            for (y in 0 until frame.height) {
                val rowStart = startPosition + y * plane.rowStride
                buffer.position(rowStart)
                buffer.get(packedRgba, y * packedRowBytes, packedRowBytes)
            }
        }

        return CapturedCameraFrame(
            packedRgba = packedRgba,
            sourceWidth = frame.width,
            sourceHeight = frame.height,
            rotationDegrees = frame.rotationDegrees,
            timestampNanos = frame.timestamp
        )
    }
}
