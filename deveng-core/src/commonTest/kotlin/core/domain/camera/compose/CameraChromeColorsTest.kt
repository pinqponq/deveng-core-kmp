package core.domain.camera.compose

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals

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
}
