package core.presentation.motion

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CountUpTextLogicTest {

    @Test
    fun countUpValues_withSmallRise_stepsOneByOne() {
        assertEquals(listOf(1, 2, 3), countUpValues(startValue = 0, endValue = 3, maxStepCount = 12))
    }

    @Test
    fun countUpValues_withSmallDrop_stepsDownOneByOne() {
        assertEquals(listOf(4, 3, 2), countUpValues(startValue = 5, endValue = 2, maxStepCount = 12))
    }

    @Test
    fun countUpValues_withLargeRise_limitsStepCountAndEndsOnTarget() {
        val values = countUpValues(startValue = 0, endValue = 640, maxStepCount = 8)

        assertEquals(8, values.size)
        assertEquals(640, values.last())
        assertEquals(80, values.first())
    }

    @Test
    fun countUpValues_withSameStartAndEnd_hasNoSteps() {
        assertEquals(emptyList(), countUpValues(startValue = 7, endValue = 7, maxStepCount = 12))
    }

    @Test
    fun countUpValues_withExtremeRange_doesNotOverflow() {
        val values = countUpValues(startValue = Int.MIN_VALUE, endValue = Int.MAX_VALUE, maxStepCount = 4)

        assertEquals(4, values.size)
        assertEquals(Int.MAX_VALUE, values.last())
        assertTrue(values.zipWithNext().all { (previous, next) -> next > previous })
    }

    @Test
    fun countUpStepDelayMillis_withRoomInBudget_keepsRequestedDelay() {
        assertEquals(120L, countUpStepDelayMillis(stepCount = 5, stepDelayMillis = 120L, maxTotalMillis = 1500L))
    }

    @Test
    fun countUpStepDelayMillis_overBudget_shrinksDelayToFitTotal() {
        assertEquals(125L, countUpStepDelayMillis(stepCount = 12, stepDelayMillis = 900L, maxTotalMillis = 1500L))
    }

    @Test
    fun countUpStepDelayMillis_withoutSteps_isZero() {
        assertEquals(0L, countUpStepDelayMillis(stepCount = 0, stepDelayMillis = 120L, maxTotalMillis = 1500L))
    }
}
