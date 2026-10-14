package core.presentation.baseviewmodel.fake

import core.presentation.baseviewmodel.BaseViewModel
import core.presentation.baseviewmodel.UiState
import core.presentation.baseviewmodel.ViewEvent
import core.presentation.baseviewmodel.ViewSideEffect
import core.presentation.baseviewmodel.launch
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.awaitCancellation

data class FakeUiState(val counterValue: Int = 0, val handledEventList: List<String> = emptyList()) : UiState

sealed interface FakeEvent : ViewEvent {
    data class AddToCounter(val amount: Int) : FakeEvent
    data object RequestEffect : FakeEvent
}

sealed interface FakeEffect : ViewSideEffect {
    data object CounterChanged : FakeEffect
}

class FakeViewModel(private val dispatcher: CoroutineDispatcher) :
    BaseViewModel<FakeUiState, FakeEvent, FakeEffect>(initialState = FakeUiState()) {

    override fun handleEvents(event: FakeEvent) {
        when (event) {
            is FakeEvent.AddToCounter -> setState {
                copy(counterValue = counterValue + event.amount, handledEventList = handledEventList + event.toString())
            }

            FakeEvent.RequestEffect -> setEffect { FakeEffect.CounterChanged }
        }
    }

    fun runFailingWork(exception: Exception, notifyError: Boolean) {
        launch(dispatcher = dispatcher, notifyError = notifyError) {
            throw exception
        }
    }

    fun runEndlessWork() = launch(dispatcher = dispatcher) {
        awaitCancellation()
    }
}
