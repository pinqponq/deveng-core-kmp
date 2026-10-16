package core.presentation.motion

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class NavTransitionsTest {

    @Test
    fun transitions_withReduceMotion_areNone() {
        assertEquals(EnterTransition.None, NavTransitions.enter(isReduceMotionEnabled = true))
        assertEquals(ExitTransition.None, NavTransitions.exit(isReduceMotionEnabled = true))
        assertEquals(EnterTransition.None, NavTransitions.popEnter(isReduceMotionEnabled = true))
        assertEquals(ExitTransition.None, NavTransitions.popExit(isReduceMotionEnabled = true))
    }

    @Test
    fun transitions_withoutReduceMotion_animate() {
        assertNotEquals(EnterTransition.None, NavTransitions.enter(isReduceMotionEnabled = false))
        assertNotEquals(ExitTransition.None, NavTransitions.exit(isReduceMotionEnabled = false))
        assertNotEquals(EnterTransition.None, NavTransitions.popEnter(isReduceMotionEnabled = false))
        assertNotEquals(ExitTransition.None, NavTransitions.popExit(isReduceMotionEnabled = false))
    }

    @Test
    fun navigationSlideOffset_returnsQuarterOfWidth() {
        assertEquals(250, navigationSlideOffset(fullWidth = 1000))
    }
}
