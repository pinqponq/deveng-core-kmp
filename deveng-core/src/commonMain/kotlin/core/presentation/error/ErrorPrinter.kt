package core.presentation.error

interface ErrorPrinter {
    fun print(exception: Exception): String
}
