package core.util

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class CooldownTimerTest {

    private companion object {
        const val TICK_MILLIS = 1_000L
        const val TWO_TICKS_MILLIS = 2_000L
        const val FIVE_TICKS_MILLIS = 5_000L
        const val LONG_WAIT_MILLIS = 10_000L
        const val SHORT_TICK_MILLIS = 500L
    }

    private fun TestScope.timer(tickDelayMillis: Long = TICK_MILLIS) =
        CooldownTimer(scope = this, tickDelayMillis = tickDelayMillis)

    @Test
    fun remainingSeconds_beforeStart_isZeroAndNotRunning() = runTest {
        val timer = timer()

        assertEquals(expected = 0, actual = timer.remainingSeconds.value)
        assertFalse(actual = timer.isRunning.value)
    }

    @Test
    fun start_withThreeSeconds_countsDownToZeroAndStopsRunning() = runTest {
        val timer = timer()

        timer.start(seconds = 3)
        runCurrent()
        assertEquals(expected = 3, actual = timer.remainingSeconds.value)
        assertTrue(actual = timer.isRunning.value)

        advanceTimeBy(delayTimeMillis = TICK_MILLIS)
        runCurrent()
        assertEquals(expected = 2, actual = timer.remainingSeconds.value)

        advanceTimeBy(delayTimeMillis = TICK_MILLIS)
        runCurrent()
        assertEquals(expected = 1, actual = timer.remainingSeconds.value)
        assertTrue(actual = timer.isRunning.value)

        advanceTimeBy(delayTimeMillis = TICK_MILLIS)
        runCurrent()
        assertEquals(expected = 0, actual = timer.remainingSeconds.value)
        assertFalse(actual = timer.isRunning.value)
    }

    @Test
    fun start_beforeTickDelayPassed_keepsRemainingSeconds() = runTest {
        val timer = timer(tickDelayMillis = SHORT_TICK_MILLIS)

        timer.start(seconds = 2)
        advanceTimeBy(delayTimeMillis = (SHORT_TICK_MILLIS - 1L))
        runCurrent()
        assertEquals(expected = 2, actual = timer.remainingSeconds.value)

        advanceTimeBy(delayTimeMillis = 1L)
        runCurrent()
        assertEquals(expected = 1, actual = timer.remainingSeconds.value)
    }

    @Test
    fun start_whileRunning_restartsFromNewValue() = runTest {
        val timer = timer()

        timer.start(seconds = 5)
        advanceTimeBy(delayTimeMillis = TWO_TICKS_MILLIS)
        runCurrent()
        assertEquals(expected = 3, actual = timer.remainingSeconds.value)

        timer.start(seconds = 10)
        runCurrent()
        assertEquals(expected = 10, actual = timer.remainingSeconds.value)

        advanceTimeBy(delayTimeMillis = TICK_MILLIS)
        runCurrent()
        assertEquals(expected = 9, actual = timer.remainingSeconds.value)
    }

    @Test
    fun start_twice_leavesOnlyOneCountdownRunning() = runTest {
        val timer = timer()

        timer.start(seconds = 5)
        timer.start(seconds = 5)
        advanceTimeBy(delayTimeMillis = TICK_MILLIS)
        runCurrent()

        assertEquals(expected = 4, actual = timer.remainingSeconds.value)
    }

    @Test
    fun cancel_whileRunning_stopsCountdownAndResetsToZero() = runTest {
        val timer = timer()

        timer.start(seconds = 5)
        advanceTimeBy(delayTimeMillis = TICK_MILLIS)
        runCurrent()
        timer.cancel()

        assertEquals(expected = 0, actual = timer.remainingSeconds.value)
        assertFalse(actual = timer.isRunning.value)
        advanceTimeBy(delayTimeMillis = LONG_WAIT_MILLIS)
        runCurrent()
        assertEquals(expected = 0, actual = timer.remainingSeconds.value)
    }

    @Test
    fun start_withZeroSeconds_finishesAtOnce() = runTest {
        val timer = timer()

        timer.start(seconds = 0)

        assertEquals(expected = 0, actual = timer.remainingSeconds.value)
        assertFalse(actual = timer.isRunning.value)
    }

    @Test
    fun start_withNegativeSeconds_finishesAtOnce() = runTest {
        val timer = timer()

        timer.start(seconds = -4)

        assertEquals(expected = 0, actual = timer.remainingSeconds.value)
        assertFalse(actual = timer.isRunning.value)
    }

    @Test
    fun start_withZeroSecondsWhileRunning_stopsCountdown() = runTest {
        val timer = timer()

        timer.start(seconds = 5)
        runCurrent()
        timer.start(seconds = 0)
        advanceTimeBy(delayTimeMillis = FIVE_TICKS_MILLIS)
        runCurrent()

        assertEquals(expected = 0, actual = timer.remainingSeconds.value)
        assertFalse(actual = timer.isRunning.value)
    }

    @Test
    fun start_afterFinished_runsAgain() = runTest {
        val timer = timer()

        timer.start(seconds = 1)
        advanceTimeBy(delayTimeMillis = TICK_MILLIS)
        runCurrent()
        assertFalse(actual = timer.isRunning.value)

        timer.start(seconds = 2)
        runCurrent()

        assertEquals(expected = 2, actual = timer.remainingSeconds.value)
        assertTrue(actual = timer.isRunning.value)
    }
}
