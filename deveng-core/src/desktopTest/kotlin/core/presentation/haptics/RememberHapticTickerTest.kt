package core.presentation.haptics

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runDesktopComposeUiTest
import core.presentation.haptics.fake.FakeHapticFeedback
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class RememberHapticTickerTest {

    @Test
    fun rememberHapticTicker_withProvidedFeedback_performsTicksOnIt() = runDesktopComposeUiTest {
        val hapticFeedback = FakeHapticFeedback()
        lateinit var hapticTicker: HapticTicker
        setContent {
            CompositionLocalProvider(LocalHapticFeedback provides hapticFeedback) {
                hapticTicker = rememberHapticTicker()
            }
        }

        hapticTicker.strong()

        assertEquals(listOf(HapticFeedbackType.LongPress), hapticFeedback.performedTypes)
    }

    @Test
    fun rememberHapticTicker_withDefaultInterval_dropsImmediateSecondLightTick() = runDesktopComposeUiTest {
        val hapticFeedback = FakeHapticFeedback()
        lateinit var hapticTicker: HapticTicker
        setContent {
            CompositionLocalProvider(LocalHapticFeedback provides hapticFeedback) {
                hapticTicker = rememberHapticTicker(minLightIntervalMillis = LONG_INTERVAL_MILLIS)
            }
        }

        hapticTicker.light()
        hapticTicker.light()

        assertEquals(1, hapticFeedback.performedTypes.size)
    }

    private companion object {
        const val LONG_INTERVAL_MILLIS = 60_000L
    }
}
