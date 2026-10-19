package core.presentation.baseviewmodel

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import core.presentation.error.ErrorPrinter
import core.presentation.error.ErrorPrinterImpl
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

abstract class BaseViewModel<
    S : UiState,
    Event : ViewEvent,
    Effect : ViewSideEffect>(
    initialState: S,
    private val errorPrinter: ErrorPrinter = ErrorPrinterImpl()
) : ViewModel() {

    private val viewModelName: String = this::class.simpleName ?: UNKNOWN_VIEW_MODEL_NAME

    private var hasStartedInitialLoad = false

    private val _stateFlow = MutableStateFlow(State(initialState))
    val stateFlow: StateFlow<State<S>> get() = _stateFlow

    val currentUiState: S get() = _stateFlow.value.uiState

    val currentState: State<S> get() = _stateFlow.value

    private val _event: MutableSharedFlow<Event> = MutableSharedFlow()

    private val _effect: Channel<Effect> = Channel()
    val effect = _effect.receiveAsFlow()

    init {
        subscribeToEvents()
    }

    abstract fun handleEvents(event: Event)

    /**
     * Claims the initial load for this ViewModel instance: true on the first call, false afterwards.
     *
     * The flag deliberately lives here and nowhere else. It guards data held by this ViewModel, so
     * it has to die with that data: a flag kept in saved state outlives the process while the data
     * does not.
     */
    internal fun markInitialLoadStarted(): Boolean {
        if (hasStartedInitialLoad) return false

        hasStartedInitialLoad = true
        return true
    }

    fun setEvent(event: Event) {
        ViewModelInterceptors.registeredInterceptors.forEach { interceptor ->
            interceptor.onEvent(viewModelName = viewModelName, event = event)
        }

        viewModelScope.launch { _event.emit(event) }
    }

    fun setState(reducer: S.() -> S) {
        _stateFlow.update { it.copy(uiState = reducer(it.uiState)) }
    }

    fun setProgress(showProgress: Boolean) {
        _stateFlow.update { it.copy(isLoading = showProgress) }
    }

    fun setError(exception: Exception) {
        val errorDecision = decideError(exception = exception)
        if (errorDecision == ErrorDecision.HANDLED) return

        Logger.e(exception) { exception.message.orEmpty() }
        if (errorDecision == ErrorDecision.SHOW) {
            showError(exception = exception)
        }
    }

    fun hideError() {
        _stateFlow.update { it.copy(error = null, errorDescription = null) }
    }

    @Composable
    fun getState(): State<S> {
        return stateFlow.collectAsState().value
    }

    protected fun setEffect(builder: () -> Effect) {
        val effectValue = builder()
        ViewModelInterceptors.registeredInterceptors.forEach { interceptor ->
            interceptor.onEffect(viewModelName = viewModelName, effect = effectValue)
        }

        viewModelScope.launch { _effect.send(effectValue) }
    }

    internal fun notifySilentError(exception: Exception) {
        ViewModelInterceptors.registeredInterceptors.forEach { interceptor ->
            interceptor.onSilentError(viewModelName = viewModelName, exception = exception)
        }
    }

    private fun subscribeToEvents() {
        viewModelScope.launch {
            _event.collect {
                handleEvents(it)
            }
        }
    }

    private fun decideError(exception: Exception): ErrorDecision {
        val errorDecisionList = ViewModelInterceptors.registeredInterceptors.map { interceptor ->
            interceptor.onError(viewModelName = viewModelName, exception = exception)
        }

        return errorDecisionList.maxByOrNull { it.ordinal } ?: ErrorDecision.SHOW
    }

    private fun showError(exception: Exception) {
        _stateFlow.update {
            it.copy(
                error = exception,
                errorDescription = errorPrinter.print(exception)
            )
        }
    }
}

interface ViewEvent

data class State<T : UiState>(
    val uiState: T,
    val isLoading: Boolean = false,
    val error: Exception? = null,
    val errorDescription: String? = null
) {
    val showError get() = error != null
}

interface UiState

interface ViewSideEffect

const val SIDE_EFFECTS_KEY = "side-effects_key"

private const val UNKNOWN_VIEW_MODEL_NAME = "UnknownViewModel"
