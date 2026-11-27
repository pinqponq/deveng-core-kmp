package core.util.polling

import co.touchlab.kermit.Logger
import co.touchlab.kermit.platformLogWriter
import core.testing.RecordingLogWriter
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class VisibleScreenPollerTest {
    private val recordingLogWriter = RecordingLogWriter()

    @BeforeTest
    fun setUp() {
        Logger.setLogWriters(recordingLogWriter)
    }

    @AfterTest
    fun tearDown() {
        Logger.setLogWriters(platformLogWriter())
    }

    @Test
    fun start_whileScreenHidden_makesNoAttempt() = runTest {
        val poller = VisibleScreenPoller(timeSource = testScheduler.timeSource)
        var attemptCount = 0

        poller.startCounting(scope = this) { attemptCount++ }
        advanceTimeBy(INTERVAL * 5)
        runCurrent()

        assertEquals(0, attemptCount)
        assertTrue(poller.isPolling)
    }

    @Test
    fun start_whileScreenVisible_attemptsAtOnceAndThenOnSchedule() = runTest {
        val poller = VisibleScreenPoller(timeSource = testScheduler.timeSource)
        var attemptCount = 0

        poller.changeVisibility(isVisible = true)
        poller.startCounting(scope = this) { attemptCount++ }
        runCurrent()
        assertEquals(1, attemptCount)

        advanceTimeBy(INTERVAL - ONE_MOMENT)
        runCurrent()
        assertEquals(1, attemptCount)

        advanceTimeBy(ONE_MOMENT)
        runCurrent()
        assertEquals(2, attemptCount)
    }

    @Test
    fun changeVisibility_hiddenThenVisible_pausesAndAttemptsStraightAway() = runTest {
        val poller = VisibleScreenPoller(timeSource = testScheduler.timeSource)
        var attemptCount = 0

        poller.changeVisibility(isVisible = true)
        poller.startCounting(scope = this) { attemptCount++ }
        runCurrent()
        poller.changeVisibility(isVisible = false)
        advanceTimeBy(INTERVAL * 3)
        runCurrent()
        assertEquals(1, attemptCount)
        assertTrue(poller.isPolling)

        poller.changeVisibility(isVisible = true)
        runCurrent()
        assertEquals(2, attemptCount)
    }

    @Test
    fun changeVisibility_visibleTwice_runsOneLoop() = runTest {
        val poller = VisibleScreenPoller(timeSource = testScheduler.timeSource)
        var attemptCount = 0

        poller.changeVisibility(isVisible = true)
        poller.startCounting(scope = this) { attemptCount++ }
        runCurrent()
        poller.changeVisibility(isVisible = true)
        runCurrent()

        assertEquals(1, attemptCount)
    }

    @Test
    fun changeVisibility_visibleWithoutSession_makesNoAttempt() = runTest {
        val poller = VisibleScreenPoller(timeSource = testScheduler.timeSource)

        poller.changeVisibility(isVisible = true)
        runCurrent()

        assertFalse(poller.isPolling)
    }

    @Test
    fun pollAttempt_returnsFinished_endsPolling() = runTest {
        val poller = VisibleScreenPoller(timeSource = testScheduler.timeSource)
        var attemptCount = 0
        var isExhaustionReported = false

        poller.changeVisibility(isVisible = true)
        poller.start(
            scope = backgroundScope,
            schedule = PollingSchedule.fixedInterval(interval = INTERVAL),
            pollAttempt = {
                attemptCount++
                if (attemptCount == 2) PollAttemptOutcome.FINISHED else PollAttemptOutcome.CONTINUE
            },
            onScheduleExhausted = { isExhaustionReported = true },
        )

        advanceTimeBy(INTERVAL * 10)
        runCurrent()

        assertEquals(2, attemptCount)
        assertFalse(poller.isPolling)
        assertFalse(isExhaustionReported)
    }

    @Test
    fun fromElapsedSchedule_runsOut_stopsPollingAndReportsExhaustion() = runTest {
        val poller = VisibleScreenPoller(timeSource = testScheduler.timeSource)
        var exhaustionReportCount = 0
        var attemptCount = 0

        poller.changeVisibility(isVisible = true)
        poller.start(
            scope = backgroundScope,
            schedule = PollingSchedule.fromElapsed { elapsed -> if (elapsed < DURATION) INTERVAL else null },
            pollAttempt = {
                attemptCount++
                PollAttemptOutcome.CONTINUE
            },
            onScheduleExhausted = { exhaustionReportCount++ },
        )

        advanceTimeBy(DURATION * 3)
        runCurrent()

        assertEquals(1, exhaustionReportCount)
        assertFalse(poller.isPolling)
        assertEquals((DURATION / INTERVAL).toInt() + 1, attemptCount)
    }

    @Test
    fun fixedIntervalSchedule_withMaxAttempts_stopsAfterMaxAttempts() = runTest {
        val poller = VisibleScreenPoller(timeSource = testScheduler.timeSource)
        var attemptCount = 0
        var isExhaustionReported = false

        poller.changeVisibility(isVisible = true)
        poller.start(
            scope = backgroundScope,
            schedule = PollingSchedule.fixedInterval(interval = INTERVAL, maxAttempts = MAX_ATTEMPTS),
            pollAttempt = {
                attemptCount++
                PollAttemptOutcome.CONTINUE
            },
            onScheduleExhausted = { isExhaustionReported = true },
        )

        advanceTimeBy(INTERVAL * (MAX_ATTEMPTS + 5))
        runCurrent()

        assertEquals(MAX_ATTEMPTS, attemptCount)
        assertTrue(isExhaustionReported)
        assertFalse(poller.isPolling)
    }

    @Test
    fun fixedIntervalSchedule_withInitialDelay_waitsBeforeTheFirstAttempt() = runTest {
        val poller = VisibleScreenPoller(timeSource = testScheduler.timeSource)
        var attemptCount = 0

        poller.changeVisibility(isVisible = true)
        poller.start(
            scope = backgroundScope,
            schedule = PollingSchedule.fixedInterval(interval = INTERVAL, initialDelay = INITIAL_DELAY),
            pollAttempt = {
                attemptCount++
                PollAttemptOutcome.CONTINUE
            },
        )

        advanceTimeBy(INITIAL_DELAY - ONE_MOMENT)
        runCurrent()
        assertEquals(0, attemptCount)

        advanceTimeBy(ONE_MOMENT)
        runCurrent()
        assertEquals(1, attemptCount)
    }

    @Test
    fun changeVisibility_resumeDuringInitialDelay_waitsOnlyTheRestOfIt() = runTest {
        val poller = VisibleScreenPoller(timeSource = testScheduler.timeSource)
        var attemptCount = 0

        poller.changeVisibility(isVisible = true)
        poller.start(
            scope = backgroundScope,
            schedule = PollingSchedule.fixedInterval(interval = INTERVAL, initialDelay = INITIAL_DELAY),
            pollAttempt = {
                attemptCount++
                PollAttemptOutcome.CONTINUE
            },
        )

        advanceTimeBy(ONE_MOMENT)
        poller.changeVisibility(isVisible = false)
        advanceTimeBy(ONE_MOMENT)
        poller.changeVisibility(isVisible = true)
        advanceTimeBy(INITIAL_DELAY - ONE_MOMENT * 3)
        runCurrent()
        assertEquals(0, attemptCount)

        advanceTimeBy(ONE_MOMENT)
        runCurrent()
        assertEquals(1, attemptCount)
    }

    @Test
    fun changeVisibility_resumeAfterLongPause_keepsTheOriginalScheduleStart() = runTest {
        val poller = VisibleScreenPoller(timeSource = testScheduler.timeSource)
        val elapsedPerAttempt = mutableListOf<Duration>()

        poller.changeVisibility(isVisible = true)
        poller.start(
            scope = backgroundScope,
            schedule = PollingSchedule { progress ->
                elapsedPerAttempt.add(progress.elapsed)
                INTERVAL
            },
            pollAttempt = { PollAttemptOutcome.CONTINUE },
        )

        runCurrent()
        poller.changeVisibility(isVisible = false)
        advanceTimeBy(DURATION)
        poller.changeVisibility(isVisible = true)
        runCurrent()

        assertEquals(listOf(Duration.ZERO, DURATION), elapsedPerAttempt)
    }

    @Test
    fun pollAttempt_throws_countsTheAttemptAndPollingContinues() = runTest {
        val poller = VisibleScreenPoller(timeSource = testScheduler.timeSource)
        val attemptCountPerProgress = mutableListOf<Int>()
        var attemptCount = 0

        poller.changeVisibility(isVisible = true)
        poller.start(
            scope = backgroundScope,
            schedule = PollingSchedule { progress ->
                attemptCountPerProgress.add(progress.attemptCount)
                INTERVAL
            },
            pollAttempt = {
                attemptCount++
                if (attemptCount == 1) {
                    throw IllegalStateException(ATTEMPT_FAILURE_MESSAGE)
                }

                PollAttemptOutcome.CONTINUE
            },
        )

        runCurrent()
        advanceTimeBy(INTERVAL)
        runCurrent()

        assertEquals(2, attemptCount)
        assertEquals(listOf(1, 2), attemptCountPerProgress)
        assertIs<IllegalStateException>(recordingLogWriter.warningThrowables.single())
        assertTrue(poller.isPolling)
    }

    @Test
    fun pollAttempt_timesOut_countsTheAttemptAndPollingContinues() = runTest {
        val poller = VisibleScreenPoller(timeSource = testScheduler.timeSource)
        var attemptCount = 0

        poller.changeVisibility(isVisible = true)
        poller.start(
            scope = backgroundScope,
            schedule = PollingSchedule.fixedInterval(interval = INTERVAL),
            pollAttempt = {
                attemptCount++
                if (attemptCount == 1) {
                    withTimeout(timeout = ONE_MOMENT) { awaitCancellation() }
                }

                PollAttemptOutcome.CONTINUE
            },
        )

        advanceTimeBy(ONE_MOMENT + INTERVAL)
        runCurrent()

        assertEquals(2, attemptCount)
        assertIs<TimeoutCancellationException>(recordingLogWriter.warningThrowables.single())
        assertTrue(poller.isPolling)
    }

    @Test
    fun changeVisibility_hiddenDuringAttempt_doesNotCountTheCutOffAttempt() = runTest {
        val poller = VisibleScreenPoller(timeSource = testScheduler.timeSource)
        val attemptCountPerProgress = mutableListOf<Int>()
        val firstAttemptGate = CompletableDeferred<Unit>()
        var startedAttemptCount = 0

        poller.changeVisibility(isVisible = true)
        poller.start(
            scope = backgroundScope,
            schedule = PollingSchedule { progress ->
                attemptCountPerProgress.add(progress.attemptCount)
                INTERVAL
            },
            pollAttempt = {
                startedAttemptCount++
                if (startedAttemptCount == 1) {
                    firstAttemptGate.await()
                }

                PollAttemptOutcome.CONTINUE
            },
        )

        runCurrent()
        poller.changeVisibility(isVisible = false)
        poller.changeVisibility(isVisible = true)
        runCurrent()

        assertEquals(2, startedAttemptCount)
        assertEquals(listOf(1), attemptCountPerProgress)
    }

    @Test
    fun changeVisibility_hiddenDuringAttemptThatIgnoresCancellation_dropsItsResult() = runTest {
        val poller = VisibleScreenPoller(timeSource = testScheduler.timeSource)
        val attemptCountPerProgress = mutableListOf<Int>()
        val firstAttemptGate = CompletableDeferred<Unit>()
        var startedAttemptCount = 0

        poller.changeVisibility(isVisible = true)
        poller.start(
            scope = backgroundScope,
            schedule = PollingSchedule { progress ->
                attemptCountPerProgress.add(progress.attemptCount)
                INTERVAL
            },
            pollAttempt = {
                startedAttemptCount++
                if (startedAttemptCount == 1) {
                    withContext(NonCancellable) { firstAttemptGate.await() }
                }

                PollAttemptOutcome.CONTINUE
            },
        )

        runCurrent()
        poller.changeVisibility(isVisible = false)
        poller.changeVisibility(isVisible = true)
        runCurrent()
        firstAttemptGate.complete(Unit)
        runCurrent()

        assertEquals(2, startedAttemptCount)
        assertEquals(listOf(1), attemptCountPerProgress)
        assertTrue(poller.isPolling)
    }

    @Test
    fun changeVisibility_hiddenDuringAttemptThatIgnoresCancellationAndThrows_dropsIt() = runTest {
        val poller = VisibleScreenPoller(timeSource = testScheduler.timeSource)
        val attemptCountPerProgress = mutableListOf<Int>()
        val firstAttemptGate = CompletableDeferred<Unit>()
        var startedAttemptCount = 0

        poller.changeVisibility(isVisible = true)
        poller.start(
            scope = backgroundScope,
            schedule = PollingSchedule { progress ->
                attemptCountPerProgress.add(progress.attemptCount)
                INTERVAL
            },
            pollAttempt = {
                startedAttemptCount++
                if (startedAttemptCount == 1) {
                    withContext(NonCancellable) { firstAttemptGate.await() }
                    throw IllegalStateException(ATTEMPT_FAILURE_MESSAGE)
                }

                PollAttemptOutcome.CONTINUE
            },
        )

        runCurrent()
        poller.changeVisibility(isVisible = false)
        poller.changeVisibility(isVisible = true)
        runCurrent()
        firstAttemptGate.complete(Unit)
        runCurrent()

        assertEquals(2, startedAttemptCount)
        assertEquals(listOf(1), attemptCountPerProgress)
        assertTrue(poller.isPolling)
    }

    @Test
    fun stop_duringAttempt_cancelsItAndEndsPolling() = runTest {
        val poller = VisibleScreenPoller(timeSource = testScheduler.timeSource)
        val attemptGate = CompletableDeferred<Unit>()
        var startedAttemptCount = 0
        var completedAttemptCount = 0

        poller.changeVisibility(isVisible = true)
        poller.start(
            scope = backgroundScope,
            schedule = PollingSchedule.fixedInterval(interval = INTERVAL),
            pollAttempt = {
                startedAttemptCount++
                attemptGate.await()
                completedAttemptCount++
                PollAttemptOutcome.CONTINUE
            },
        )

        runCurrent()
        poller.stop()
        attemptGate.complete(Unit)
        advanceTimeBy(INTERVAL * 3)
        runCurrent()

        assertEquals(1, startedAttemptCount)
        assertEquals(0, completedAttemptCount)
        assertTrue(recordingLogWriter.warningThrowables.isEmpty())
        assertFalse(poller.isPolling)
    }

    @Test
    fun stop_whileHidden_keepsPollingOffAfterTheScreenIsVisible() = runTest {
        val poller = VisibleScreenPoller(timeSource = testScheduler.timeSource)
        var attemptCount = 0

        poller.startCounting(scope = this) { attemptCount++ }
        poller.stop()
        poller.changeVisibility(isVisible = true)
        runCurrent()

        assertEquals(0, attemptCount)
        assertFalse(poller.isPolling)
    }

    @Test
    fun start_whilePolling_replacesTheRunningSession() = runTest {
        val poller = VisibleScreenPoller(timeSource = testScheduler.timeSource)
        var firstSessionAttemptCount = 0
        var secondSessionAttemptCount = 0

        poller.changeVisibility(isVisible = true)
        poller.startCounting(scope = this) { firstSessionAttemptCount++ }
        runCurrent()
        poller.startCounting(scope = this) { secondSessionAttemptCount++ }
        advanceTimeBy(INTERVAL * 2)
        runCurrent()

        assertEquals(1, firstSessionAttemptCount)
        assertEquals(3, secondSessionAttemptCount)
        assertTrue(poller.isPolling)
    }

    @Test
    fun start_fromInsideAnAttempt_replacedSessionDoesNotEndTheNewOne() = runTest {
        val poller = VisibleScreenPoller(timeSource = testScheduler.timeSource)
        var secondSessionAttemptCount = 0

        poller.changeVisibility(isVisible = true)
        poller.start(
            scope = backgroundScope,
            schedule = PollingSchedule.fixedInterval(interval = INTERVAL),
            pollAttempt = {
                poller.startCounting(scope = this@runTest) { secondSessionAttemptCount++ }
                PollAttemptOutcome.FINISHED
            },
        )

        runCurrent()

        assertTrue(poller.isPolling)
        assertEquals(1, secondSessionAttemptCount)
    }

    private fun VisibleScreenPoller.startCounting(scope: TestScope, onAttempt: () -> Unit) {
        start(
            scope = scope.backgroundScope,
            schedule = PollingSchedule.fixedInterval(interval = INTERVAL),
            pollAttempt = {
                onAttempt()
                PollAttemptOutcome.CONTINUE
            },
        )
    }

    private companion object {
        val INTERVAL = 2.seconds
        val DURATION = 10.seconds
        val INITIAL_DELAY = 8.seconds
        val ONE_MOMENT = 1.seconds
        const val MAX_ATTEMPTS = 4
        const val ATTEMPT_FAILURE_MESSAGE = "connection reset"
    }
}
