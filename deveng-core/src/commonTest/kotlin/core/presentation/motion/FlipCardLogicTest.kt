package core.presentation.motion

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FlipCardLogicTest {

    @Test
    fun isFrontFaceVisible_pastHalfTurn_isTrue() {
        assertTrue(isFrontFaceVisible(rotationDegrees = 90.5f))
    }

    @Test
    fun isFrontFaceVisible_atHalfTurn_isFalse() {
        assertFalse(isFrontFaceVisible(rotationDegrees = 90f))
    }
}
