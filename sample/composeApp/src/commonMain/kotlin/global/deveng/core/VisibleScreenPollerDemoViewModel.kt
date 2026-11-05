package global.deveng.core

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import core.util.polling.PollAttemptOutcome
import core.util.polling.PollingSchedule
import core.util.polling.VisibleScreenPoller
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.time.Duration.Companion.seconds

internal const val DEMO_MAX_ATTEMPTS = 10
internal const val DEMO_ATTEMPTS_UNTIL_FINISHED = 6
internal val DEMO_INTERVAL = 2.seconds
internal val DEMO_INITIAL_DELAY = 1.seconds

internal enum class DemoPollingStatus(val label: String) {
    IDLE("Idle"),
    POLLING("Polling"),
    FINISHED("Finished"),
    SCHEDULE_EXHAUSTED("Schedule ran out"),
    STOPPED("Stopped"),
}

internal enum class DemoJobBehavior(val label: String, val toggleButtonText: String) {
    FINISHES("Job: done on check $DEMO_ATTEMPTS_UNTIL_FINISHED", "Make the job never finish"),
    NEVER_FINISHES("Job: never done, the schedule runs out", "Make the job finish"),
}

internal enum class DemoScreenVisibility(val label: String, val toggleButtonText: String) {
    VISIBLE("Screen: visible", "Hide screen"),
    HIDDEN("Screen: hidden (paused)", "Show screen"),
}

internal data class VisibleScreenPollerDemoState(
    val pollingStatus: DemoPollingStatus = DemoPollingStatus.IDLE,
    val attemptCount: Int = 0,
    val screenVisibility: DemoScreenVisibility = DemoScreenVisibility.VISIBLE,
    val jobBehavior: DemoJobBehavior = DemoJobBehavior.FINISHES,
)

internal class VisibleScreenPollerDemoViewModel : ViewModel() {
    private val poller = VisibleScreenPoller()
    private val _state = MutableStateFlow(VisibleScreenPollerDemoState())

    val state: StateFlow<VisibleScreenPollerDemoState> = _state.asStateFlow()

    init {
        poller.changeVisibility(isVisible = true)
    }

    fun startPolling() {
        _state.update { it.copy(pollingStatus = DemoPollingStatus.POLLING, attemptCount = 0) }
        poller.start(
            scope = viewModelScope,
            schedule = PollingSchedule.fixedInterval(
                interval = DEMO_INTERVAL,
                maxAttempts = DEMO_MAX_ATTEMPTS,
                initialDelay = DEMO_INITIAL_DELAY,
            ),
            pollAttempt = ::checkDemoJob,
            onScheduleExhausted = { _state.update { it.copy(pollingStatus = DemoPollingStatus.SCHEDULE_EXHAUSTED) } },
        )
    }

    fun toggleScreenVisibility() {
        val isScreenVisibleAfterToggle = _state.value.screenVisibility == DemoScreenVisibility.HIDDEN
        val screenVisibility =
            if (isScreenVisibleAfterToggle) DemoScreenVisibility.VISIBLE else DemoScreenVisibility.HIDDEN
        _state.update { it.copy(screenVisibility = screenVisibility) }
        poller.changeVisibility(isVisible = isScreenVisibleAfterToggle)
    }

    fun toggleJobBehavior() {
        val isJobFinishing = _state.value.jobBehavior == DemoJobBehavior.FINISHES
        val jobBehavior = if (isJobFinishing) DemoJobBehavior.NEVER_FINISHES else DemoJobBehavior.FINISHES
        _state.update { it.copy(jobBehavior = jobBehavior) }
    }

    fun stopPolling() {
        poller.stop()
        _state.update { it.copy(pollingStatus = DemoPollingStatus.STOPPED) }
    }

    private fun checkDemoJob(): PollAttemptOutcome {
        val attemptCount = _state.value.attemptCount + 1
        val isDemoJobFinishing = _state.value.jobBehavior == DemoJobBehavior.FINISHES
        val isDemoJobDone = isDemoJobFinishing && attemptCount >= DEMO_ATTEMPTS_UNTIL_FINISHED
        val pollingStatus = if (isDemoJobDone) DemoPollingStatus.FINISHED else DemoPollingStatus.POLLING
        _state.update { it.copy(attemptCount = attemptCount, pollingStatus = pollingStatus) }

        return if (isDemoJobDone) PollAttemptOutcome.FINISHED else PollAttemptOutcome.CONTINUE
    }
}
