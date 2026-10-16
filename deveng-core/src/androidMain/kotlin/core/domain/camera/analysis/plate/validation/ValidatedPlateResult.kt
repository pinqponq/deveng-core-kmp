package core.domain.camera.analysis.plate.validation

import core.domain.camera.analysis.ocr.roi.CapturedCameraFrame

/**
 * Represents a valid plate read stored in the rolling validation window.
 */
internal data class ValidatedPlateResult(
    val text: String,
    val confidence: Float,
    val fullFrame: CapturedCameraFrame?
)