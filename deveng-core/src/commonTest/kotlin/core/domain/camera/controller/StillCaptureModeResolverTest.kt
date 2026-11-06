package core.domain.camera.controller

import core.domain.camera.enums.FlashMode
import core.domain.camera.enums.QualityPrioritization
import core.domain.camera.enums.StillCaptureMode
import kotlin.test.Test
import kotlin.test.assertEquals

class StillCaptureModeResolverTest {

    @Test
    fun speedWithFlashOffUsesZeroShutterLag() {
        assertEquals(
            StillCaptureMode.ZERO_SHUTTER_LAG,
            resolveStillCaptureMode(null, QualityPrioritization.SPEED, FlashMode.OFF),
        )
        assertEquals(
            StillCaptureMode.ZERO_SHUTTER_LAG,
            resolveStillCaptureMode(null, QualityPrioritization.BALANCED, FlashMode.OFF),
        )
    }

    @Test
    fun speedWithFlashOnOrAutoUsesMinimizeLatency() {
        assertEquals(
            StillCaptureMode.MINIMIZE_LATENCY,
            resolveStillCaptureMode(null, QualityPrioritization.SPEED, FlashMode.ON),
        )
        assertEquals(
            StillCaptureMode.MINIMIZE_LATENCY,
            resolveStillCaptureMode(null, QualityPrioritization.BALANCED, FlashMode.AUTO),
        )
    }

    @Test
    fun qualityAndNoneUseMaximizeQuality() {
        assertEquals(
            StillCaptureMode.MAXIMIZE_QUALITY,
            resolveStillCaptureMode(null, QualityPrioritization.QUALITY, FlashMode.OFF),
        )
        assertEquals(
            StillCaptureMode.MAXIMIZE_QUALITY,
            resolveStillCaptureMode(null, QualityPrioritization.NONE, FlashMode.OFF),
        )
    }

    @Test
    fun overrideWinsOverQualityPrioritization() {
        assertEquals(
            StillCaptureMode.MINIMIZE_LATENCY,
            resolveStillCaptureMode(StillCaptureMode.MINIMIZE_LATENCY, QualityPrioritization.SPEED, FlashMode.OFF),
        )
        assertEquals(
            StillCaptureMode.ZERO_SHUTTER_LAG,
            resolveStillCaptureMode(StillCaptureMode.ZERO_SHUTTER_LAG, QualityPrioritization.QUALITY, FlashMode.ON),
        )
    }
}
