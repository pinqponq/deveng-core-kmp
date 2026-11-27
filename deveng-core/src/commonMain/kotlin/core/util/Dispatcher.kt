package core.util

@Retention(AnnotationRetention.RUNTIME)
annotation class Dispatcher(val customDispatcher: CustomDispatchers)

enum class CustomDispatchers {
    Default,
    IO
}