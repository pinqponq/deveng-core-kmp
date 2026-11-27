package core.testing.fake

import core.presentation.error.ErrorPrinter

class FakeErrorPrinter : ErrorPrinter {
    override fun print(exception: Exception): String = exception.message.orEmpty()
}
