package core.push.fake

import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Severity

class RecordingLogWriter : LogWriter() {
    data class LogEntry(val severity: Severity, val message: String, val throwable: Throwable?)

    val logEntries = mutableListOf<LogEntry>()

    val warningThrowables: List<Throwable?>
        get() = logEntries.filter { it.severity == Severity.Warn }.map { it.throwable }

    override fun log(severity: Severity, message: String, tag: String, throwable: Throwable?) {
        logEntries += LogEntry(severity = severity, message = message, throwable = throwable)
    }
}
