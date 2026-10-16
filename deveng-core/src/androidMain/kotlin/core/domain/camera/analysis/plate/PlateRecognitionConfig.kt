package core.domain.camera.analysis.plate

import core.domain.camera.analysis.nanodet.NanoDetConfig
import core.domain.camera.analysis.ocr.FastPlateOcrConfig
import core.domain.camera.analysis.plate.manager.ModelSessionReleasePolicy

/**
 * Public configuration for the first deterministic end-to-end pipeline.
 */
data class PlateRecognitionConfig(
    val detectorConfig: NanoDetConfig = NanoDetConfig(),
    val ocrConfig: FastPlateOcrConfig = FastPlateOcrConfig(),
    val minimumBoundingBoxWidthPx: Float = 32f,
    val minimumBoundingBoxHeightPx: Float = 8f,
    val bboxPaddingHorizontalRatio: Float = 0f,
    val bboxPaddingVerticalRatio: Float = 0f,
    val requiredMatchingReads: Int = 2,
    val validationWindowSize: Int = 3,
    val lockAfterSuccessfulRead: Boolean = true,
    val modelSessionReleasePolicy: ModelSessionReleasePolicy = ModelSessionReleasePolicy.MANUAL
) {
    init {
        require(lockAfterSuccessfulRead || modelSessionReleasePolicy != ModelSessionReleasePolicy.AFTER_SUCCESS) {
            "Automatic session release requires lockAfterSuccessfulRead = true."
        }
    }
}