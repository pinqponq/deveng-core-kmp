package core.domain.camera.analysis.nanodet

import android.graphics.PixelFormat
import android.graphics.Rect
import androidx.camera.core.ImageInfo
import androidx.camera.core.ImageProxy
import androidx.test.ext.junit.runners.AndroidJUnit4
import core.domain.camera.analysis.AndroidCameraFrame
import java.lang.reflect.Proxy
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NanoDetRgbaNativePreprocessorInstrumentedTest {
    @Test
    fun testRgbaByteOrderProducesBgrChwTensor() {
        val frame = rgbaFrame(
            width = 1,
            height = 1,
            rotationDegrees = 0,
            packedRgba = byteArrayOf(12, 34, 56, 255.toByte()),
            rowPaddingBytes = 0,
        )

        val tensor = preprocess(frame)
        val planeSize = NanoDetModelContract.INPUT_SIZE * NanoDetModelContract.INPUT_SIZE

        assertEquals(normalize(56f, channel = 0), tensor[0], 0.0001f)
        assertEquals(normalize(34f, channel = 1), tensor[planeSize], 0.0001f)
        assertEquals(normalize(12f, channel = 2), tensor[2 * planeSize], 0.0001f)
    }

    private fun preprocess(frame: AndroidCameraFrame): FloatArray {
        val output = ByteBuffer
            .allocateDirect(NanoDetModelContract.INPUT_VALUE_COUNT * Float.SIZE_BYTES)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
        NanoDetPreprocessor().use { preprocessor ->
            preprocessor.preprocessInto(
                frame = frame,
                destination = output,
                includePreview = false,
            )
        }
        return FloatArray(output.capacity()) { output.get(it) }
    }

    private fun rgbaFrame(
        width: Int,
        height: Int,
        rotationDegrees: Int,
        packedRgba: ByteArray,
        rowPaddingBytes: Int,
    ): AndroidCameraFrame {
        val packedRowBytes = width * 4
        val rowStride = packedRowBytes + rowPaddingBytes
        val buffer = ByteBuffer.allocateDirect(rowStride * height)
        repeat(height) { row ->
            val sourceOffset = row * packedRowBytes
            buffer.put(packedRgba, sourceOffset, packedRowBytes)
            repeat(rowPaddingBytes) { buffer.put(0x5A.toByte()) }
        }
        buffer.flip()
        val plane = proxy<ImageProxy.PlaneProxy> { method ->
            when (method.name) {
                "getBuffer" -> buffer.duplicate()
                "getRowStride" -> rowStride
                "getPixelStride" -> 4
                else -> defaultValue(method.returnType)
            }
        }
        val imageInfo = proxy<ImageInfo> { method ->
            when (method.name) {
                "getRotationDegrees" -> rotationDegrees
                "getTimestamp" -> 1L
                else -> defaultValue(method.returnType)
            }
        }
        val imageProxy = proxy<ImageProxy> { method ->
            when (method.name) {
                "getFormat" -> PixelFormat.RGBA_8888
                "getWidth" -> width
                "getHeight" -> height
                "getCropRect" -> Rect(0, 0, width, height)
                "getPlanes" -> arrayOf(plane)
                "getImageInfo" -> imageInfo
                "getImage" -> null
                "close" -> null
                else -> defaultValue(method.returnType)
            }
        }
        return AndroidCameraFrame(imageProxy)
    }

    private inline fun <reified T> proxy(crossinline answer: (java.lang.reflect.Method) -> Any?): T =
        Proxy.newProxyInstance(
            T::class.java.classLoader,
            arrayOf(T::class.java),
        ) { _, method, _ -> answer(method) } as T

    private fun normalize(value: Float, channel: Int): Float =
        (value - NanoDetModelContract.mean[channel]) /
            NanoDetModelContract.standardDeviation[channel]

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
}
