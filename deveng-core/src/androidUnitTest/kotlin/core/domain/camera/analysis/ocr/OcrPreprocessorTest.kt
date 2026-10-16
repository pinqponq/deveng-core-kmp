package core.domain.camera.analysis.ocr

import core.domain.camera.analysis.nanodet.model.PlateBoundingBox
import core.domain.camera.analysis.ocr.pipeline.OcrPreprocessor
import core.domain.camera.analysis.ocr.roi.OcrBboxPadding
import core.domain.camera.analysis.ocr.roi.SourceRoiReader
import java.nio.FloatBuffer
import kotlin.test.Test
import kotlin.test.assertEquals

class OcrPreprocessorTest {

    @Test
    fun rgbPixelsAreWrittenInBatchLastRgbOrderWithoutNormalization() {
        val preprocessor = OcrPreprocessor(
            inputWidth = 2,
            inputHeight = 1,
        )
        try {
            val destination = FloatBuffer.allocate(2 * 1 * 3)
            preprocessor.resizeRgbLinearInto(
                sourcePixels = intArrayOf(
                    rgb(red = 255, green = 0, blue = 0),
                    rgb(red = 0, green = 0, blue = 255),
                ),
                sourceWidth = 2,
                sourceHeight = 1,
                destination = destination,
            )

            assertEquals(255f, destination.get(0))
            assertEquals(0f, destination.get(1))
            assertEquals(0f, destination.get(2))
            assertEquals(0f, destination.get(3))
            assertEquals(0f, destination.get(4))
            assertEquals(255f, destination.get(5))
        } finally {
            preprocessor.close()
        }
    }

    @Test
    fun onePixelRoiIsExpandedByLinearResizeWithoutChangingItsColor() {
        val preprocessor = OcrPreprocessor(
            inputWidth = 2,
            inputHeight = 2,
        )
        try {
            val destination = FloatBuffer.allocate(2 * 2 * 3)
            preprocessor.resizeRgbLinearInto(
                sourcePixels = intArrayOf(rgb(red = 12, green = 34, blue = 56)),
                sourceWidth = 1,
                sourceHeight = 1,
                destination = destination,
            )

            repeat(4) { pixel ->
                val offset = pixel * 3
                assertEquals(12f, destination.get(offset))
                assertEquals(34f, destination.get(offset + 1))
                assertEquals(56f, destination.get(offset + 2))
            }
        } finally {
            preprocessor.close()
        }
    }

    @Test
    fun resolvedInputDimensionsDriveTheOutputBufferSize() {
        val preprocessor = OcrPreprocessor(
            inputWidth = 2,
            inputHeight = 1,
        )
        try {
            val destination = FloatBuffer.allocate(2 * 1 * 3)
            preprocessor.resizeRgbLinearInto(
                sourcePixels = intArrayOf(rgb(red = 12, green = 34, blue = 56)),
                sourceWidth = 1,
                sourceHeight = 1,
                destination = destination,
            )

            assertEquals(2 * 1 * 3, destination.capacity())
            assertEquals(12f, destination.get(0))
            assertEquals(34f, destination.get(1))
            assertEquals(56f, destination.get(2))
        } finally {
            preprocessor.close()
        }
    }

    private fun rgb(red: Int, green: Int, blue: Int): Int =
        (red shl 16) or (green shl 8) or blue
}
