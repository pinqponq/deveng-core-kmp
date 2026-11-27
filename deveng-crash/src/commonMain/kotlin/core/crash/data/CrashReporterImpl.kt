package core.crash.data

import core.crash.domain.CrashReporter
import core.crash.domain.PlatformCrashReporter
import core.crash.domain.model.CrashReportingOptions

internal class CrashReporterImpl(
    private val crashReportingOptions: CrashReportingOptions,
    private val platformCrashReporter: PlatformCrashReporter,
    private val installIdProvider: () -> String,
) : CrashReporter {
    override fun identify(userId: String?) {
        platformCrashReporter.setUserId(userId)
        platformCrashReporter.setCustomKey(crashReportingOptions.installIdKey, installIdProvider())
    }

    override fun setCustomKey(key: String, value: String) {
        platformCrashReporter.setCustomKey(key, value)
    }

    override fun recordNonFatal(throwable: Throwable) {
        platformCrashReporter.recordNonFatal(throwable)
    }

    override fun log(message: String) {
        platformCrashReporter.log(message)
    }
}
