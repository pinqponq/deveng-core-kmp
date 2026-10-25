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
        val hasCrossedAgain = state.dragBy(10f)

        assertTrue(hasCrossedTrigger)
        assertFalse(hasCrossedAgain)
        assertTrue(state.isPastTrigger)
        assertEquals(1f, state.triggerProgress)
    }

    @Test
    fun dragBy_backBelowTriggerAndPastAgain_reportsSecondCrossing() {
        val state = createState()
        state.dragBy(TRIGGER_DISTANCE_PX + 5f)

        state.dragBy(-20f)
        val hasCrossedAgain = state.dragBy(20f)

        assertTrue(hasCrossedAgain)
    }

    @Test
    fun offset_pastMaximum_staysAtMaximum() {
        val state = createState()

        state.dragBy(1_000f)

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
    }
}
