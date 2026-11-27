package core.presentation.error

class ErrorPrinterImpl : ErrorPrinter {
    override fun print(exception: Exception): String {
        return exception.message.orEmpty()
    }
}
