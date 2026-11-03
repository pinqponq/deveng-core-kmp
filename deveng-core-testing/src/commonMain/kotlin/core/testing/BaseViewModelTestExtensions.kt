package core.testing

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import core.presentation.baseviewmodel.BaseViewModel
import core.presentation.baseviewmodel.UiState
import core.presentation.baseviewmodel.ViewEvent
import core.presentation.baseviewmodel.ViewSideEffect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

// The effect channel is a rendezvous channel: the effect waits in send until this collector reads it.
suspend fun <Event : ViewEvent, Effect : ViewSideEffect> BaseViewModel<*, Event, Effect>.awaitEffectAfterEvent(
    event: Event
): Effect {
    setEvent(event)
    return effect.first()
}

suspend fun <S : UiState> BaseViewModel<S, *, *>.testUiStates(
    validate: suspend ReceiveTurbine<S>.() -> Unit
) {
    stateFlow
        .map { state -> state.uiState }
        .distinctUntilChanged()
        .test(validate = validate)
}
