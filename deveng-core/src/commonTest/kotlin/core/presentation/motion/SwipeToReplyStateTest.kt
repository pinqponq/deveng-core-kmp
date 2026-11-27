package core.presentation.motion

import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SwipeToReplyStateTest {

    @Test
    fun dragBy_belowTrigger_reportsNoCrossingAndPartialProgress() {
        val state = createState()

        val hasCrossedTrigger = state.dragBy(TRIGGER_DISTANCE_PX / 2f)

        assertFalse(hasCrossedTrigger)
        assertFalse(state.isPastTrigger)
        assertEquals(0.5f, state.triggerProgress)
    }

    @Test
    fun dragBy_reachingTrigger_reportsCrossingOnceWithFullProgress() {
        val state = createState()

        val hasCrossedTrigger = state.dragBy(TRIGGER_DISTANCE_PX)
        val hasCrossedAgain = state.dragBy(SMALL_DRAG_PX)

        assertTrue(hasCrossedTrigger)
        assertFalse(hasCrossedAgain)
        assertTrue(state.isPastTrigger)
        assertEquals(1f, state.triggerProgress)
    }

    @Test
    fun dragBy_backBelowTriggerAndPastAgain_reportsSecondCrossing() {
        val state = createState()
        state.dragBy(TRIGGER_DISTANCE_PX + SMALL_DRAG_PX / 2f)

        state.dragBy(-SMALL_DRAG_PX)
        val hasCrossedAgain = state.dragBy(SMALL_DRAG_PX)

        assertTrue(hasCrossedAgain)
    }

    @Test
    fun offset_pastMaximum_staysAtMaximum() {
        val state = createState()

        state.dragBy(LARGE_DRAG_PX)

        assertEquals(MAX_DISTANCE_PX, state.offset)
    }

    private fun createState(): SwipeToReplyState = SwipeToReplyState(
        coroutineScope = CoroutineScope(Dispatchers.Unconfined),
        isReduceMotionEnabled = mutableStateOf(false),
        triggerDistancePx = TRIGGER_DISTANCE_PX,
        maxDistancePx = MAX_DISTANCE_PX,
        resistanceExponent = MotionDefaults.SWIPE_TO_REPLY_RESISTANCE_EXPONENT,
        returnMillis = MotionDefaults.SWIPE_TO_REPLY_RETURN_MILLIS
    )

    private companion object {
        const val TRIGGER_DISTANCE_PX = 64f
        const val MAX_DISTANCE_PX = 96f
        const val SMALL_DRAG_PX = 10f
        const val LARGE_DRAG_PX = 1_000f
    }
}
