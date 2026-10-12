package core.widget.domain.push

import kotlinx.coroutines.withTimeout
import kotlin.time.TimeSource

/**
 * Updates the widget straight from the push callback, bounded by [pushUpdateTimeoutMillis]. On a Samsung with
 * Android 16, background work enqueued from a push (WorkManager, a Glance session) was held back until the app
 * was opened again, so the widget is drawn inside the callback and the background refresh is only the fallback.
 */
class ApplyHomeScreenWidgetPushUseCase(
    private val pushUpdateTimeoutMillis: Long,
) {
    suspend operator fun invoke(updateWidget: suspend () -> Unit): HomeScreenWidgetPushUpdateOutcome {
        val updateStartMark = TimeSource.Monotonic.markNow()
        return try {
            withTimeout(pushUpdateTimeoutMillis) { updateWidget() }
            HomeScreenWidgetPushUpdateOutcome.Applied(elapsedMillis = updateStartMark.elapsedNow().inWholeMilliseconds)
        } catch (updateException: Exception) {
            HomeScreenWidgetPushUpdateOutcome.FallbackRequired(
                elapsedMillis = updateStartMark.elapsedNow().inWholeMilliseconds,
                updateException = updateException,
            )
        }
    }
}
