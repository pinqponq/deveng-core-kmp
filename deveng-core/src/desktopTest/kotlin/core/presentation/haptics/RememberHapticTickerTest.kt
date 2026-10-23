package core.presentation.haptics

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runDesktopComposeUiTest
import core.presentation.haptics.fake.FakeHapticFeedback
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertSame

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

    @Test
    fun rememberHapticTicker_withoutInterval_dropsImmediateSecondLightTick() = runDesktopComposeUiTest {
        val hapticFeedback = FakeHapticFeedback()
        lateinit var hapticTicker: HapticTicker
        setContent {
            CompositionLocalProvider(LocalHapticFeedback provides hapticFeedback) {
                hapticTicker = rememberHapticTicker()
            }
        }

        hapticTicker.light()
        hapticTicker.light()

        assertEquals(1, hapticFeedback.performedTypes.size)
    }

    @Test
    fun rememberHapticTicker_withZeroInterval_performsEveryLightTick() = runDesktopComposeUiTest {
        val hapticFeedback = FakeHapticFeedback()
        lateinit var hapticTicker: HapticTicker
        setContent {
            CompositionLocalProvider(LocalHapticFeedback provides hapticFeedback) {
                hapticTicker = rememberHapticTicker(minLightIntervalMillis = 0L)
            }
        }

        hapticTicker.light()
        hapticTicker.light()

        assertEquals(2, hapticFeedback.performedTypes.size)
    }

    @Test
    fun rememberHapticTicker_afterIntervalPassed_performsNextLightTick() = runDesktopComposeUiTest {
        val hapticFeedback = FakeHapticFeedback()
        lateinit var hapticTicker: HapticTicker
        setContent {
            CompositionLocalProvider(LocalHapticFeedback provides hapticFeedback) {
                hapticTicker = rememberHapticTicker(minLightIntervalMillis = SHORT_INTERVAL_MILLIS)
            }
        }

        hapticTicker.light()
        Thread.sleep(WAIT_MILLIS)
        hapticTicker.light()

        assertEquals(2, hapticFeedback.performedTypes.size)
    }

    @Test
    fun rememberHapticTicker_whenFeedbackChanges_performsOnNewFeedback() = runDesktopComposeUiTest {
        val firstFeedback = FakeHapticFeedback()
        val secondFeedback = FakeHapticFeedback()
        var hapticFeedback by mutableStateOf<androidx.compose.ui.hapticfeedback.HapticFeedback>(firstFeedback)
        lateinit var hapticTicker: HapticTicker
        setContent {
            CompositionLocalProvider(LocalHapticFeedback provides hapticFeedback) {
                hapticTicker = rememberHapticTicker()
            }
        }
        val firstTicker = hapticTicker

        hapticFeedback = secondFeedback
        waitForIdle()
        hapticTicker.strong()

        assertNotSame(firstTicker, hapticTicker)
        assertEquals(emptyList(), firstFeedback.performedTypes)
        assertEquals(listOf(HapticFeedbackType.LongPress), secondFeedback.performedTypes)
    }

    @Test
    fun rememberHapticTicker_whenIntervalChanges_usesNewInterval() = runDesktopComposeUiTest {
        val hapticFeedback = FakeHapticFeedback()
        var intervalMillis by mutableStateOf(LONG_INTERVAL_MILLIS)
        lateinit var hapticTicker: HapticTicker
        setContent {
            CompositionLocalProvider(LocalHapticFeedback provides hapticFeedback) {
                hapticTicker = rememberHapticTicker(minLightIntervalMillis = intervalMillis)
            }
        }
        val firstTicker = hapticTicker

        intervalMillis = 0L
        waitForIdle()
        hapticTicker.light()
        hapticTicker.light()

        assertNotSame(firstTicker, hapticTicker)
        assertEquals(2, hapticFeedback.performedTypes.size)
    }

    @Test
    fun rememberHapticTicker_whenRecomposedWithSameInputs_keepsTicker() = runDesktopComposeUiTest {
        var tick by mutableStateOf(0)
        lateinit var hapticTicker: HapticTicker
        setContent {
            CompositionLocalProvider(LocalHapticFeedback provides FakeHapticFeedback()) {
                tick
                hapticTicker = rememberHapticTicker()
            }
        }
        val firstTicker = hapticTicker

        tick = 1
        waitForIdle()

        assertSame(firstTicker, hapticTicker)
    }

    private companion object {
        const val SHORT_INTERVAL_MILLIS = 5L
        const val WAIT_MILLIS = 30L
        const val LONG_INTERVAL_MILLIS = 60_000L
    }
}
