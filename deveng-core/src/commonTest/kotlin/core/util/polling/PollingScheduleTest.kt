package core.util.polling

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class PollingScheduleTest {

    @Test
    fun fixedInterval_withoutMaxAttempts_neverRunsOut() {
        val schedule = PollingSchedule.fixedInterval(interval = INTERVAL)

        val nextDelay = schedule.nextDelay(PollProgress(elapsed = LONG_ELAPSED, attemptCount = Int.MAX_VALUE))

        assertEquals(INTERVAL, nextDelay)
        assertEquals(Duration.ZERO, schedule.initialDelay)
    }

    @Test
    fun fixedInterval_belowMaxAttempts_returnsTheInterval() {
        val schedule = PollingSchedule.fixedInterval(interval = INTERVAL, maxAttempts = MAX_ATTEMPTS)

        val nextDelay = schedule.nextDelay(PollProgress(elapsed = Duration.ZERO, attemptCount = MAX_ATTEMPTS - 1))

        assertEquals(INTERVAL, nextDelay)
    }

    @Test
    fun fixedInterval_atMaxAttempts_runsOut() {
        val schedule = PollingSchedule.fixedInterval(interval = INTERVAL, maxAttempts = MAX_ATTEMPTS)

        val nextDelay = schedule.nextDelay(PollProgress(elapsed = Duration.ZERO, attemptCount = MAX_ATTEMPTS))

        assertNull(nextDelay)
    }

    @Test
    fun fixedInterval_withZeroMaxAttempts_isRejected() {
        assertFailsWith<IllegalArgumentException> {
            PollingSchedule.fixedInterval(interval = INTERVAL, maxAttempts = 0)
        }
    }

    @Test
    fun fixedInterval_withZeroInterval_isRejected() {
        assertFailsWith<IllegalArgumentException> {
            PollingSchedule.fixedInterval(interval = Duration.ZERO)
        }
    }

    @Test
    fun fixedInterval_withOneMaxAttempt_isAccepted() {
        val schedule = PollingSchedule.fixedInterval(interval = INTERVAL, maxAttempts = 1)

        assertNull(schedule.nextDelay(PollProgress(elapsed = Duration.ZERO, attemptCount = 1)))
    }

    @Test
    fun fixedInterval_withInitialDelay_exposesIt() {
        val schedule = PollingSchedule.fixedInterval(interval = INTERVAL, initialDelay = INITIAL_DELAY)

        assertEquals(INITIAL_DELAY, schedule.initialDelay)
    }

    @Test
    fun fromElapsed_passesTheElapsedTime() {
        val schedule = PollingSchedule.fromElapsed { elapsed -> elapsed * 2 }

        val nextDelay = schedule.nextDelay(PollProgress(elapsed = INTERVAL, attemptCount = 1))

        assertEquals(INTERVAL * 2, nextDelay)
        assertEquals(Duration.ZERO, schedule.initialDelay)
    }

    private companion object {
        val INTERVAL = 8.seconds
        val INITIAL_DELAY = 3.seconds
        val LONG_ELAPSED = 3_600.seconds
        const val MAX_ATTEMPTS = 45
    }
}
