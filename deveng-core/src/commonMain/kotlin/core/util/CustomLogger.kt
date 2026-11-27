package core.util

object CustomLogger {
    var isLoggingEnabled: Boolean = false

    fun log(message: String) {
        if (isLoggingEnabled) {
            println(message)
        }
    }
}