package core.util.polling

import co.touchlab.kermit.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.TimeMark
import kotlin.time.TimeSource

/**
 * Repeats a check while its screen is visible, for example while the server finishes a payment or a render.
 *
 * A ViewModel outlives its screen (tab navigation keeps it), so polling that ignored visibility would keep
 * calling the server from a screen nobody is looking at. The screen reports visibility through
 * [changeVisibility]. While the screen is hidden the loop is cancelled. When the screen is visible again,
 * the poller checks straight away and then continues the same schedule, measured from [start].
 * The first attempt of a session waits the schedule's initial delay, also measured from [start]; a screen
 * that becomes visible again before that delay has passed waits for the rest of it.
 * An attempt cut off because the screen was hidden is not counted and runs again on resume.
 * A screen that never reports visibility is treated as hidden, so it must call `changeVisibility(true)`.
 *
 * Polling ends when an attempt returns [PollAttemptOutcome.FINISHED], when the schedule runs out, or on [stop].
 * An attempt that throws, a timeout inside it included, is logged and counts as [PollAttemptOutcome.CONTINUE].
 *
 * Call every function from the thread of [start]'s scope (for example the ViewModel's main scope): the
 * session fields are not synchronised. An attempt that does I/O switches dispatcher itself.
 *
 * @param timeSource measures the time since [start]; tests pass `testScheduler.timeSource`.
 */
class VisibleScreenPoller(
    private val timeSource: TimeSource = TimeSource.Monotonic,
) {
    private var pollingJob: Job? = null
    private var session: PollingSession? = null
    private var isScreenVisible = false

    /** True from [start] until polling ends; stays true while the screen is hidden. */
    val isPolling: Boolean
        get() = session != null

    /**
     * Starts a new polling session and replaces a running one.
     *
     * @param onScheduleExhausted called once when [schedule] runs out before an attempt finished.
     */
    fun start(
        scope: CoroutineScope,
        schedule: PollingSchedule,
        pollAttempt: suspend () -> PollAttemptOutcome,
        onScheduleExhausted: () -> Unit = {},
    ) {
        stop()
        session = PollingSession(
            scope = scope,
            startMark = timeSource.markNow(),
            schedule = schedule,
            pollAttempt = pollAttempt,
            onScheduleExhausted = onScheduleExhausted,
        )

        if (isScreenVisible) {
            launchPollingLoop()
        }
    }

    fun changeVisibility(isVisible: Boolean) {
        isScreenVisible = isVisible

        if (!isVisible) {
            cancelPollingLoop()

            return
        }

        val isLoopRunning = pollingJob?.isActive == true
        if (session != null && !isLoopRunning) {
            launchPollingLoop()
        }
    }

    fun stop() {
        cancelPollingLoop()
        session = null
    }

    private fun launchPollingLoop() {
        val activeSession = session ?: return

        pollingJob = activeSession.scope.launch {
            runPollingLoop(activeSession)
        }
    }

    private suspend fun runPollingLoop(activeSession: PollingSession) {
        waitForFirstAttempt(activeSession)

        while (true) {
            val outcome = attemptSafely(activeSession)
            if (outcome == PollAttemptOutcome.FINISHED) {
                endSession(activeSession)

                return
            }

            val progress = PollProgress(
                elapsed = activeSession.startMark.elapsedNow(),
                attemptCount = activeSession.attemptCount,
            )

            val nextDelay = activeSession.schedule.nextDelay(progress)
            if (nextDelay == null) {
                endSession(activeSession)
                activeSession.onScheduleExhausted()

                return
            }

            delay(nextDelay)
        }
    }

    private suspend fun waitForFirstAttempt(activeSession: PollingSession) {
        if (activeSession.attemptCount > 0) {
            return
        }

        val remainingInitialDelay = activeSession.schedule.initialDelay - activeSession.startMark.elapsedNow()
        delay(remainingInitialDelay)
    }

    private suspend fun attemptSafely(activeSession: PollingSession): PollAttemptOutcome {
        val outcome = try {
            activeSession.pollAttempt().also {
                // An attempt that ignores cancellation can return after its loop was cancelled; drop its result.
                currentCoroutineContext().ensureActive()
            }
        } catch (cancellation: CancellationException) {
            // A timeout inside the attempt is a CancellationException too; only a cancelled loop may end polling.
            currentCoroutineContext().ensureActive()
            Logger.w(throwable = cancellation) { "A poll attempt was cancelled; polling continues." }

            PollAttemptOutcome.CONTINUE
        } catch (exception: Exception) {
            Logger.w(throwable = exception) { "A poll attempt failed; polling continues." }

            PollAttemptOutcome.CONTINUE
        }

        activeSession.attemptCount++

        return outcome
    }

    private fun endSession(activeSession: PollingSession) {
        if (session === activeSession) {
            session = null
        }
    }

    private fun cancelPollingLoop() {
        pollingJob?.cancel()
        pollingJob = null
    }

    private class PollingSession(
        val scope: CoroutineScope,
        val startMark: TimeMark,
        val schedule: PollingSchedule,
        val pollAttempt: suspend () -> PollAttemptOutcome,
        val onScheduleExhausted: () -> Unit,
    ) {
        var attemptCount = 0
    }
}
