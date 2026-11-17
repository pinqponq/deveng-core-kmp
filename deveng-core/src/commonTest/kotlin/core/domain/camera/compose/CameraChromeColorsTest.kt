package core.domain.camera.compose

import androidx.compose.ui.graphics.Color
import core.domain.camera.enums.FlashMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CameraChromeColorsTest {

    @Test
    fun cameraChipContentColor_whenActive_isOpaqueWhite() {
        assertEquals(Color.White, cameraChipContentColor(isActive = true))
    }

    @Test
    fun cameraChipContentColor_whenInactive_isWhiteAt85Percent() {
        assertEquals(Color.White.copy(alpha = 0.85f), cameraChipContentColor(isActive = false))
    }

    @Test
    fun cameraChromeIconTint_whenOn_isOpaqueWhite() {
        assertEquals(Color.White, cameraChromeIconTint(isOn = true))
    }

    @Test
    fun cameraChromeIconTint_whenOff_isWhiteAt75Percent() {
        assertEquals(Color.White.copy(alpha = 0.75f), cameraChromeIconTint(isOn = false))
    }

    @Test
    fun isFlashIndicatorOn_isOffOnlyForFlashModeOff() {
        assertFalse(isFlashIndicatorOn(FlashMode.OFF))
        assertTrue(isFlashIndicatorOn(FlashMode.ON))
        assertTrue(isFlashIndicatorOn(FlashMode.AUTO))
    }

    @Test
    fun cameraChipPillAlpha_isDimmedOnlyWhenDisabled() {
        assertEquals(1f, cameraChipPillAlpha(enabled = true))
        assertEquals(0.45f, cameraChipPillAlpha(enabled = false))
    }

    @Test
    fun cameraChipHighlightColor_isTranslucentWhiteOnlyWhenActive() {
        assertEquals(Color.White.copy(alpha = 0.25f), cameraChipHighlightColor(isActive = true))
        assertEquals(Color.Transparent, cameraChipHighlightColor(isActive = false))
    }
}
