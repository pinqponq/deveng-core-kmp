package core.presentation.motion

import androidx.compose.ui.unit.LayoutDirection
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SwipeToReplyLogicTest {

    @Test
    fun isSwipeToReplyStart_withSidewaysMoveMoreThanThreeTimesVertical_isTrue() {
        assertTrue(isSwipeToReplyStart(directionalDistance = JUST_PAST_GATE_DISTANCE, verticalDistance = VERTICAL_DISTANCE))
    }

    @Test
    fun isSwipeToReplyStart_withSidewaysMoveExactlyThreeTimesVertical_isFalse() {
        assertFalse(isSwipeToReplyStart(directionalDistance = GATE_DISTANCE, verticalDistance = VERTICAL_DISTANCE))
    }

    @Test
    fun isSwipeToReplyStart_withUpwardMove_comparesAgainstItsLength() {
        assertFalse(isSwipeToReplyStart(directionalDistance = GATE_DISTANCE, verticalDistance = -VERTICAL_DISTANCE))
        assertTrue(isSwipeToReplyStart(directionalDistance = JUST_PAST_GATE_DISTANCE, verticalDistance = -VERTICAL_DISTANCE))
    }

    @Test
    fun isSwipeToReplyStart_withMoveAgainstTheDirection_isFalse() {
        assertFalse(isSwipeToReplyStart(directionalDistance = BACKWARD_DISTANCE, verticalDistance = 0f))
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
        assertEquals(SMALL_OFFSET + SMALL_DRAG, offsetAfter(currentOffset = SMALL_OFFSET, dragDelta = SMALL_DRAG))
    }

    @Test
    fun swipeToReplyOffset_movingBack_followsTheFinger() {
        assertEquals(PAST_TRIGGER_OFFSET - SMALL_DRAG, offsetAfter(currentOffset = PAST_TRIGGER_OFFSET, dragDelta = -SMALL_DRAG))
    }

    @Test
    fun swipeToReplyOffset_movingBackPastRest_stopsAtRest() {
        assertEquals(0f, offsetAfter(currentOffset = SMALL_OFFSET, dragDelta = -(SMALL_OFFSET + SMALL_DRAG)))
    }

    @Test
    fun swipeToReplyOffset_crossingTrigger_movesFreelyUpToTheTriggerAndResistsTheRest() {
        val dragDelta = TRIGGER_DISTANCE - SMALL_OFFSET + SMALL_DRAG

        assertEquals(TRIGGER_DISTANCE + SMALL_DRAG, offsetAfter(currentOffset = SMALL_OFFSET, dragDelta = dragDelta))
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
        assertEquals(MAX_DISTANCE, offsetAfter(currentOffset = MAX_DISTANCE, dragDelta = SMALL_DRAG))
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
        const val VERTICAL_DISTANCE = 10f
        const val SMALL_OFFSET = 10f
        const val SMALL_DRAG = 30f
        const val PAST_TRIGGER_OFFSET = 80f
        const val GATE_DISTANCE = 30f
        const val JUST_PAST_GATE_DISTANCE = 31f
        const val BACKWARD_DISTANCE = -40f
        const val TRIGGER_DISTANCE = 64f
        const val MAX_DISTANCE = 96f
        const val RESISTANCE_EXPONENT = 1.5f
        const val HALFWAY_RESISTANCE = 0.35355339f
        const val TOLERANCE = 0.0001f
    }
}
