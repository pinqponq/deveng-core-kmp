package core.testing

import core.testing.fake.FakeCounterEffect
import core.testing.fake.FakeCounterEvent
import core.testing.fake.FakeCounterUiState
import core.testing.fake.FakeCounterViewModel
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class BaseViewModelTestExtensionsTest {

    private val environment = ViewModelTestEnvironment()

    @BeforeTest
    fun setUp() {
        environment.start()
    }

    @AfterTest
    fun tearDown() {
        environment.stop()
    }

    @Test
    fun awaitEffectAfterEvent_withEffectSendingEvent_returnsThatEffect() = runTest {
        val viewModel = createViewModel()
        viewModel.setEvent(FakeCounterEvent.AddToCounter(amount = ADDED_AMOUNT))

        val receivedEffect = viewModel.awaitEffectAfterEvent(FakeCounterEvent.RequestCounterEffect)

        assertEquals(FakeCounterEffect.CounterChanged(counterValue = ADDED_AMOUNT), receivedEffect)
    }

    @Test
    fun testUiStates_withRepeatedEqualState_emitsEachDistinctUiStateOnce() = runTest {
        val viewModel = createViewModel()

        viewModel.testUiStates {
            assertEquals(FakeCounterUiState(counterValue = 0), awaitItem())
            viewModel.setEvent(FakeCounterEvent.AddToCounter(amount = 0))
            viewModel.setProgress(showProgress = true)
            viewModel.setEvent(FakeCounterEvent.AddToCounter(amount = ADDED_AMOUNT))
            assertEquals(FakeCounterUiState(counterValue = ADDED_AMOUNT), awaitItem())
        }
    }

    private fun createViewModel() = FakeCounterViewModel(
        dispatcher = environment.testDispatcher,
        errorPrinter = environment.errorPrinter
    )

    private companion object {
        const val ADDED_AMOUNT = 2
    }
}
