package core.util.polling

import kotlin.time.Duration

/**
 * Where a polling session stands after an attempt.
 *
 * @property elapsed time since [VisibleScreenPoller.start]; time while the screen was hidden counts too.
 * @property attemptCount attempts made so far, failed attempts included.
 */
data class PollProgress(
    val elapsed: Duration,
    val attemptCount: Int,
)
