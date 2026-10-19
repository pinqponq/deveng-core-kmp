package core.presentation.baseviewmodel.fake

import core.presentation.baseviewmodel.BaseViewModel
import core.presentation.baseviewmodel.UiState
import core.presentation.baseviewmodel.ViewEvent
import core.presentation.baseviewmodel.ViewSideEffect
import core.presentation.baseviewmodel.launch
import core.presentation.error.ErrorPrinter
import core.presentation.error.ErrorPrinterImpl
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

class FakeViewModel(
    private val dispatcher: CoroutineDispatcher,
    errorPrinter: ErrorPrinter = ErrorPrinterImpl()
) : BaseViewModel<FakeUiState, FakeEvent, FakeEffect>(initialState = FakeUiState(), errorPrinter = errorPrinter) {

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

    fun runFailingWorkWithCallbacks(exception: Exception, callbackLog: MutableList<String>) {
        launch(
            dispatcher = dispatcher,
            onError = { callbackLog += "onError:${it.message}:showError=${currentState.showError}" },
            onCompleted = { callbackLog += "onCompleted:isLoading=${currentState.isLoading}" }
        ) {
            throw exception
        }
    }

    fun runEndlessWork() = launch(dispatcher = dispatcher) {
        awaitCancellation()
    }
}
