package core.domain.camera.analysis.plate.validation

import core.domain.camera.analysis.nanodet.model.PlateBoundingBox
import core.domain.camera.analysis.nanodet.model.PlateDetectionResult

/** Filters mapped detector results before any OCR inference is started. */
internal class PlateQualityGate(
    private val detectionConfidenceThreshold: Float,
    private val minimumBoundingBoxWidthPx: Float,
    private val minimumBoundingBoxHeightPx: Float
) {
    fun selectAcceptableDetection(
        detection: PlateDetectionResult?,
        frameWidth: Int,
        frameHeight: Int
    ): PlateDetectionResult? {
        if (detection == null || frameWidth <= 0 || frameHeight <= 0) return null
        return if (isAcceptable(detection, frameWidth, frameHeight)) detection else null
    }

    private fun isAcceptable(
        detection: PlateDetectionResult,
        frameWidth: Int,
        frameHeight: Int
    ): Boolean {
        val boundingBox = detection.boundingBox

        if (!isConfidenceValid(detection.confidence) || !isBoundingBoxValid(boundingBox, frameWidth, frameHeight)) {
            return false
        }

        return isBoxSizeValid(boundingBox)
    }

    private fun isBoxSizeValid(box: PlateBoundingBox): Boolean {
        val width = box.right - box.left + 1f
        val height = box.bottom - box.top + 1f
        return width >= minimumBoundingBoxWidthPx &&
            height >= minimumBoundingBoxHeightPx
    }

    private fun isConfidenceValid(confidence: Float): Boolean {
        return confidence.isFinite() && confidence >= detectionConfidenceThreshold
    }

    private fun isBoundingBoxValid(
        box: PlateBoundingBox,
        frameWidth: Int,
        frameHeight: Int
    ): Boolean {
        val isBoxValid = box.left.isFinite() &&
            box.top.isFinite() &&
            box.right.isFinite() &&
            box.bottom.isFinite() &&
            box.left <= box.right &&
            box.top <= box.bottom

        if (!isBoxValid) return false

        val maxX = (frameWidth - 1).toFloat()
        val maxY = (frameHeight - 1).toFloat()

        return box.left >= 0f &&
            box.top >= 0f &&
            box.right <= maxX &&
            box.bottom <= maxY
    }
}