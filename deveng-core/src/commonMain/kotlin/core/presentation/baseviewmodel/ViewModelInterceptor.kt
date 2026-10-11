package core.presentation.baseviewmodel

/**
 * App-level rule plugged into every [BaseViewModel] through [ViewModelInterceptors], so the base
 * class stays free of logging, analytics and session code.
 */
interface ViewModelInterceptor {
    fun onEvent(viewModelName: String, event: ViewEvent) {}

    fun onEffect(viewModelName: String, effect: ViewSideEffect) {}

    fun onError(viewModelName: String, exception: Exception): ErrorDecision = ErrorDecision.SHOW

    /**
     * Called for failures the user is not shown, for example a `launch` with `notifyError = false`.
     */
    fun onSilentError(viewModelName: String, exception: Exception) {}
}

/**
 * What [BaseViewModel.setError] does with an error. When several interceptors answer, the one
 * declared last here wins, so a single [HANDLED] hides the dialog whatever the others return.
 */
enum class ErrorDecision {
    SHOW,
    SILENT,
    HANDLED
}
