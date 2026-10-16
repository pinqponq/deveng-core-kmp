package core.domain.camera.analysis.ocr.model

import core.domain.camera.analysis.ocr.roi.CapturedCameraFrame

internal data class FastPlateOcrResult(
    val plateText: String,
    val characterConfidences: List<Float>,
    val confidence: Float,
    /** Detached raw copy of the complete CameraX frame used for this OCR read. */
    val fullFrame: CapturedCameraFrame? = null,
    val frameCopyNanos: Long = 0L,
    val preprocessNanos: Long = 0L,
    val inferenceNanos: Long = 0L
)