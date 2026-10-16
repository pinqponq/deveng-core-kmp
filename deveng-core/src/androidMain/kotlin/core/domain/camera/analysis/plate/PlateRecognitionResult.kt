package core.domain.camera.analysis.plate

import core.domain.camera.analysis.ocr.roi.CapturedCameraFrame

/**
 * Result emitted only after the configured OCR rolling-window consensus is reached.
 *
 * [confidence] is the average OCR confidence of the reads supporting the consensus.
 */
data class PlateRecognitionResult(
    val plate: String,
    val confidence: Float,
    /**
     * The display-oriented full camera frame from the strongest OCR read supporting this result.
     * The bitmap is detached from CameraX and remains usable after the callback returns.
     */
    val fullFrame: CapturedCameraFrame? = null,
)
