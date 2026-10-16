package core.domain.camera.analysis.ocr.pipeline

import core.domain.camera.analysis.ocr.model.FastPlateOcrModelContract
import core.domain.camera.analysis.ocr.model.FastPlateOcrResult
import java.nio.FloatBuffer

/** Decodes fast-plate-ocr v5's slot-major output without a CTC step. */
internal object OcrDecoder {

    fun decode(probabilities: FloatBuffer): FastPlateOcrResult {
        val characterConfidences = FloatArray(FastPlateOcrModelContract.MAX_PLATE_SLOTS)
        val characters = CharArray(FastPlateOcrModelContract.MAX_PLATE_SLOTS)

        for (slot in 0 until FastPlateOcrModelContract.MAX_PLATE_SLOTS) {
            val offset = slot * FastPlateOcrModelContract.OUTPUT_CLASSES
            var bestIndex = 0
            var bestValue = probabilities.get(offset)

            for (characterIndex in 1 until FastPlateOcrModelContract.OUTPUT_CLASSES) {
                val value = probabilities.get(offset + characterIndex)

                if (value > bestValue) {
                    bestIndex = characterIndex
                    bestValue = value
                }
            }

            characters[slot] = FastPlateOcrModelContract.ALPHABET[bestIndex]
            characterConfidences[slot] = bestValue
        }

        return FastPlateOcrResult(
            plateText = String(characters).trimEnd(FastPlateOcrModelContract.PAD_CHARACTER),
            characterConfidences = characterConfidences.toList(),
            confidence = characterConfidences.average().toFloat()
        )
    }
}