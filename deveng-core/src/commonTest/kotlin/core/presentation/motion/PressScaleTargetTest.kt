package core.presentation.motion

import kotlin.test.Test
import kotlin.test.assertEquals

class PressScaleTargetTest {

    @Test
    fun pressScaleTarget_whilePressed_returnsPressedScale() {
        val targetScale = pressScaleTarget(isPressed = true, isReduceMotionEnabled = false, pressedScale = PRESSED_SCALE)

        assertEquals(PRESSED_SCALE, targetScale)
    }

    @Test
    fun pressScaleTarget_whenReleased_returnsFullSize() {
        val targetScale = pressScaleTarget(isPressed = false, isReduceMotionEnabled = false, pressedScale = PRESSED_SCALE)

        assertEquals(FULL_SIZE_SCALE, targetScale)
    }

    @Test
    fun pressScaleTarget_whilePressedWithReduceMotion_returnsFullSize() {
        val targetScale = pressScaleTarget(isPressed = true, isReduceMotionEnabled = true, pressedScale = PRESSED_SCALE)

        assertEquals(FULL_SIZE_SCALE, targetScale)
    }

    private companion object {
        const val PRESSED_SCALE = 0.9f
        const val FULL_SIZE_SCALE = 1f
    }
}
