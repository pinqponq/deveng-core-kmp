package core.domain.camera.analysis.ocr

import android.graphics.PixelFormat
import android.graphics.Rect
import androidx.camera.core.ImageInfo
import androidx.camera.core.ImageProxy
import core.domain.camera.analysis.AndroidCameraFrame
import core.domain.camera.analysis.nanodet.model.PlateBoundingBox
import core.domain.camera.analysis.ocr.roi.FullFrameCapture
import core.domain.camera.analysis.ocr.roi.OcrBboxPadding
import core.domain.camera.analysis.ocr.roi.SourceRoiBounds
import core.domain.camera.analysis.ocr.roi.SourceRoiReader
import java.lang.reflect.Proxy
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class SourceRoiReaderTest {
    @Test
    fun fullFrameCaptureCopiesPackedRowsWithoutCreatingBitmap() {
        val frame = rgbaFrame(
            width = 2,
            height = 2,
            rowPaddingBytes = 8,
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
            assertContentEquals(
                byteArrayOf(
                    1, 2, 3, -1, 4, 5, 6, -1,
                    7, 8, 9, -1, 10, 11, 12, -1,
                ),
                captured.packedRgba,
            )
            assertFalse(captured.hasMaterializedBitmap)
        } finally {
            frame.frame.releaseAfterRead()
        }
    }

    @Test
    fun readerUsesSourceCoordinatesAndHonorsRowStride() {
        val frame = rgbaFrame(
            width = 3,
            height = 2,
            rowPaddingBytes = 4,
            pixels = intArrayOf(
                rgb(255, 0, 0), rgb(0, 255, 0), rgb(0, 0, 255),
                rgb(255, 255, 0), rgb(0, 255, 255), rgb(255, 0, 255),
            ),
        )
        val reader = SourceRoiReader()
        try {
            val roi = reader.read(
                frame = frame.frame,
                boundingBox = PlateBoundingBox(left = 1f, top = 0f, right = 2f, bottom = 1f),
            )

            assertEquals(SourceRoiBounds(left = 1, top = 0, right = 2, bottom = 1), roi.bounds)
            assertContentEquals(
                intArrayOf(
                    rgb(0, 255, 0), rgb(0, 0, 255),
                    rgb(0, 255, 255), rgb(255, 0, 255),
                ),
                roi.pixels.copyOf(roi.bounds.width * roi.bounds.height),
            )
        } finally {
            reader.releaseBuffers()
            frame.frame.releaseAfterRead()
        }
        assertEquals(1, frame.closeCount.get())
    }

    @Test
    fun bboxPaddingIsClampedToFrameBounds() {
        val frame = rgbaFrame(
            width = 4,
            height = 2,
            pixels = IntArray(8) { rgb(1, 2, 3) },
        )
        try {
            val roi = SourceRoiReader().read(
                frame = frame.frame,
                boundingBox = PlateBoundingBox(left = 1f, top = 0f, right = 2f, bottom = 1f),
                padding = OcrBboxPadding(horizontalRatio = 1f, verticalRatio = 1f),
            )

            assertEquals(SourceRoiBounds(left = 0, top = 0, right = 3, bottom = 1), roi.bounds)
        } finally {
            frame.frame.releaseAfterRead()
        }
    }

    @Test
    fun readerOrientsRoiForAllSupportedRotations() {
        val sourcePixels = intArrayOf(
            rgb(1, 0, 0), rgb(2, 0, 0),
            rgb(3, 0, 0), rgb(4, 0, 0),
            rgb(5, 0, 0), rgb(6, 0, 0),
        )
        assertOrientedPixels(
            rotationDegrees = 90,
            expectedPixels = intArrayOf(
                rgb(5, 0, 0), rgb(3, 0, 0), rgb(1, 0, 0),
                rgb(6, 0, 0), rgb(4, 0, 0), rgb(2, 0, 0),
            ),
            sourcePixels = sourcePixels,
            expectedWidth = 3,
            expectedHeight = 2,
        )
        assertOrientedPixels(
            rotationDegrees = 180,
            expectedPixels = intArrayOf(
                rgb(6, 0, 0), rgb(5, 0, 0),
                rgb(4, 0, 0), rgb(3, 0, 0),
                rgb(2, 0, 0), rgb(1, 0, 0),
            ),
            sourcePixels = sourcePixels,
            expectedWidth = 2,
            expectedHeight = 3,
        )
        assertOrientedPixels(
            rotationDegrees = 270,
            expectedPixels = intArrayOf(
                rgb(2, 0, 0), rgb(4, 0, 0), rgb(6, 0, 0),
                rgb(1, 0, 0), rgb(3, 0, 0), rgb(5, 0, 0),
            ),
            sourcePixels = sourcePixels,
            expectedWidth = 3,
            expectedHeight = 2,
        )
    }

    private fun assertOrientedPixels(
        rotationDegrees: Int,
        expectedPixels: IntArray,
        sourcePixels: IntArray,
        expectedWidth: Int,
        expectedHeight: Int,
    ) {
        val frame = rgbaFrame(
            width = 2,
            height = 3,
            pixels = sourcePixels,
            rotationDegrees = rotationDegrees,
        )
        val reader = SourceRoiReader()
        try {
            val roi = reader.read(
                frame = frame.frame,
                boundingBox = PlateBoundingBox(left = 0f, top = 0f, right = 1f, bottom = 2f),
            )

            assertEquals(expectedWidth, roi.pixelWidth)
            assertEquals(expectedHeight, roi.pixelHeight)
            assertContentEquals(
                expectedPixels,
                roi.pixels.copyOf(expectedPixels.size),
            )
        } finally {
            reader.releaseBuffers()
            frame.frame.releaseAfterRead()
        }
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
