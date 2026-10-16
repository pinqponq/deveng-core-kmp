package core.domain.camera.analysis.ocr

import core.domain.camera.analysis.ocr.model.FastPlateOcrModelContract
import core.domain.camera.analysis.ocr.pipeline.OcrDecoder
import java.nio.FloatBuffer
import kotlin.test.Test
import kotlin.test.assertEquals

class OcrDecoderTest {

    @Test
    fun slotArgmaxMapsCharactersAndTrimsTrailingPadding() {
        val output = FloatArray(FastPlateOcrModelContract.OUTPUT_VALUE_COUNT)
        // Slot 0 -> '3' (index 3 in ALPHABET)
        output[0 * FastPlateOcrModelContract.OUTPUT_CLASSES + 3] = 0.90f
        // Slot 1 -> '4' (index 4 in ALPHABET)
        output[1 * FastPlateOcrModelContract.OUTPUT_CLASSES + 4] = 0.95f
        // Slots 2..9 -> '_' (index 36 in ALPHABET)
        val padIndex = FastPlateOcrModelContract.ALPHABET.indexOf(FastPlateOcrModelContract.PAD_CHARACTER)
        for (slot in 2 until FastPlateOcrModelContract.MAX_PLATE_SLOTS) {
            output[slot * FastPlateOcrModelContract.OUTPUT_CLASSES + padIndex] = 0.99f
        }

        val result = OcrDecoder.decode(FloatBuffer.wrap(output))

        assertEquals("34", result.plateText)
        assertEquals(0.90f, result.characterConfidences[0])
        assertEquals(0.95f, result.characterConfidences[1])
        assertEquals(10, result.characterConfidences.size)
    }
}
