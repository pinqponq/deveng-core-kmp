package core.widget.domain.push

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TestTimeSource

class ApplyHomeScreenWidgetPushUseCaseTest {
    private val elapsedTimeSource = TestTimeSource()
    private val applyHomeScreenWidgetPushUseCase = ApplyHomeScreenWidgetPushUseCase(
        pushUpdateTimeoutMilliseconds = PUSH_UPDATE_TIMEOUT_MILLISECONDS,
        elapsedTimeSource = elapsedTimeSource,
    )

    @Test
    fun invoke_whenUpdateSucceeds_returnsAppliedAfterRunningTheUpdate() = runTest {
        var updateCount = 0

        val pushUpdateOutcome = applyHomeScreenWidgetPushUseCase {
            updateCount++
            elapsedTimeSource += UPDATE_DURATION_MILLISECONDS.milliseconds
        }

        assertEquals(HomeScreenWidgetPushUpdateOutcome.Applied(elapsedMilliseconds = UPDATE_DURATION_MILLISECONDS), pushUpdateOutcome)
        assertEquals(1, updateCount)
    }

    @Test
    fun invoke_whenUpdateThrows_returnsFallbackWithTheException() = runTest {
        val pushUpdateOutcome = applyHomeScreenWidgetPushUseCase {
            elapsedTimeSource += UPDATE_DURATION_MILLISECONDS.milliseconds
            throw IllegalArgumentException(UPDATE_FAILURE_MESSAGE)
        }

        assertIs<HomeScreenWidgetPushUpdateOutcome.FallbackRequired>(pushUpdateOutcome)
        val updateException = assertIs<IllegalArgumentException>(pushUpdateOutcome.updateException)
        assertEquals(UPDATE_FAILURE_MESSAGE, updateException.message)
        assertEquals(UPDATE_DURATION_MILLISECONDS, pushUpdateOutcome.elapsedMilliseconds)
    }

    @Test
    fun invoke_whenUpdateOutlastsTheTimeout_returnsFallback() = runTest {
        val pushUpdateOutcome = applyHomeScreenWidgetPushUseCase { awaitCancellation() }

        assertIs<HomeScreenWidgetPushUpdateOutcome.FallbackRequired>(pushUpdateOutcome)
    }

    @Test
    fun invoke_whenCallerIsCancelled_rethrowsInsteadOfFallingBack() = runTest {
        assertFailsWith<CancellationException> {
            applyHomeScreenWidgetPushUseCase { throw CancellationException(CANCELLATION_MESSAGE) }
        }
    }

    private companion object {
        const val PUSH_UPDATE_TIMEOUT_MILLISECONDS = 12_000L
        const val UPDATE_DURATION_MILLISECONDS = 250L
        const val CANCELLATION_MESSAGE = "push scope cancelled"
        const val UPDATE_FAILURE_MESSAGE = "Widget push arrived without a widget id."
    }
}
