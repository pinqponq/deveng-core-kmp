package core.crash.data

import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Severity
import core.crash.domain.CrashReporter

/**
 * Forwards Kermit logs at [minimumSeverity] or above to [crashReporter]: the message goes to the
 * crash log and an attached throwable is recorded as a non-fatal error. Add it with
 * `Logger.addLogWriter`; it is not added by default.
 */
class CrashReporterLogWriter(
    private val crashReporter: CrashReporter,
    private val minimumSeverity: Severity = Severity.Error,
) : LogWriter() {
    override fun isLoggable(tag: String, severity: Severity): Boolean = severity >= minimumSeverity

    override fun log(severity: Severity, message: String, tag: String, throwable: Throwable?) {
        crashReporter.log("$severity $tag: $message")
        throwable?.let(crashReporter::recordNonFatal)
    }
}
