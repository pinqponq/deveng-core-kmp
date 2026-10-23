package core.presentation.haptics

import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import core.presentation.haptics.fake.FakeHapticFeedback
import kotlin.test.Test
import kotlin.test.assertEquals

class HapticTickerTest {
    private val hapticFeedback = FakeHapticFeedback()
    private var nowMillis = 1_000L
    private val hapticTicker = HapticTicker(
        hapticFeedback = hapticFeedback,
        minLightIntervalMillis = MIN_LIGHT_INTERVAL_MILLIS,
        currentTimeMillis = { nowMillis }
    )

    @Test
    fun light_firstTick_performsSegmentFrequentTick() {
        hapticTicker.light()

        assertEquals(listOf(HapticFeedbackType.SegmentFrequentTick), hapticFeedback.performedTypes)
    }

    @Test
    fun light_beforeMinimumInterval_dropsTick() {
        hapticTicker.light()
        nowMillis += MIN_LIGHT_INTERVAL_MILLIS - 1

        hapticTicker.light()

        assertEquals(1, hapticFeedback.performedTypes.size)
    }

    @Test
    fun light_atMinimumInterval_performsTick() {
        hapticTicker.light()
        nowMillis += MIN_LIGHT_INTERVAL_MILLIS

        hapticTicker.light()

        assertEquals(2, hapticFeedback.performedTypes.size)
    }

    @Test
    fun light_afterDroppedTick_measuresIntervalFromLastPlayedTick() {
        hapticTicker.light()
        nowMillis += MIN_LIGHT_INTERVAL_MILLIS - 1
        hapticTicker.light()
        nowMillis += 1

        hapticTicker.light()

        assertEquals(2, hapticFeedback.performedTypes.size)
    }

    @Test
    fun medium_rightAfterLightTick_stillPerformsContextClick() {
        hapticTicker.light()

        hapticTicker.medium()

        assertEquals(HapticFeedbackType.ContextClick, hapticFeedback.performedTypes.last())
    }

    @Test
    fun strong_repeatedAtOnce_performsLongPressEachTime() {
        hapticTicker.strong()
        hapticTicker.strong()

        assertEquals(List(2) { HapticFeedbackType.LongPress }, hapticFeedback.performedTypes)
    }

    private companion object {
        const val MIN_LIGHT_INTERVAL_MILLIS = 40L
    }
}
