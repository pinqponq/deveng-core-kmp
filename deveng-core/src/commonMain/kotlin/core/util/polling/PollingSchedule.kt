package core.util.polling

import kotlin.time.Duration

/** Decides how long [VisibleScreenPoller] waits before each attempt. */
fun interface PollingSchedule {

    /** Measured from [VisibleScreenPoller.start]; a pause does not restart it. */
    val initialDelay: Duration
        get() = Duration.ZERO

    /** The wait before the next attempt, or null when the schedule has run out. */
    fun nextDelay(progress: PollProgress): Duration?

    companion object {

        /**
         * @param interval the wait between attempts, more than zero.
         * @param maxAttempts the number of attempts after which the schedule runs out, at least 1; null means no limit.
         */
        fun fixedInterval(
            interval: Duration,
            maxAttempts: Int? = null,
            initialDelay: Duration = Duration.ZERO,
        ): PollingSchedule {
            require(interval > Duration.ZERO) { "interval must be positive, was $interval." }
            require(maxAttempts == null || maxAttempts >= 1) { "maxAttempts must be at least 1, was $maxAttempts." }

            return FixedIntervalSchedule(
                interval = interval,
                maxAttempts = maxAttempts,
                initialDelay = initialDelay,
            )
        }

        /** Computes the next wait from the time since polling started; null ends the schedule. */
        fun fromElapsed(nextDelayForElapsed: (elapsed: Duration) -> Duration?): PollingSchedule =
            PollingSchedule { progress -> nextDelayForElapsed(progress.elapsed) }
    }
}

private class FixedIntervalSchedule(
    private val interval: Duration,
    private val maxAttempts: Int?,
    override val initialDelay: Duration,
) : PollingSchedule {

    override fun nextDelay(progress: PollProgress): Duration? {
        val isMaxAttemptsReached = maxAttempts != null && progress.attemptCount >= maxAttempts
        if (isMaxAttemptsReached) {
            return null
        }

        return interval
    }
}
