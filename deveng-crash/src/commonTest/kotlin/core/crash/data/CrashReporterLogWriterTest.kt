package core.crash.data

import co.touchlab.kermit.Severity
import core.crash.fake.FakeCrashReporter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CrashReporterLogWriterTest {

    private val crashReporter = FakeCrashReporter()

    @Test
    fun isLoggable_withDefaultMinimumSeverity_acceptsErrorAndAboveOnly() {
        val logWriter = CrashReporterLogWriter(crashReporter = crashReporter)

        assertFalse(logWriter.isLoggable(TAG, Severity.Warn))
        assertTrue(logWriter.isLoggable(TAG, Severity.Error))
        assertTrue(logWriter.isLoggable(TAG, Severity.Assert))
    }

    @Test
    fun isLoggable_withWarnMinimumSeverity_acceptsWarn() {
        val logWriter = CrashReporterLogWriter(crashReporter = crashReporter, minimumSeverity = Severity.Warn)

        assertFalse(logWriter.isLoggable(TAG, Severity.Info))
        assertTrue(logWriter.isLoggable(TAG, Severity.Warn))
    }

    @Test
    fun log_withThrowable_writesMessageAndRecordsNonFatal() {
        val logWriter = CrashReporterLogWriter(crashReporter = crashReporter)
        val throwable = IllegalStateException(ERROR_MESSAGE)

        logWriter.log(Severity.Error, LOG_MESSAGE, TAG, throwable)

        assertEquals(listOf("Error $TAG: $LOG_MESSAGE"), crashReporter.loggedMessages)
        assertEquals(listOf<Throwable>(throwable), crashReporter.recordedNonFatals)
    }

    @Test
    fun log_withoutThrowable_writesMessageOnly() {
        val logWriter = CrashReporterLogWriter(crashReporter = crashReporter)

        logWriter.log(Severity.Error, LOG_MESSAGE, TAG, null)

        assertEquals(listOf("Error $TAG: $LOG_MESSAGE"), crashReporter.loggedMessages)
        assertTrue(crashReporter.recordedNonFatals.isEmpty())
    }

    private companion object {
        const val TAG = "Gallery"
        const val LOG_MESSAGE = "upload failed"
        const val ERROR_MESSAGE = "timeout"
    }
}
