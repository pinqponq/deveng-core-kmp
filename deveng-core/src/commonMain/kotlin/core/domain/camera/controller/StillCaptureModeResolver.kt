package core.domain.camera.controller

import core.domain.camera.enums.FlashMode
import core.domain.camera.enums.QualityPrioritization
import core.domain.camera.enums.StillCaptureMode

/**
 * Zero shutter lag keeps the saved frame at the moment of the tap, but it does not work with the
 * flash on or on auto, so those fall back to minimize latency.
 */
internal fun resolveStillCaptureMode(
    override: StillCaptureMode?,
    qualityPrioritization: QualityPrioritization,
    flashMode: FlashMode,
): StillCaptureMode = override ?: when (qualityPrioritization) {
    QualityPrioritization.QUALITY, QualityPrioritization.NONE -> StillCaptureMode.MAXIMIZE_QUALITY
    QualityPrioritization.SPEED, QualityPrioritization.BALANCED ->
        if (flashMode == FlashMode.OFF) StillCaptureMode.ZERO_SHUTTER_LAG else StillCaptureMode.MINIMIZE_LATENCY
}
