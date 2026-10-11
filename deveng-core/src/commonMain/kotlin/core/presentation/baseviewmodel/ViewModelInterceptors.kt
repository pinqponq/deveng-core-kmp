package core.presentation.baseviewmodel

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
