package core.testing.fake

import core.presentation.baseviewmodel.BaseViewModel
import core.presentation.baseviewmodel.UiState
import core.presentation.baseviewmodel.ViewEvent
import core.presentation.baseviewmodel.ViewSideEffect
import core.presentation.baseviewmodel.launch
import kotlinx.coroutines.CoroutineDispatcher

data class FakeCounterUiState(val counterValue: Int = 0) : UiState

sealed interface FakeCounterEvent : ViewEvent {
    data class AddToCounter(val amount: Int) : FakeCounterEvent
    data object RequestCounterEffect : FakeCounterEvent
}

sealed interface FakeCounterEffect : ViewSideEffect {
    data class CounterChanged(val counterValue: Int) : FakeCounterEffect
}

class FakeCounterViewModel(
    private val dispatcher: CoroutineDispatcher,
    errorPrinter: FakeErrorPrinter
) : BaseViewModel<FakeCounterUiState, FakeCounterEvent, FakeCounterEffect>(
    initialState = FakeCounterUiState(),
    errorPrinter = errorPrinter
) {

    override fun handleEvents(event: FakeCounterEvent) {
        when (event) {
            is FakeCounterEvent.AddToCounter -> setState { copy(counterValue = counterValue + event.amount) }
            FakeCounterEvent.RequestCounterEffect -> setEffect {
                FakeCounterEffect.CounterChanged(counterValue = currentUiState.counterValue)
            }
        }
    }

    fun runFailingWork(exception: Exception, notifyError: Boolean) {
        launch(dispatcher = dispatcher, notifyError = notifyError) {
            throw exception
        }
    }
}
