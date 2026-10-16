package core.domain.camera.analysis.plate.validation

import core.domain.camera.analysis.ocr.model.FastPlateOcrResult
import core.domain.camera.analysis.plate.PlateRecognitionResult
import java.util.ArrayDeque

/** Accepts a plate when it reaches the configured consensus in a rolling OCR window. */
internal class PlateResultValidator(
    private val requiredMatchingReads: Int,
    private val validationWindowSize: Int,
    private val confidenceThreshold: Float
) {
    private val validReads = ArrayDeque<ValidatedPlateResult>()
    private var isCompleted = false

    @Synchronized
    fun validate(ocrResult: FastPlateOcrResult): PlateRecognitionResult? {
        if (isCompleted) return null

        val plateText = ocrResult.plateText.trim()
        val confidence = ocrResult.confidence

        if (isInvalidRead(plateText, confidence)) return null

        if (validReads.size == validationWindowSize) {
            validReads.removeFirst()
        }

        validReads.addLast(
            ValidatedPlateResult(
                text = plateText,
                confidence = confidence,
                fullFrame = ocrResult.fullFrame,
            )
        )

        if (validReads.size < validationWindowSize) return null

        val consensusText = findConsensusPlateText() ?: return null
        val supportingReads = validReads.filter { it.text == consensusText }
        val bestSupportingRead = selectBestSupportingRead(supportingReads)

        isCompleted = true

        val result = PlateRecognitionResult(
            plate = consensusText,
            confidence = supportingReads.map { it.confidence }.average().toFloat(),
            fullFrame = bestSupportingRead?.fullFrame,
        )

        validReads.clear()

        return result
    }

    @Synchronized
    fun reset() {
        validReads.clear()
        isCompleted = false
    }

    private fun isInvalidRead(plateText: String, confidence: Float): Boolean {
        return plateText.isEmpty() || !confidence.isFinite() || confidence < confidenceThreshold
    }

    private fun findConsensusPlateText(): String? {
        return validReads.firstOrNull { candidate ->
                validReads.count { it.text == candidate.text } >= requiredMatchingReads
            }?.text
    }

    private fun selectBestSupportingRead(supportingReads: List<ValidatedPlateResult>): ValidatedPlateResult? {
        return supportingReads.maxWithOrNull(
            compareBy<ValidatedPlateResult> { it.confidence }
                .thenBy { it.fullFrame?.timestampNanos ?: Long.MIN_VALUE }
        )
    }
}
