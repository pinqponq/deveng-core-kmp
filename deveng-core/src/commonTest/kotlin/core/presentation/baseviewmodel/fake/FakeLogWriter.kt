package core.presentation.baseviewmodel.fake

import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Severity

class FakeLogWriter : LogWriter() {
    val loggedMessageList = mutableListOf<Pair<Severity, String>>()

    override fun log(severity: Severity, message: String, tag: String, throwable: Throwable?) {
        loggedMessageList += severity to message
    }
}
