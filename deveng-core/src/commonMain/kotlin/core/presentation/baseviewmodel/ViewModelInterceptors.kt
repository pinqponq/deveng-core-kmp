package core.presentation.baseviewmodel

/**
 * App-wide list of [ViewModelInterceptor]s every [BaseViewModel] calls. Register them once at app
 * start, before the first ViewModel is created; the list is not guarded for concurrent changes.
 */
object ViewModelInterceptors {
    var registeredInterceptors: List<ViewModelInterceptor> = emptyList()
        private set

    fun register(interceptor: ViewModelInterceptor) {
        registeredInterceptors = registeredInterceptors + interceptor
    }

    fun unregisterAll() {
        registeredInterceptors = emptyList()
    }
}
