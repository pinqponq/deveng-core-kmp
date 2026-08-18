package core.domain.camera.analysis.nanodet

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class NanoDetValidationAnalyzerTest {
    @Test
    fun normalizedBgrChwTensorIsConvertedToArgbInRgbOrder() {
        val inputSize = 2
        val planeSize = inputSize * inputSize
        val input = FloatArray(3 * planeSize)

        setNormalizedPixel(input, planeSize, index = 0, red = 255, green = 0, blue = 0)
        setNormalizedPixel(input, planeSize, index = 1, red = 0, green = 255, blue = 0)
        setNormalizedPixel(input, planeSize, index = 2, red = 0, green = 0, blue = 255)
        setNormalizedPixel(input, planeSize, index = 3, red = 12, green = 34, blue = 56)

        val pixels = normalizedBgrChwToArgb(input, inputSize)

        assertEquals(0xFFFF0000.toInt(), pixels[0])
        assertEquals(0xFF00FF00.toInt(), pixels[1])
        assertEquals(0xFF0000FF.toInt(), pixels[2])
        assertEquals(0xFF0C2238.toInt(), pixels[3])
    }

    @Test
    fun invalidTensorSizeIsRejected() {
        assertFailsWith<IllegalArgumentException> {
            normalizedBgrChwToArgb(FloatArray(3), inputSize = 2)
        }
    }

    @Test
    fun nanosecondsAreReportedWithoutIntegerMillisecondTruncation() {
        assertEquals(0.25, 250_000L.toMilliseconds())
    }

    private fun setNormalizedPixel(
        input: FloatArray,
        planeSize: Int,
        index: Int,
        red: Int,
        green: Int,
        blue: Int,
    ) {
        input[index] = normalize(blue, channel = 0)
        input[planeSize + index] = normalize(green, channel = 1)
        input[2 * planeSize + index] = normalize(red, channel = 2)
    }

    private fun normalize(value: Int, channel: Int): Float =
        (value - NanoDetModelContract.mean[channel]) /
            NanoDetModelContract.standardDeviation[channel]
}
