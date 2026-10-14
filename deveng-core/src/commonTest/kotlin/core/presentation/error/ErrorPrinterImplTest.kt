package core.presentation.error

import kotlin.test.Test
import kotlin.test.assertEquals

class ErrorPrinterImplTest {
    private val errorPrinter = ErrorPrinterImpl()

    @Test
    fun print_exceptionWithMessage_returnsMessage() {
        assertEquals(ERROR_MESSAGE, errorPrinter.print(IllegalStateException(ERROR_MESSAGE)))
    }

    @Test
    fun print_exceptionWithoutMessage_returnsEmptyText() {
        assertEquals("", errorPrinter.print(IllegalStateException()))
    }

    private companion object {
        const val ERROR_MESSAGE = "request failed"
    }
}
