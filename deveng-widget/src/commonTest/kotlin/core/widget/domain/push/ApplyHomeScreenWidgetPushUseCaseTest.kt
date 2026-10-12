package core.widget.domain.push

import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ApplyHomeScreenWidgetPushUseCaseTest {
    private val applyHomeScreenWidgetPushUseCase = ApplyHomeScreenWidgetPushUseCase(pushUpdateTimeoutMillis = PUSH_UPDATE_TIMEOUT_MILLIS)

    @Test
    fun invoke_whenUpdateSucceeds_returnsAppliedAfterRunningTheUpdate() = runTest {
        var updateCount = 0

        val pushUpdateOutcome = applyHomeScreenWidgetPushUseCase { updateCount++ }

        assertIs<HomeScreenWidgetPushUpdateOutcome.Applied>(pushUpdateOutcome)
        assertEquals(1, updateCount)
    }

    @Test
    fun invoke_whenUpdateThrows_returnsFallbackWithTheException() = runTest {
        val pushUpdateOutcome = applyHomeScreenWidgetPushUseCase { throw IllegalArgumentException(UPDATE_FAILURE_MESSAGE) }

        assertIs<HomeScreenWidgetPushUpdateOutcome.FallbackRequired>(pushUpdateOutcome)
        val updateException = assertIs<IllegalArgumentException>(pushUpdateOutcome.updateException)
        assertEquals(UPDATE_FAILURE_MESSAGE, updateException.message)
    }

    @Test
    fun invoke_whenUpdateOutlastsTheTimeout_returnsFallback() = runTest {
        val pushUpdateOutcome = applyHomeScreenWidgetPushUseCase { awaitCancellation() }

        assertIs<HomeScreenWidgetPushUpdateOutcome.FallbackRequired>(pushUpdateOutcome)
    }

    private companion object {
        const val PUSH_UPDATE_TIMEOUT_MILLIS = 12_000L
        const val UPDATE_FAILURE_MESSAGE = "Widget push arrived without a widget id."
    }
}
