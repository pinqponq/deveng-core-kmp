package core.presentation.baseviewmodel

import app.cash.turbine.test
import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import co.touchlab.kermit.platformLogWriter
import core.presentation.baseviewmodel.fake.FakeEffect
import core.presentation.baseviewmodel.fake.FakeEvent
import core.presentation.baseviewmodel.fake.FakeViewModel
import core.presentation.baseviewmodel.fake.RecordingLogWriter
import core.presentation.baseviewmodel.fake.RecordingViewModelInterceptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class BaseViewModelTest {
    private val testDispatcher: TestDispatcher = UnconfinedTestDispatcher()
    private val callLog = mutableListOf<String>()
    private val logWriter = RecordingLogWriter()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        Logger.setLogWriters(logWriter)
    }

    @AfterTest
    fun tearDown() {
        ViewModelInterceptors.unregisterAll()
        Logger.setLogWriters(platformLogWriter())
        Dispatchers.resetMain()
    }

    @Test
    fun setEvent_withTwoInterceptors_notifiesBothInRegistrationOrderAndHandlesEvent() {
        registerInterceptors(FIRST_INTERCEPTOR, SECOND_INTERCEPTOR)
        val viewModel = FakeViewModel(dispatcher = testDispatcher)
        val event = FakeEvent.AddToCounter(amount = 2)

        viewModel.setEvent(event)

        assertEquals(
            listOf(
                "$FIRST_INTERCEPTOR:event:$VIEW_MODEL_NAME:$event",
                "$SECOND_INTERCEPTOR:event:$VIEW_MODEL_NAME:$event"
            ),
            callLog
        )
        assertEquals(2, viewModel.currentUiState.counterValue)
    }

    @Test
    fun setEvent_manyEventsInARow_appliesStateInEventOrder() {
        val viewModel = FakeViewModel(dispatcher = testDispatcher)
        val eventList = (1..EVENT_COUNT).map { FakeEvent.AddToCounter(amount = it) }

        eventList.forEach(viewModel::setEvent)

        assertEquals(eventList.map { it.toString() }, viewModel.currentUiState.handledEventList)
        assertEquals(eventList.sumOf { it.amount }, viewModel.currentUiState.counterValue)
    }

    @Test
    fun setState_calledTwice_isVisibleImmediatelyAfterEachCall() {
        val viewModel = FakeViewModel(dispatcher = testDispatcher)

        viewModel.setState { copy(counterValue = 1) }
        val counterAfterFirstUpdate = viewModel.currentUiState.counterValue
        viewModel.setState { copy(counterValue = counterValue * 10) }

        assertEquals(1, counterAfterFirstUpdate)
        assertEquals(10, viewModel.currentUiState.counterValue)
    }

    @Test
    fun setEffect_withInterceptor_notifiesInterceptorAndDeliversEffect() = runTest {
        registerInterceptors(FIRST_INTERCEPTOR)
        val viewModel = FakeViewModel(dispatcher = testDispatcher)

        viewModel.effect.test {
            viewModel.setEvent(FakeEvent.RequestEffect)

            assertEquals(FakeEffect.CounterChanged, awaitItem())
        }
        assertEquals(
            "$FIRST_INTERCEPTOR:effect:$VIEW_MODEL_NAME:${FakeEffect.CounterChanged}",
            callLog.last()
        )
    }

    @Test
    fun setError_withoutInterceptors_showsErrorWithPrintedMessage() {
        val viewModel = FakeViewModel(dispatcher = testDispatcher)
        val exception = IllegalStateException(ERROR_MESSAGE)

        viewModel.setError(exception)

        assertEquals(exception, viewModel.currentState.error)
        assertEquals(ERROR_MESSAGE, viewModel.currentState.errorDescription)
    }

    @Test
    fun setError_withAllInterceptorsShowing_showsError() {
        registerInterceptors(FIRST_INTERCEPTOR, SECOND_INTERCEPTOR)
        val viewModel = FakeViewModel(dispatcher = testDispatcher)

        viewModel.setError(IllegalStateException(ERROR_MESSAGE))

        assertTrue(viewModel.currentState.showError)
    }

    @Test
    fun setError_whenOneInterceptorHandlesIt_hidesErrorAndStillNotifiesEveryInterceptor() {
        registerInterceptors(FIRST_INTERCEPTOR to ErrorDecision.HANDLED, SECOND_INTERCEPTOR to ErrorDecision.SHOW)
        val viewModel = FakeViewModel(dispatcher = testDispatcher)

        viewModel.setError(IllegalStateException(ERROR_MESSAGE))

        assertFalse(viewModel.currentState.showError)
        assertNull(viewModel.currentState.errorDescription)
        assertEquals(
            listOf(
                "$FIRST_INTERCEPTOR:error:$VIEW_MODEL_NAME:$ERROR_MESSAGE",
                "$SECOND_INTERCEPTOR:error:$VIEW_MODEL_NAME:$ERROR_MESSAGE"
            ),
            callLog
        )
    }

    @Test
    fun setError_whenOneInterceptorSilencesIt_hidesErrorAndLogsIt() {
        registerInterceptors(FIRST_INTERCEPTOR to ErrorDecision.SHOW, SECOND_INTERCEPTOR to ErrorDecision.SILENT)
        val viewModel = FakeViewModel(dispatcher = testDispatcher)

        viewModel.setError(IllegalStateException(ERROR_MESSAGE))

        assertFalse(viewModel.currentState.showError)
        assertEquals(listOf(Severity.Error to ERROR_MESSAGE), logWriter.loggedMessageList)
    }

    @Test
    fun setError_whenOneInterceptorHandlesIt_doesNotLogIt() {
        registerInterceptors(FIRST_INTERCEPTOR to ErrorDecision.SILENT, SECOND_INTERCEPTOR to ErrorDecision.HANDLED)
        val viewModel = FakeViewModel(dispatcher = testDispatcher)

        viewModel.setError(IllegalStateException(ERROR_MESSAGE))

        assertTrue(logWriter.loggedMessageList.isEmpty())
    }

    @Test
    fun setError_withInterceptorOverridingNothing_showsError() {
        ViewModelInterceptors.register(object : ViewModelInterceptor {})
        val viewModel = FakeViewModel(dispatcher = testDispatcher)

        viewModel.setEvent(FakeEvent.AddToCounter(amount = 1))
        viewModel.setError(IllegalStateException(ERROR_MESSAGE))

        assertEquals(1, viewModel.currentUiState.counterValue)
        assertTrue(viewModel.currentState.showError)
    }

    @Test
    fun hideError_afterShownError_clearsErrorAndDescription() {
        val viewModel = FakeViewModel(dispatcher = testDispatcher)
        viewModel.setError(IllegalStateException(ERROR_MESSAGE))

        viewModel.hideError()

        assertNull(viewModel.currentState.error)
        assertNull(viewModel.currentState.errorDescription)
    }

    @Test
    fun launch_failingWithNotifyError_showsErrorWithoutSilentErrorAndStopsProgress() {
        registerInterceptors(FIRST_INTERCEPTOR)
        val viewModel = FakeViewModel(dispatcher = testDispatcher)

        viewModel.runFailingWork(exception = IllegalStateException(ERROR_MESSAGE), notifyError = true)

        assertTrue(viewModel.currentState.showError)
        assertFalse(viewModel.currentState.isLoading)
        assertEquals(listOf("$FIRST_INTERCEPTOR:error:$VIEW_MODEL_NAME:$ERROR_MESSAGE"), callLog)
    }

    @Test
    fun launch_failingWithoutNotifyError_reportsSilentErrorWithoutShowingIt() {
        registerInterceptors(FIRST_INTERCEPTOR, SECOND_INTERCEPTOR)
        val viewModel = FakeViewModel(dispatcher = testDispatcher)

        viewModel.runFailingWork(exception = IllegalStateException(ERROR_MESSAGE), notifyError = false)

        assertFalse(viewModel.currentState.showError)
        assertFalse(viewModel.currentState.isLoading)
        assertEquals(
            listOf(
                "$FIRST_INTERCEPTOR:silentError:$VIEW_MODEL_NAME:$ERROR_MESSAGE",
                "$SECOND_INTERCEPTOR:silentError:$VIEW_MODEL_NAME:$ERROR_MESSAGE"
            ),
            callLog
        )
        assertEquals(listOf(Severity.Error to ERROR_MESSAGE), logWriter.loggedMessageList)
    }

    @Test
    fun launch_cancelled_neitherShowsNorReportsError() {
        registerInterceptors(FIRST_INTERCEPTOR)
        val viewModel = FakeViewModel(dispatcher = testDispatcher)
        val job = viewModel.runEndlessWork()
        val isLoadingWhileRunning = viewModel.currentState.isLoading

        job.cancel()

        assertTrue(isLoadingWhileRunning)
        assertFalse(viewModel.currentState.isLoading)
        assertFalse(viewModel.currentState.showError)
        assertTrue(callLog.isEmpty())
    }

    @Test
    fun markInitialLoadStarted_calledTwice_claimsOnlyTheFirstTime() {
        val viewModel = FakeViewModel(dispatcher = testDispatcher)

        val firstClaim = viewModel.markInitialLoadStarted()
        val secondClaim = viewModel.markInitialLoadStarted()

        assertTrue(firstClaim)
        assertFalse(secondClaim)
    }

    private fun registerInterceptors(vararg interceptorNames: String) {
        registerInterceptors(*interceptorNames.map { it to ErrorDecision.SHOW }.toTypedArray())
    }

    private fun registerInterceptors(vararg interceptorDecisions: Pair<String, ErrorDecision>) {
        interceptorDecisions.forEach { (interceptorName, errorDecision) ->
            ViewModelInterceptors.register(
                RecordingViewModelInterceptor(
                    interceptorName = interceptorName,
                    callLog = callLog,
                    errorDecision = errorDecision
                )
            )
        }
    }

    private companion object {
        const val FIRST_INTERCEPTOR = "first"
        const val SECOND_INTERCEPTOR = "second"
        const val VIEW_MODEL_NAME = "FakeViewModel"
        const val ERROR_MESSAGE = "request failed"
        const val EVENT_COUNT = 50
    }
}
