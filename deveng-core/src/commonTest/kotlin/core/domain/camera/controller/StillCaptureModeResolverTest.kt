package core.domain.camera.controller

import core.domain.camera.enums.FlashMode
import core.domain.camera.enums.QualityPrioritization
import core.domain.camera.enums.StillCaptureMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StillCaptureModeResolverTest {

    @Test
    fun resolveStillCaptureMode_withSpeedOrBalancedAndFlashOff_returnsZeroShutterLag() {
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
    fun resolveStillCaptureMode_withSpeedOrBalancedAndFlashOnOrAuto_returnsMinimizeLatency() {
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
    fun resolveStillCaptureMode_withQualityOrNone_returnsMaximizeQuality() {
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
    fun resolveStillCaptureMode_withOverride_returnsTheOverride() {
        assertEquals(
            StillCaptureMode.MINIMIZE_LATENCY,
            resolveStillCaptureMode(StillCaptureMode.MINIMIZE_LATENCY, QualityPrioritization.SPEED, FlashMode.OFF),
        )
        assertEquals(
            StillCaptureMode.ZERO_SHUTTER_LAG,
            resolveStillCaptureMode(StillCaptureMode.ZERO_SHUTTER_LAG, QualityPrioritization.QUALITY, FlashMode.ON),
        )
    }

    @Test
    fun needsStillCaptureRebind_withTheSameMode_returnsFalse() {
        assertFalse(needsStillCaptureRebind(StillCaptureMode.MINIMIZE_LATENCY, StillCaptureMode.MINIMIZE_LATENCY))
        assertFalse(needsStillCaptureRebind(current = null, requested = null))
    }

    @Test
    fun needsStillCaptureRebind_withAnotherModeOrNull_returnsTrue() {
        assertTrue(needsStillCaptureRebind(StillCaptureMode.ZERO_SHUTTER_LAG, StillCaptureMode.MINIMIZE_LATENCY))
        assertTrue(needsStillCaptureRebind(current = null, requested = StillCaptureMode.MAXIMIZE_QUALITY))
        assertTrue(needsStillCaptureRebind(current = StillCaptureMode.MAXIMIZE_QUALITY, requested = null))
    }
}
