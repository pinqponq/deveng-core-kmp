package core.domain.camera.compose

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals

class CameraModeLabelStyleTest {

    @Test
    fun cameraModeLabelColor_whenSelected_isOpaqueWhite() {
        assertEquals(Color.White, cameraModeLabelColor(isSelected = true))
    }

    @Test
    fun cameraModeLabelColor_whenNotSelected_isWhiteAt70Percent() {
        assertEquals(Color.White.copy(alpha = 0.7f), cameraModeLabelColor(isSelected = false))
    }

    @Test
    fun cameraModeLabelShadow_onAnyMode_isSemiTransparentBlackBlur() {
        assertEquals(Color.Black.copy(alpha = 0.6f), CameraModeLabelShadow.color)
        assertEquals(6f, CameraModeLabelShadow.blurRadius)
    }
}
