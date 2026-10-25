package core.presentation.motion

import androidx.compose.ui.unit.LayoutDirection
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SwipeToReplyLogicTest {

    @Test
    fun isSwipeToReplyStart_withSidewaysMoveMoreThanThreeTimesVertical_isTrue() {
        assertTrue(isSwipeToReplyStart(directionalDistance = 31f, verticalDistance = 10f))
    }

    @Test
    fun isSwipeToReplyStart_withSidewaysMoveExactlyThreeTimesVertical_isFalse() {
        assertFalse(isSwipeToReplyStart(directionalDistance = 30f, verticalDistance = 10f))
    }

    @Test
    fun isSwipeToReplyStart_withUpwardMove_comparesAgainstItsLength() {
        assertFalse(isSwipeToReplyStart(directionalDistance = 30f, verticalDistance = -10f))
        assertTrue(isSwipeToReplyStart(directionalDistance = 31f, verticalDistance = -10f))
    }

    @Test
    fun isSwipeToReplyStart_withMoveAgainstTheDirection_isFalse() {
        assertFalse(isSwipeToReplyStart(directionalDistance = -40f, verticalDistance = 0f))
    }

    @Test
    fun isSwipeToReplyStart_withoutAnyMove_isFalse() {
        assertFalse(isSwipeToReplyStart(directionalDistance = 0f, verticalDistance = 0f))
    }

    @Test
    fun isTriggerReached_atTriggerDistance_isTrue() {
        assertTrue(isTriggerReached(offset = TRIGGER_DISTANCE, triggerDistance = TRIGGER_DISTANCE))
    }

    @Test
    fun isTriggerReached_belowTriggerDistance_isFalse() {
        assertFalse(isTriggerReached(offset = TRIGGER_DISTANCE - 0.1f, triggerDistance = TRIGGER_DISTANCE))
    }

    @Test
    fun swipeToReplyOffset_belowTrigger_followsTheFinger() {
        assertEquals(40f, offsetAfter(currentOffset = 10f, dragDelta = 30f))
    }

    @Test
    fun swipeToReplyOffset_movingBack_followsTheFingerAndStopsAtRest() {
        assertEquals(70f, offsetAfter(currentOffset = 80f, dragDelta = -10f))
        assertEquals(0f, offsetAfter(currentOffset = 10f, dragDelta = -30f))
    }

    @Test
    fun swipeToReplyOffset_crossingTrigger_movesFreelyUpToTheTriggerAndResistsTheRest() {
        assertEquals(TRIGGER_DISTANCE + 10f, offsetAfter(currentOffset = 60f, dragDelta = 14f))
    }

    @Test
    fun swipeToReplyOffset_pastTrigger_appliesResistanceExponent() {
        val halfwayOffset = TRIGGER_DISTANCE + (MAX_DISTANCE - TRIGGER_DISTANCE) / 2f

        val offset = offsetAfter(currentOffset = halfwayOffset, dragDelta = 1f)

        assertEquals(halfwayOffset + HALFWAY_RESISTANCE, offset, absoluteTolerance = TOLERANCE)
    }

    @Test
    fun swipeToReplyOffset_pastMaximum_staysAtMaximum() {
        assertEquals(MAX_DISTANCE, offsetAfter(currentOffset = 95f, dragDelta = 500f))
        assertEquals(MAX_DISTANCE, offsetAfter(currentOffset = MAX_DISTANCE, dragDelta = 10f))
    }

    @Test
    fun swipeToReplyOffset_withManySmallMoves_neverPassesMaximum() {
        var offset = 0f
        repeat(1_000) { offset = offsetAfter(currentOffset = offset, dragDelta = 2f) }

        assertTrue(offset <= MAX_DISTANCE)
        assertTrue(offset > TRIGGER_DISTANCE)
    }

    @Test
    fun swipeToReplyDirectionSign_startToEnd_pointsTowardsTheEndEdge() {
        assertEquals(1f, swipeToReplyDirectionSign(SwipeToReplyDirection.StartToEnd, LayoutDirection.Ltr))
        assertEquals(-1f, swipeToReplyDirectionSign(SwipeToReplyDirection.StartToEnd, LayoutDirection.Rtl))
    }

    @Test
    fun swipeToReplyDirectionSign_endToStart_pointsTowardsTheStartEdge() {
        assertEquals(-1f, swipeToReplyDirectionSign(SwipeToReplyDirection.EndToStart, LayoutDirection.Ltr))
        assertEquals(1f, swipeToReplyDirectionSign(SwipeToReplyDirection.EndToStart, LayoutDirection.Rtl))
    }

    private fun offsetAfter(currentOffset: Float, dragDelta: Float): Float = swipeToReplyOffset(
        currentOffset = currentOffset,
        dragDelta = dragDelta,
        triggerDistance = TRIGGER_DISTANCE,
        maxDistance = MAX_DISTANCE,
        resistanceExponent = RESISTANCE_EXPONENT
    )

    private companion object {
        const val TRIGGER_DISTANCE = 64f
        const val MAX_DISTANCE = 96f
        const val RESISTANCE_EXPONENT = 1.5f
        const val HALFWAY_RESISTANCE = 0.35355339f
        const val TOLERANCE = 0.0001f
    }
}
