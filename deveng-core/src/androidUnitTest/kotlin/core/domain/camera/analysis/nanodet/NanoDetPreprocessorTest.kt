package core.domain.camera.analysis.nanodet

import core.domain.camera.analysis.nanodet.model.NanoDetModelContract
import core.domain.camera.analysis.nanodet.pipeline.NanoDetPreprocessor
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.test.Test
import kotlin.test.assertEquals

class NanoDetPreprocessorTest {
    @Test
    fun argbPixelsAreConvertedToNormalizedBgrChwInOnePass() {
        val pixels = intArrayOf(
            0xFFFF0000.toInt(),
            0xFF00FF00.toInt(),
            0xFF0000FF.toInt(),
            0xFF0C2238.toInt(),
        )

        val tensor = NanoDetPreprocessor.argbToNormalizedBgrChw(pixels, width = 2, height = 2)

        assertChannel(tensor, channel = 0, expectedRgb = listOf(0f, 0f, 255f, 56f))
        assertChannel(tensor, channel = 1, expectedRgb = listOf(0f, 255f, 0f, 34f))
        assertChannel(tensor, channel = 2, expectedRgb = listOf(255f, 0f, 0f, 12f))
    }

    @Test
    fun optimizedArgbNormalizationFillsDirectBufferAndResetsPosition() {
        val pixels = intArrayOf(
            0xFFFF0000.toInt(),
            0xFF00FF00.toInt(),
            0xFF0000FF.toInt(),
            0xFF0C2238.toInt(),
        )

        val destination = ByteBuffer
            .allocateDirect(12 * Float.SIZE_BYTES)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
        destination.position(5)

        val scratch = FloatArray(12)
        NanoDetPreprocessor.argbToNormalizedBgrChw(
            pixels = pixels,
            width = 2,
            height = 2,
            destination = scratch,
        )
        destination.position(0)
        destination.put(scratch, 0, 12)
        destination.position(0)

        assertEquals(0, destination.position())
        val tensor = FloatArray(destination.capacity()) { destination.get(it) }
        assertChannel(tensor, channel = 0, expectedRgb = listOf(0f, 0f, 255f, 56f))
        assertChannel(tensor, channel = 1, expectedRgb = listOf(0f, 255f, 0f, 34f))
        assertChannel(tensor, channel = 2, expectedRgb = listOf(255f, 0f, 0f, 12f))
    }

    private fun assertChannel(tensor: FloatArray, channel: Int, expectedRgb: List<Float>) {
        val planeSize = expectedRgb.size
        expectedRgb.forEachIndexed { index, rgb ->
            val expected = (rgb - NanoDetModelContract.mean[channel]) /
                NanoDetModelContract.standardDeviation[channel]
            assertEquals(expected, tensor[channel * planeSize + index], absoluteTolerance = 0.0001f)
        }
    }
}
