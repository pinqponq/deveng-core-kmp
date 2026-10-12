package core.testing

import core.presentation.baseviewmodel.ErrorDecision
import core.presentation.baseviewmodel.ViewEvent
import core.presentation.baseviewmodel.ViewModelInterceptor
import core.presentation.baseviewmodel.ViewModelInterceptors
import core.presentation.baseviewmodel.ViewSideEffect
import core.testing.fake.FakeCounterEffect
import core.testing.fake.FakeCounterEvent
import core.testing.fake.FakeCounterViewModel
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ViewModelTestEnvironmentTest {

    private val environment = ViewModelTestEnvironment()

    @AfterTest
    fun tearDown() {
        environment.stop()
    }

    @Test
    fun start_thenSetEvent_recordsEventWithViewModelNameAndHandlesItOnTestDispatcher() {
        environment.start()
        val viewModel = createViewModel()
        val event = FakeCounterEvent.AddToCounter(amount = ADDED_AMOUNT)

        viewModel.setEvent(event)

        assertEquals(listOf(RecordedViewModelCall<ViewEvent>(VIEW_MODEL_NAME, event)), environment.recordingInterceptor.recordedEvents)
        assertEquals(ADDED_AMOUNT, viewModel.currentUiState.counterValue)
    }

    @Test
    fun start_thenEffectSent_recordsEffect() = runTest {
        environment.start()
        val viewModel = createViewModel()

        viewModel.awaitEffectAfterEvent(FakeCounterEvent.RequestCounterEffect)

        assertEquals(
            listOf(RecordedViewModelCall<ViewSideEffect>(VIEW_MODEL_NAME, FakeCounterEffect.CounterChanged(counterValue = 0))),
            environment.recordingInterceptor.recordedEffects
        )
    }

    @Test
    fun start_thenShownError_recordsErrorAndShowsItWithFakeErrorPrinter() {
        environment.start()
        val viewModel = createViewModel()

        viewModel.runFailingWork(exception = FAILURE, notifyError = true)

        assertEquals(listOf(RecordedViewModelCall<Exception>(VIEW_MODEL_NAME, FAILURE)), environment.recordingInterceptor.recordedErrors)
        assertEquals(FAILURE_MESSAGE, viewModel.currentState.errorDescription)
        assertTrue(environment.recordingInterceptor.recordedSilentErrors.isEmpty())
    }

    @Test
    fun start_thenSilentError_recordsSilentErrorWithoutShowingIt() {
        environment.start()
        val viewModel = createViewModel()

        viewModel.runFailingWork(exception = FAILURE, notifyError = false)

        assertEquals(listOf(RecordedViewModelCall<Exception>(VIEW_MODEL_NAME, FAILURE)), environment.recordingInterceptor.recordedSilentErrors)
        assertTrue(environment.recordingInterceptor.recordedErrors.isEmpty())
        assertNull(viewModel.currentState.error)
    }

    @Test
    fun start_withAdditionalHandlingInterceptor_keepsRecordingAndHidesError() {
        environment.start(additionalInterceptors = listOf(RecordingInterceptor(errorDecision = ErrorDecision.HANDLED)))
        val viewModel = createViewModel()

        viewModel.runFailingWork(exception = FAILURE, notifyError = true)

        assertEquals(1, environment.recordingInterceptor.recordedErrors.size)
        assertNull(viewModel.currentState.error)
    }

    @Test
    fun start_withInterceptorLeftByEarlierTest_replacesRegistry() {
        val leftoverInterceptor = object : ViewModelInterceptor {}
        ViewModelInterceptors.register(leftoverInterceptor)

        environment.start()

        assertEquals(listOf<ViewModelInterceptor>(environment.recordingInterceptor), ViewModelInterceptors.registeredInterceptors)
    }

    @Test
    fun stop_afterRecordedCalls_emptiesRegistryAndRecordings() {
        environment.start()
        createViewModel().setEvent(FakeCounterEvent.AddToCounter(amount = ADDED_AMOUNT))

        environment.stop()

        assertTrue(ViewModelInterceptors.registeredInterceptors.isEmpty())
        assertTrue(environment.recordingInterceptor.recordedEvents.isEmpty())
    }

    private fun createViewModel() = FakeCounterViewModel(
        dispatcher = environment.testDispatcher,
        errorPrinter = environment.errorPrinter
    )

    private companion object {
        const val VIEW_MODEL_NAME = "FakeCounterViewModel"
        const val ADDED_AMOUNT = 3
        const val FAILURE_MESSAGE = "upload failed"
        val FAILURE = IllegalStateException(FAILURE_MESSAGE)
    }
}
