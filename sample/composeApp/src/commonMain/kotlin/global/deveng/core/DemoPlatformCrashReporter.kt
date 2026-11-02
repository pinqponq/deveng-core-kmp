package global.deveng.core

import core.crash.domain.PlatformCrashReporter

internal class DemoPlatformCrashReporter(
    private val onCall: (String) -> Unit,
) : PlatformCrashReporter {
    override fun setUserId(userId: String?) {
        onCall("crash: setUserId ${userId ?: "cleared"}")
    }

    override fun setCustomKey(key: String, value: String) {
        onCall("crash: setCustomKey $key=$value")
    }

    override fun recordNonFatal(throwable: Throwable) {
        onCall("crash: recordNonFatal ${throwable.message}")
    }

    override fun log(message: String) {
        onCall("crash: log $message")
    }
}
