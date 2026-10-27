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

    private fun TestScope.timer(tickDelayMillis: Long = 1_000L) =
        CooldownTimer(scope = this, tickDelayMillis = tickDelayMillis)

    @Test
    fun `is idle before start`() = runTest {
        val timer = timer()

        assertEquals(expected = 0, actual = timer.remainingSeconds.value)
        assertFalse(actual = timer.isRunning.value)
    }

    @Test
    fun `counts down second by second and finishes at zero`() = runTest {
        val timer = timer()

        timer.start(seconds = 3)
        runCurrent()
        assertEquals(expected = 3, actual = timer.remainingSeconds.value)
        assertTrue(actual = timer.isRunning.value)

        advanceTimeBy(delayTimeMillis = 1_000L)
        runCurrent()
        assertEquals(expected = 2, actual = timer.remainingSeconds.value)

        advanceTimeBy(delayTimeMillis = 1_000L)
        runCurrent()
        assertEquals(expected = 1, actual = timer.remainingSeconds.value)
        assertTrue(actual = timer.isRunning.value)

        advanceTimeBy(delayTimeMillis = 1_000L)
        runCurrent()
        assertEquals(expected = 0, actual = timer.remainingSeconds.value)
        assertFalse(actual = timer.isRunning.value)
    }

    @Test
    fun `does not tick before the tick delay has passed`() = runTest {
        val timer = timer(tickDelayMillis = 500L)

        timer.start(seconds = 2)
        advanceTimeBy(delayTimeMillis = 499L)
        runCurrent()
        assertEquals(expected = 2, actual = timer.remainingSeconds.value)

        advanceTimeBy(delayTimeMillis = 1L)
        runCurrent()
        assertEquals(expected = 1, actual = timer.remainingSeconds.value)
    }

    @Test
    fun `start again restarts from the new value`() = runTest {
        val timer = timer()

        timer.start(seconds = 5)
        advanceTimeBy(delayTimeMillis = 2_000L)
        runCurrent()
        assertEquals(expected = 3, actual = timer.remainingSeconds.value)

        timer.start(seconds = 10)
        runCurrent()
        assertEquals(expected = 10, actual = timer.remainingSeconds.value)

        advanceTimeBy(delayTimeMillis = 1_000L)
        runCurrent()
        assertEquals(expected = 9, actual = timer.remainingSeconds.value)
    }

    @Test
    fun `restarting leaves only one countdown running`() = runTest {
        val timer = timer()

        timer.start(seconds = 5)
        timer.start(seconds = 5)
        advanceTimeBy(delayTimeMillis = 1_000L)
        runCurrent()

        assertEquals(expected = 4, actual = timer.remainingSeconds.value)
    }

    @Test
    fun `cancel stops the countdown and resets to zero`() = runTest {
        val timer = timer()

        timer.start(seconds = 5)
        advanceTimeBy(delayTimeMillis = 1_000L)
        runCurrent()
        timer.cancel()

        assertEquals(expected = 0, actual = timer.remainingSeconds.value)
        assertFalse(actual = timer.isRunning.value)
        advanceTimeBy(delayTimeMillis = 10_000L)
        runCurrent()
        assertEquals(expected = 0, actual = timer.remainingSeconds.value)
    }

    @Test
    fun `zero seconds finishes at once`() = runTest {
        val timer = timer()

        timer.start(seconds = 0)

        assertEquals(expected = 0, actual = timer.remainingSeconds.value)
        assertFalse(actual = timer.isRunning.value)
    }

    @Test
    fun `negative seconds finish at once`() = runTest {
        val timer = timer()

        timer.start(seconds = -4)

        assertEquals(expected = 0, actual = timer.remainingSeconds.value)
        assertFalse(actual = timer.isRunning.value)
    }

    @Test
    fun `zero seconds stops a running countdown`() = runTest {
        val timer = timer()

        timer.start(seconds = 5)
        runCurrent()
        timer.start(seconds = 0)
        advanceTimeBy(delayTimeMillis = 5_000L)
        runCurrent()

        assertEquals(expected = 0, actual = timer.remainingSeconds.value)
        assertFalse(actual = timer.isRunning.value)
    }

    @Test
    fun `can start again after finishing`() = runTest {
        val timer = timer()

        timer.start(seconds = 1)
        advanceTimeBy(delayTimeMillis = 1_000L)
        runCurrent()
        assertFalse(actual = timer.isRunning.value)

        timer.start(seconds = 2)
        runCurrent()

        assertEquals(expected = 2, actual = timer.remainingSeconds.value)
        assertTrue(actual = timer.isRunning.value)
    }
}
