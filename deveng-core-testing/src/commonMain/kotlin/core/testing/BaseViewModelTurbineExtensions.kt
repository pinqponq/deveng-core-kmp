package core.testing

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import core.presentation.baseviewmodel.BaseViewModel
import core.presentation.baseviewmodel.UiState
import core.presentation.baseviewmodel.ViewEvent
import core.presentation.baseviewmodel.ViewSideEffect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

suspend fun <Event : ViewEvent, Effect : ViewSideEffect> BaseViewModel<*, Event, Effect>.awaitEffectAfterEvent(
    event: Event
): Effect {
    lateinit var receivedEffect: Effect
    effect.test {
        setEvent(event)
        receivedEffect = awaitItem()
        cancelAndIgnoreRemainingEvents()
    }
    return receivedEffect
}

suspend fun <S : UiState> BaseViewModel<S, *, *>.testUiStates(
    validate: suspend ReceiveTurbine<S>.() -> Unit
) {
    stateFlow
        .map { state -> state.uiState }
        .distinctUntilChanged()
        .test(validate = validate)
}
