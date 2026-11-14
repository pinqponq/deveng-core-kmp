package core.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val DEFAULT_TICK_DELAY_MILLIS = 1_000L

/**
 * A countdown that ticks once per second, for example the wait before a verification code can be sent again.
 * It has no text or UI: the caller formats [remainingSeconds] itself.
 *
 * @param scope scope that runs the countdown; the countdown ends when it is cancelled.
 * @param tickDelayMillis time between two ticks; tests pass the virtual time of their dispatcher.
 */
class CooldownTimer(
    private val scope: CoroutineScope,
    private val tickDelayMillis: Long = DEFAULT_TICK_DELAY_MILLIS,
) {
    private val _remainingSeconds = MutableStateFlow(value = 0)
    private val _isRunning = MutableStateFlow(value = false)
    private var countdownJob: Job? = null

    /** Seconds left; 0 when idle or finished. */
    val remainingSeconds: StateFlow<Int> = _remainingSeconds.asStateFlow()

    /** True from [start] until the countdown reaches 0 or [cancel] is called. */
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    /**
     * Starts counting down from [seconds]; a running countdown is cancelled first.
     * Zero or a negative value finishes at once.
     */
    fun start(seconds: Int) {
        countdownJob?.cancel()
        _remainingSeconds.value = seconds.coerceAtLeast(0)
        if (_remainingSeconds.value == 0) {
            _isRunning.value = false
            countdownJob = null
            return
        }
        _isRunning.value = true
        countdownJob = scope.launch {
            while (_remainingSeconds.value > 0) {
                delay(tickDelayMillis)
                _remainingSeconds.value -= 1
            }
            _isRunning.value = false
        }
    }

    /** Stops the countdown and sets [remainingSeconds] to 0. */
    fun cancel() {
        countdownJob?.cancel()
        countdownJob = null
        _remainingSeconds.value = 0
        _isRunning.value = false
    }
}
