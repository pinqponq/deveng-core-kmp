package core.presentation.baseviewmodel.fake

import core.presentation.error.ErrorPrinter

class FakeErrorPrinter(private val prefix: String) : ErrorPrinter {
    override fun print(exception: Exception): String = "$prefix${exception.message}"
}
