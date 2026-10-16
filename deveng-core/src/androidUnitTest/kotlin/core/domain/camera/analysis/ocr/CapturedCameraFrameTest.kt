package core.domain.camera.analysis.ocr

import android.graphics.PixelFormat
import android.graphics.Rect
import androidx.camera.core.ImageInfo
import androidx.camera.core.ImageProxy
import core.domain.camera.analysis.AndroidCameraFrame
import core.domain.camera.analysis.ocr.roi.CapturedCameraFrame
import core.domain.camera.analysis.ocr.roi.FullFrameCapture
import core.domain.camera.analysis.ocr.roi.packedRgbaToArgbPixels
import java.lang.reflect.Proxy
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class CapturedCameraFrameTest {

    @Test
    fun fullFrameCaptureWithContiguousStrideUsesFastPath() {
        val frame = rgbaFrame(
            width = 2,
            height = 2,
            rowPaddingBytes = 0, // plane.rowStride == packedRowBytes
            pixels = intArrayOf(
                rgb(10, 20, 30), rgb(40, 50, 60),
                rgb(70, 80, 90), rgb(100, 110, 120),
            ),
            rotationDegrees = 0,
        )
        try {
            val captured = FullFrameCapture.capture(frame.frame)

            assertEquals(2, captured.sourceWidth)
            assertEquals(2, captured.sourceHeight)
            assertEquals(0, captured.rotationDegrees)
            assertFalse(captured.hasMaterializedBitmap)
            assertEquals(0L, captured.bitmapRenderNanos)

            assertContentEquals(
                byteArrayOf(
                    10, 20, 30, -1, 40, 50, 60, -1,
                    70, 80, 90, -1, 100, 110, 120, -1,
                ),
                captured.packedRgba,
            )
        } finally {
            frame.frame.releaseAfterRead()
        }
    }

    @Test
    fun fullFrameCaptureWithPaddedStrideCorrectlySkipsPadding() {
        val frame = rgbaFrame(
            width = 2,
            height = 2,
            rowPaddingBytes = 8, // plane.rowStride > packedRowBytes
            pixels = intArrayOf(
                rgb(1, 2, 3), rgb(4, 5, 6),
                rgb(7, 8, 9), rgb(10, 11, 12),
            ),
            rotationDegrees = 90,
        )
        try {
            val captured = FullFrameCapture.capture(frame.frame)

            assertEquals(2, captured.sourceWidth)
            assertEquals(2, captured.sourceHeight)
            assertEquals(90, captured.rotationDegrees)
            assertFalse(captured.hasMaterializedBitmap)
            assertEquals(0L, captured.bitmapRenderNanos)

            assertContentEquals(
                byteArrayOf(
                    1, 2, 3, -1, 4, 5, 6, -1,
                    7, 8, 9, -1, 10, 11, 12, -1,
                ),
                captured.packedRgba,
            )
        } finally {
            frame.frame.releaseAfterRead()
        }
    }

    @Test
    fun capturedFrameDoesNotMaterializeBitmapEagerly() {
        val packedRgba = byteArrayOf(
            255.toByte(), 0, 0, 255.toByte(), // Red
            0, 255.toByte(), 0, 255.toByte(), // Green
        )
        val captured = CapturedCameraFrame(
            packedRgba = packedRgba,
            sourceWidth = 2,
            sourceHeight = 1,
            rotationDegrees = 0,
            timestampNanos = 12345L,
        )

        assertFalse(captured.hasMaterializedBitmap)
        assertEquals(0L, captured.bitmapRenderNanos)
        assertEquals(2, captured.sourceWidth)
        assertEquals(1, captured.sourceHeight)
        assertEquals(0, captured.rotationDegrees)
        assertEquals(12345L, captured.timestampNanos)
    }

    @Test
    fun packedRgbaToArgbPixelsCorrectlyConvertsRgbaBytesToArgbInts() {
        val packedRgba = byteArrayOf(
            255.toByte(), 0, 0, 255.toByte(), // Red: 0xFFFF0000.toInt()
            0, 255.toByte(), 0, 128.toByte(), // Green + alpha 128: (128 shl 24) or (255 shl 8)
            0, 0, 255.toByte(), 255.toByte(), // Blue: 0xFF0000FF.toInt()
            10, 20, 30, 40,                   // Custom RGBA
        )
        val pixels = packedRgbaToArgbPixels(packedRgba, width = 2, height = 2)

        assertEquals(4, pixels.size)
        // Red
        assertEquals((255 shl 24) or (255 shl 16) or (0 shl 8) or 0, pixels[0])
        // Green
        assertEquals((128 shl 24) or (0 shl 16) or (255 shl 8) or 0, pixels[1])
        // Blue
        assertEquals((255 shl 24) or (0 shl 16) or (0 shl 8) or 255, pixels[2])
        // Custom
        assertEquals((40 shl 24) or (10 shl 16) or (20 shl 8) or 30, pixels[3])
    }

    private fun rgbaFrame(
        width: Int,
        height: Int,
        pixels: IntArray,
        rowPaddingBytes: Int = 0,
        rotationDegrees: Int = 0,
    ): RecordingRgbaFrame {
        val rowStride = width * BYTES_PER_PIXEL + rowPaddingBytes
        val bytes = ByteBuffer.allocate(rowStride * height)
        pixels.forEachIndexed { index, pixel ->
            val x = index % width
            val y = index / width
            val offset = y * rowStride + x * BYTES_PER_PIXEL
            bytes.put(offset, ((pixel ushr 16) and 0xFF).toByte())
            bytes.put(offset + 1, ((pixel ushr 8) and 0xFF).toByte())
            bytes.put(offset + 2, (pixel and 0xFF).toByte())
            bytes.put(offset + 3, 0xFF.toByte())
        }

        val plane = Proxy.newProxyInstance(
            ImageProxy.PlaneProxy::class.java.classLoader,
            arrayOf(ImageProxy.PlaneProxy::class.java),
        ) { _, method, _ ->
            when (method.name) {
                "getBuffer" -> bytes.duplicate()
                "getPixelStride" -> BYTES_PER_PIXEL
                "getRowStride" -> rowStride
                else -> null
            }
        } as ImageProxy.PlaneProxy
        val imageInfo = Proxy.newProxyInstance(
            ImageInfo::class.java.classLoader,
            arrayOf(ImageInfo::class.java),
        ) { _, method, _ ->
            when (method.name) {
                "getRotationDegrees" -> rotationDegrees
                "getTimestamp" -> 1L
                else -> null
            }
        } as ImageInfo
        val closeCount = AtomicInteger(0)
        val proxy = Proxy.newProxyInstance(
            ImageProxy::class.java.classLoader,
            arrayOf(ImageProxy::class.java),
        ) { _, method, _ ->
            when (method.name) {
                "close" -> {
                    closeCount.incrementAndGet()
                    null
                }
                "getFormat" -> PixelFormat.RGBA_8888
                "getWidth" -> width
                "getHeight" -> height
                "getPlanes" -> arrayOf(plane)
                "getCropRect" -> Rect(0, 0, width, height)
                "getImageInfo" -> imageInfo
                "getImage" -> null
                "toString" -> "RecordingRgbaImageProxy"
                else -> defaultValue(method.returnType)
            }
        } as ImageProxy
        return RecordingRgbaFrame(AndroidCameraFrame(proxy), closeCount)
    }

    private fun defaultValue(type: Class<*>): Any? = when (type) {
        Boolean::class.javaPrimitiveType -> false
        Byte::class.javaPrimitiveType -> 0.toByte()
        Short::class.javaPrimitiveType -> 0.toShort()
        Int::class.javaPrimitiveType -> 0
        Long::class.javaPrimitiveType -> 0L
        Float::class.javaPrimitiveType -> 0f
        Double::class.javaPrimitiveType -> 0.0
        Char::class.javaPrimitiveType -> '\u0000'
        else -> null
    }

    private fun rgb(red: Int, green: Int, blue: Int): Int =
        (red shl 16) or (green shl 8) or blue

    private data class RecordingRgbaFrame(
        val frame: AndroidCameraFrame,
        val closeCount: AtomicInteger,
    )

    private companion object {
        const val BYTES_PER_PIXEL = 4
    }
}
