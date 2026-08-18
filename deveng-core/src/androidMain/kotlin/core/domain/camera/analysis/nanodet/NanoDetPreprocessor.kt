package core.domain.camera.analysis.nanodet

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PixelFormat
import android.os.Trace
import core.domain.camera.analysis.AndroidCameraFrame
import core.domain.camera.analysis.onnx.OnnxModelErrorCode
import core.domain.camera.analysis.onnx.OnnxModelException
import java.nio.ByteBuffer
import java.nio.FloatBuffer

/** Converts CameraX frames directly into a caller-owned normalized BGR-CHW buffer. */
internal class NanoDetPreprocessor : AutoCloseable {
    private var rgbaDetectorPixels = IntArray(0)
    private var nativeSourceBitmap: Bitmap? = null
    private var nativeDetectorBitmap: Bitmap? = null
    private var nativeDetectorCanvas: Canvas? = null
    private var rgbaStagingBuffer: ByteBuffer? = null
    private var normalizedArgbTensor = FloatArray(0)
    private val nativeTransform = Matrix()
    private val nativePaint = Paint(Paint.FILTER_BITMAP_FLAG)

    fun preprocessInto(
        frame: AndroidCameraFrame,
        destination: FloatBuffer,
        includePreview: Boolean,
    ): NanoDetPreprocessResult {
        try {
            require(frame.format == PixelFormat.RGBA_8888) {
                "NanoDet preprocessing requires PixelFormat.RGBA_8888; found ${frame.format}."
            }
            return preprocessRgbaNativeBitmapFrame(frame, destination, includePreview)
        } catch (error: OnnxModelException) {
            throw error
        } catch (error: Exception) {
            throw OnnxModelException(
                code = OnnxModelErrorCode.FRAME_CONVERSION_FAILED,
                message = "Could not convert CameraX frame into NanoDet input.",
                cause = error,
            )
        }
    }

    private fun preprocessRgbaNativeBitmapFrame(
        frame: AndroidCameraFrame,
        destination: FloatBuffer,
        includePreview: Boolean,
    ): NanoDetPreprocessResult {
        val frameTransferStartNanos = System.nanoTime()
        val sourceBitmap = traced("NanoDet/frame-transfer") { copyRgbaFrameToReusableBitmap(frame) }
        val frameTransferNanos = elapsedNanoseconds(frameTransferStartNanos)

        val rotateScaleStartNanos = System.nanoTime()
        val detectorBitmap = traced("NanoDet/rotate-scale") {
            drawRotatedScaledBitmap(sourceBitmap, frame.rotationDegrees)
        }
        val rotateScaleNanos = elapsedNanoseconds(rotateScaleStartNanos)

        val pixelReadStartNanos = System.nanoTime()
        val pixels = traced("NanoDet/pixel-read") { readDetectorPixels(detectorBitmap) }
        val pixelReadNanos = elapsedNanoseconds(pixelReadStartNanos)

        val normalizationStartNanos = System.nanoTime()
        traced("NanoDet/normalize") {
            normalizeArgbInto(
                pixels,
                detectorBitmap.width,
                detectorBitmap.height,
                destination,
            )
        }
        val normalizationNanos = elapsedNanoseconds(normalizationStartNanos)

        val previewStartNanos = System.nanoTime()
        val preview = if (includePreview) {
            detectorBitmap.copy(Bitmap.Config.ARGB_8888, false)
        } else {
            null
        }
        val previewSnapshotNanos = if (preview == null) 0 else elapsedNanoseconds(previewStartNanos)
        return NanoDetPreprocessResult(
            detectorInput = preview,
            transform = FrameTransform.directResize(
                sourceWidth = frame.width,
                sourceHeight = frame.height,
                inputWidth = NanoDetModelContract.INPUT_SIZE,
                inputHeight = NanoDetModelContract.INPUT_SIZE,
                rotationDegrees = frame.rotationDegrees,
            ),
            timings = NanoDetPreprocessTimings(
                frameTransferNanos = frameTransferNanos,
                rotateScaleNanos = rotateScaleNanos,
                pixelReadNanos = pixelReadNanos,
                normalizationNanos = normalizationNanos,
                previewSnapshotNanos = previewSnapshotNanos,
            ),
        )
    }

    private fun copyRgbaFrameToReusableBitmap(frame: AndroidCameraFrame): Bitmap {
        val plane = frame.planes.singleOrNull()
            ?: throw IllegalArgumentException("RGBA camera frame must have exactly one plane.")
        require(plane.pixelStride == RGBA_BYTES_PER_PIXEL) {
            "RGBA_NATIVE_BITMAP requires pixelStride=4; found ${plane.pixelStride}."
        }
        val width = frame.width
        val height = frame.height
        require(width > 0 && height > 0) { "RGBA frame dimensions must be positive." }
        val packedRowBytes = width * RGBA_BYTES_PER_PIXEL
        require(plane.rowStride >= packedRowBytes) {
            "RGBA rowStride ${plane.rowStride} is smaller than $packedRowBytes."
        }
        val buffer = plane.buffer.duplicate()
        val startPosition = buffer.position()
        val requiredLimit = startPosition + (height - 1) * plane.rowStride + packedRowBytes
        require(requiredLimit <= buffer.limit()) {
            "RGBA camera plane does not contain the declared frame dimensions."
        }

        val bitmap = reusableSourceBitmap(width, height)
        if (plane.rowStride == packedRowBytes) {
            buffer.limit(startPosition + height * packedRowBytes)
            bitmap.copyPixelsFromBuffer(buffer)
            return bitmap
        }

        val staging = reusableRgbaStagingBuffer(packedRowBytes * height)
        staging.clear()
        val originalLimit = buffer.limit()
        for (row in 0 until height) {
            val rowStart = startPosition + row * plane.rowStride
            buffer.position(rowStart)
            buffer.limit(rowStart + packedRowBytes)
            staging.put(buffer)
            buffer.limit(originalLimit)
        }
        staging.flip()
        bitmap.copyPixelsFromBuffer(staging)
        return bitmap
    }

    private fun reusableSourceBitmap(width: Int, height: Int): Bitmap {
        val current = nativeSourceBitmap
        if (current != null && current.width == width && current.height == height && !current.isRecycled) {
            return current
        }
        current?.recycle()
        return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also {
            nativeSourceBitmap = it
        }
    }

    private fun reusableDetectorBitmap(): Bitmap {
        val current = nativeDetectorBitmap
        if (current != null && !current.isRecycled) return current
        val bitmap = Bitmap.createBitmap(
            NanoDetModelContract.INPUT_SIZE,
            NanoDetModelContract.INPUT_SIZE,
            Bitmap.Config.ARGB_8888,
        )
        nativeDetectorBitmap = bitmap
        nativeDetectorCanvas = Canvas(bitmap)
        return bitmap
    }

    private fun reusableRgbaStagingBuffer(requiredBytes: Int): ByteBuffer {
        val current = rgbaStagingBuffer
        if (current != null && current.capacity() >= requiredBytes) return current
        return ByteBuffer.allocateDirect(requiredBytes).also { rgbaStagingBuffer = it }
    }

    private fun drawRotatedScaledBitmap(source: Bitmap, rotationDegrees: Int): Bitmap {
        require(rotationDegrees in SUPPORTED_ROTATIONS) { "Unsupported frame rotation: $rotationDegrees" }
        val target = reusableDetectorBitmap()
        val outputSize = NanoDetModelContract.INPUT_SIZE.toFloat()
        val width = source.width.toFloat()
        val height = source.height.toFloat()
        nativeTransform.setValues(
            when (rotationDegrees) {
                0 -> floatArrayOf(
                    outputSize / width, 0f, 0f,
                    0f, outputSize / height, 0f,
                    0f, 0f, 1f,
                )
                90 -> floatArrayOf(
                    0f, -outputSize / height, outputSize,
                    outputSize / width, 0f, 0f,
                    0f, 0f, 1f,
                )
                180 -> floatArrayOf(
                    -outputSize / width, 0f, outputSize,
                    0f, -outputSize / height, outputSize,
                    0f, 0f, 1f,
                )
                270 -> floatArrayOf(
                    0f, outputSize / height, 0f,
                    -outputSize / width, 0f, outputSize,
                    0f, 0f, 1f,
                )
                else -> error("Rotation was validated before drawing.")
            },
        )
        nativeDetectorCanvas?.drawBitmap(source, nativeTransform, nativePaint)
            ?: error("Reusable detector Canvas is not initialized.")
        return target
    }

    private fun readDetectorPixels(bitmap: Bitmap): IntArray {
        val pixels = rgbaDetectorPixels(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        return pixels
    }

    private fun rgbaDetectorPixels(size: Int): IntArray {
        if (rgbaDetectorPixels.size < size) rgbaDetectorPixels = IntArray(size)
        return rgbaDetectorPixels
    }

    private fun normalizeArgbInto(
        pixels: IntArray,
        width: Int,
        height: Int,
        destination: FloatBuffer,
    ) {
        require(width > 0 && height > 0) { "Bitmap dimensions must be positive." }
        val tensorSize = 3 * width * height
        argbToNormalizedBgrChwInto(
            pixels = pixels,
            width = width,
            height = height,
            destination = destination,
            scratch = reusableNormalizedArgbTensor(tensorSize),
        )
    }

    private fun reusableNormalizedArgbTensor(size: Int): FloatArray {
        if (normalizedArgbTensor.size < size) normalizedArgbTensor = FloatArray(size)
        return normalizedArgbTensor
    }

    private fun elapsedNanoseconds(startNanos: Long): Long = System.nanoTime() - startNanos

    private inline fun <T> traced(sectionName: String, block: () -> T): T {
        Trace.beginSection(sectionName)
        return try {
            block()
        } finally {
            Trace.endSection()
        }
    }

    override fun close() {
        nativeDetectorBitmap?.recycle()
        nativeDetectorBitmap = null
        nativeDetectorCanvas = null
        nativeSourceBitmap?.recycle()
        nativeSourceBitmap = null
        rgbaStagingBuffer = null
        normalizedArgbTensor = FloatArray(0)
    }

    private companion object {
        const val RGBA_BYTES_PER_PIXEL = 4
        const val NANOS_PER_MILLISECOND = 1_000_000L
        val SUPPORTED_ROTATIONS = setOf(0, 90, 180, 270)
    }
}

internal data class NanoDetPreprocessResult(
    val detectorInput: Bitmap?,
    val transform: FrameTransform,
    val timings: NanoDetPreprocessTimings,
)

internal data class NanoDetPreprocessTimings(
    val frameTransferNanos: Long = 0,
    val rotateScaleNanos: Long = 0,
    val pixelReadNanos: Long = 0,
    val normalizationNanos: Long = 0,
    val previewSnapshotNanos: Long = 0,
) {
    val totalNanos: Long
        get() = frameTransferNanos + rotateScaleNanos + pixelReadNanos + normalizationNanos +
            previewSnapshotNanos
}

internal fun argbToNormalizedBgrChw(
    pixels: IntArray,
    width: Int,
    height: Int,
): FloatArray {
    val tensor = FloatArray(3 * width * height)
    argbToNormalizedBgrChw(pixels, width, height, tensor)
    return tensor
}

internal fun argbToNormalizedBgrChwInto(
    pixels: IntArray,
    width: Int,
    height: Int,
    destination: FloatBuffer,
    scratch: FloatArray,
) {
    require(width > 0 && height > 0) { "Bitmap dimensions must be positive." }
    val planeSize = width * height
    require(pixels.size >= planeSize) {
        "ARGB input contains ${pixels.size} pixels; expected $planeSize."
    }
    val tensorSize = 3 * planeSize
    require(destination.capacity() >= tensorSize) {
        "Destination buffer contains ${destination.capacity()} floats; expected at least $tensorSize."
    }
    require(scratch.size >= tensorSize) {
        "Scratch array contains ${scratch.size} floats; expected at least $tensorSize."
    }
    argbToNormalizedBgrChw(pixels, width, height, scratch)
    destination.position(0)
    destination.put(scratch, 0, tensorSize)
    destination.position(0)
}

private fun argbToNormalizedBgrChw(
    pixels: IntArray,
    width: Int,
    height: Int,
    destination: FloatArray,
) {
    require(width > 0 && height > 0) { "Bitmap dimensions must be positive." }
    require(pixels.size >= width * height) {
        "ARGB input contains ${pixels.size} pixels; expected ${width * height}."
    }
    val planeSize = width * height
    require(destination.size >= 3 * planeSize) {
        "Destination array contains ${destination.size} floats; expected at least ${3 * planeSize}."
    }
    val greenOffset = planeSize
    val redOffset = planeSize * 2
    val blueLut = NanoDetNormalizationLut.blue
    val greenLut = NanoDetNormalizationLut.green
    val redLut = NanoDetNormalizationLut.red
    var index = 0
    while (index < planeSize) {
        val pixel = pixels[index]
        destination[index] = blueLut[pixel and 0xFF]
        destination[greenOffset + index] = greenLut[(pixel ushr 8) and 0xFF]
        destination[redOffset + index] = redLut[(pixel ushr 16) and 0xFF]
        index++
    }
}

private object NanoDetNormalizationLut {
    val blue: FloatArray = create(
        NanoDetModelContract.mean[0],
        NanoDetModelContract.standardDeviation[0],
    )
    val green: FloatArray = create(
        NanoDetModelContract.mean[1],
        NanoDetModelContract.standardDeviation[1],
    )
    val red: FloatArray = create(
        NanoDetModelContract.mean[2],
        NanoDetModelContract.standardDeviation[2],
    )

    private fun create(mean: Float, standardDeviation: Float): FloatArray =
        FloatArray(256) { value -> (value.toFloat() - mean) / standardDeviation }
}
