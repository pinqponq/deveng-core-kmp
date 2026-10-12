package core.testing

import core.presentation.baseviewmodel.ErrorDecision
import core.presentation.baseviewmodel.ViewEvent
import core.presentation.baseviewmodel.ViewModelInterceptor
import core.presentation.baseviewmodel.ViewSideEffect

class RecordingInterceptor(
    private val errorDecision: ErrorDecision = ErrorDecision.SHOW
) : ViewModelInterceptor {

    private val recordedEventList = mutableListOf<RecordedViewModelCall<ViewEvent>>()
    private val recordedEffectList = mutableListOf<RecordedViewModelCall<ViewSideEffect>>()
    private val recordedErrorList = mutableListOf<RecordedViewModelCall<Exception>>()
    private val recordedSilentErrorList = mutableListOf<RecordedViewModelCall<Exception>>()

    val recordedEvents: List<RecordedViewModelCall<ViewEvent>> get() = recordedEventList
    val recordedEffects: List<RecordedViewModelCall<ViewSideEffect>> get() = recordedEffectList
    val recordedErrors: List<RecordedViewModelCall<Exception>> get() = recordedErrorList
    val recordedSilentErrors: List<RecordedViewModelCall<Exception>> get() = recordedSilentErrorList

    override fun onEvent(viewModelName: String, event: ViewEvent) {
        recordedEventList += RecordedViewModelCall(viewModelName = viewModelName, value = event)
    }

    override fun onEffect(viewModelName: String, effect: ViewSideEffect) {
        recordedEffectList += RecordedViewModelCall(viewModelName = viewModelName, value = effect)
    }

    override fun onError(viewModelName: String, exception: Exception): ErrorDecision {
        recordedErrorList += RecordedViewModelCall(viewModelName = viewModelName, value = exception)
        return errorDecision
    }

    override fun onSilentError(viewModelName: String, exception: Exception) {
        recordedSilentErrorList += RecordedViewModelCall(viewModelName = viewModelName, value = exception)
    }

    fun clear() {
        recordedEventList.clear()
        recordedEffectList.clear()
        recordedErrorList.clear()
        recordedSilentErrorList.clear()
    }
}

data class RecordedViewModelCall<T>(
    val viewModelName: String,
    val value: T
)
