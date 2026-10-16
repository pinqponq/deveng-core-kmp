package core.domain.camera.analysis.nanodet.pipeline

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import androidx.core.graphics.createBitmap
import core.domain.camera.analysis.AndroidCameraFrame
import core.domain.camera.analysis.nanodet.geometry.FrameTransform
import core.domain.camera.analysis.nanodet.model.NanoDetModelContract
import java.nio.ByteBuffer
import java.nio.FloatBuffer

/** Converts CameraX frames directly into a caller-owned normalized BGR-CHW buffer. */
internal class NanoDetPreprocessor(
    private val inputWidth: Int,
    private val inputHeight: Int
) : AutoCloseable {
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
        destination: FloatBuffer
    ): FrameTransform {
        val sourceBitmap = copyRgbaFrameToReusableBitmap(frame)
        val detectorBitmap = drawRotatedScaledBitmap(sourceBitmap, frame.rotationDegrees)
        val pixels = readDetectorPixels(detectorBitmap)

        normalizePixels(
            pixels = pixels,
            width = detectorBitmap.width,
            height = detectorBitmap.height,
            destination = destination
        )

        return FrameTransform.directResize(
            sourceWidth = frame.width,
            sourceHeight = frame.height,
            inputWidth = inputWidth,
            inputHeight = inputHeight,
            rotationDegrees = frame.rotationDegrees
        )
    }

    private fun copyRgbaFrameToReusableBitmap(frame: AndroidCameraFrame): Bitmap {
        val plane = frame.planes.single()
        val width = frame.width
        val height = frame.height
        val packedRowBytes = width * 4
        val buffer = plane.buffer.duplicate()
        val startPosition = buffer.position()

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
        return createBitmap(width, height).also {
            nativeSourceBitmap = it
        }
    }

    private fun reusableDetectorBitmap(): Bitmap {
        val current = nativeDetectorBitmap
        if (current != null && !current.isRecycled) return current
        val bitmap = createBitmap(inputWidth, inputHeight)
        nativeDetectorBitmap = bitmap
        nativeDetectorCanvas = Canvas(bitmap)
        return bitmap
    }

    private fun reusableRgbaStagingBuffer(requiredBytes: Int): ByteBuffer {
        val current = rgbaStagingBuffer
        if (current != null && current.capacity() >= requiredBytes) return current
        return ByteBuffer.allocateDirect(requiredBytes).also { rgbaStagingBuffer = it }
    }

    private fun drawRotatedScaledBitmap(
        source: Bitmap,
        rotationDegrees: Int
    ): Bitmap {  // kamera görüntüsünü modelin bekleyeceği hal için bitmap üretir
        val target = reusableDetectorBitmap()  // modelin bitmap'i

        val outputWidth = inputWidth.toFloat()
        val outputHeight = inputHeight.toFloat()
        val width = source.width.toFloat()
        val height = source.height.toFloat()

        nativeTransform.setValues(
            when (rotationDegrees) {  // burada trigonometrik işlemler yerine direkt çarpanlar ile döndürme yapılıyor
                0 -> floatArrayOf(
                    outputWidth / width, 0f, 0f,
                    0f, outputHeight / height, 0f,
                    0f, 0f, 1f
                )

                90 -> floatArrayOf(
                    0f, -outputWidth / height, outputWidth,
                    outputHeight / width, 0f, 0f,
                    0f, 0f, 1f
                )

                180 -> floatArrayOf(
                    -outputWidth / width, 0f, outputWidth,
                    0f, -outputHeight / height, outputHeight,
                    0f, 0f, 1f
                )

                270 -> floatArrayOf(
                    0f, outputWidth / height, 0f,
                    -outputHeight / width, 0f, outputHeight,
                    0f, 0f, 1f
                )

                else -> error("Rotation was validated before drawing.")
            }
        )

        nativeDetectorCanvas?.drawBitmap(source, nativeTransform, nativePaint)
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

    private fun normalizePixels(
        pixels: IntArray,
        width: Int,
        height: Int,
        destination: FloatBuffer
    ) {
        val tensorSize = 3 * width * height
        val scratch = reusableNormalizedArgbTensor(tensorSize)
        argbToNormalizedBgrChw(pixels, width, height, scratch)

        destination.position(0)
        destination.put(scratch, 0, tensorSize)
        destination.position(0)
    }

    private fun reusableNormalizedArgbTensor(size: Int): FloatArray {
        if (normalizedArgbTensor.size < size) normalizedArgbTensor = FloatArray(size)
        return normalizedArgbTensor
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

    internal companion object {
        internal fun argbToNormalizedBgrChw(
            pixels: IntArray,
            width: Int,
            height: Int,
        ): FloatArray {
            val tensor = FloatArray(3 * width * height)
            argbToNormalizedBgrChw(pixels, width, height, tensor)
            return tensor
        }

        internal fun argbToNormalizedBgrChw(
            pixels: IntArray,
            width: Int,
            height: Int,
            destination: FloatArray,
        ) {
            val pixelCount = width * height

            val blueStartIndex = 0
            val greenStartIndex = pixelCount
            val redStartIndex = pixelCount * 2

            for (pixelIndex in 0 until pixelCount) {

                val argbPixel = pixels[pixelIndex]

                val blue = argbPixel and 0xFF
                val green = (argbPixel ushr 8) and 0xFF
                val red = (argbPixel ushr 16) and 0xFF

                val normalizedBlue = NormalizationTable.blue[blue]
                val normalizedGreen = NormalizationTable.green[green]
                val normalizedRed = NormalizationTable.red[red]

                destination[blueStartIndex + pixelIndex] = normalizedBlue
                destination[greenStartIndex + pixelIndex] = normalizedGreen
                destination[redStartIndex + pixelIndex] = normalizedRed
            }
        }

        private object NormalizationTable {
            val blue: FloatArray = createNormalizationTable(
                mean = NanoDetModelContract.mean[0],
                standardDeviation = NanoDetModelContract.standardDeviation[0]
            )
            val green: FloatArray = createNormalizationTable(
                mean = NanoDetModelContract.mean[1],
                standardDeviation = NanoDetModelContract.standardDeviation[1]
            )
            val red: FloatArray = createNormalizationTable(
                mean = NanoDetModelContract.mean[2],
                standardDeviation = NanoDetModelContract.standardDeviation[2]
            )

            private fun createNormalizationTable(
                mean: Float,
                standardDeviation: Float
            ): FloatArray {
                return FloatArray(256) { value ->
                    (value.toFloat() - mean) / standardDeviation
                }
            }
        }
    }
}
