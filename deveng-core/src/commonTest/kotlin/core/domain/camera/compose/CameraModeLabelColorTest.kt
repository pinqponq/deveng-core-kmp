package core.domain.camera.compose

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals

class CameraModeLabelColorTest {

    @Test
    fun cameraModeLabelColor_whenSelected_isOpaqueWhite() {
        assertEquals(Color.White, cameraModeLabelColor(isSelected = true))
    }

    @Test
    fun cameraModeLabelColor_whenNotSelected_isWhiteAt70Percent() {
        assertEquals(Color.White.copy(alpha = 0.7f), cameraModeLabelColor(isSelected = false))
    }
}
