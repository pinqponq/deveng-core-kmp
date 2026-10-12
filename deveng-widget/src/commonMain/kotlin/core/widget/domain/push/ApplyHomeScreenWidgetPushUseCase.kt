package core.widget.domain.push

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import kotlin.time.TimeMark
import kotlin.time.TimeSource

/**
 * Updates the widget straight from the push callback, bounded by [pushUpdateTimeoutMilliseconds]. On a Samsung with
 * Android 16, background work enqueued from a push (WorkManager, a Glance session) was held back until the app
 * was opened again, so the widget is drawn inside the callback and the background refresh is only the fallback.
 */
class ApplyHomeScreenWidgetPushUseCase(
    private val pushUpdateTimeoutMilliseconds: Long,
    private val elapsedTimeSource: TimeSource = TimeSource.Monotonic,
) {
    suspend operator fun invoke(updateWidget: suspend () -> Unit): HomeScreenWidgetPushUpdateOutcome {
        val updateStartMark = elapsedTimeSource.markNow()
        return try {
            withTimeout(pushUpdateTimeoutMilliseconds) { updateWidget() }
            HomeScreenWidgetPushUpdateOutcome.Applied(elapsedMilliseconds = updateStartMark.elapsedNow().inWholeMilliseconds)
        } catch (timeoutException: TimeoutCancellationException) {
            buildFallbackOutcome(updateStartMark = updateStartMark, updateException = timeoutException)
        } catch (cancellationException: CancellationException) {
            throw cancellationException
        } catch (updateException: Exception) {
            buildFallbackOutcome(updateStartMark = updateStartMark, updateException = updateException)
        }
    }

    private fun buildFallbackOutcome(
        updateStartMark: TimeMark,
        updateException: Exception,
    ): HomeScreenWidgetPushUpdateOutcome.FallbackRequired {
        return HomeScreenWidgetPushUpdateOutcome.FallbackRequired(
            elapsedMilliseconds = updateStartMark.elapsedNow().inWholeMilliseconds,
            updateException = updateException,
        )
    }
}
