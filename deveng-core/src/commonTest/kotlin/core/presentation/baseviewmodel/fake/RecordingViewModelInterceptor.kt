package core.presentation.baseviewmodel.fake

import core.presentation.baseviewmodel.ErrorDecision
import core.presentation.baseviewmodel.ViewEvent
import core.presentation.baseviewmodel.ViewModelInterceptor
import core.presentation.baseviewmodel.ViewSideEffect

class RecordingViewModelInterceptor(
    private val interceptorName: String,
    private val callLog: MutableList<String>,
    private val errorDecision: ErrorDecision = ErrorDecision.SHOW
) : ViewModelInterceptor {

    override fun onEvent(viewModelName: String, event: ViewEvent) {
        callLog += "$interceptorName:event:$viewModelName:$event"
    }

    override fun onEffect(viewModelName: String, effect: ViewSideEffect) {
        callLog += "$interceptorName:effect:$viewModelName:$effect"
    }

    override fun onError(viewModelName: String, exception: Exception): ErrorDecision {
        callLog += "$interceptorName:error:$viewModelName:${exception.message}"
        return errorDecision
    }

    override fun onSilentError(viewModelName: String, exception: Exception) {
        callLog += "$interceptorName:silentError:$viewModelName:${exception.message}"
    }
}
