package core.crash.data

import core.crash.domain.PlatformCrashReporter

internal object NoOpPlatformCrashReporter : PlatformCrashReporter {
    override fun setUserId(userId: String?) = Unit

    override fun setCustomKey(key: String, value: String) = Unit

    override fun recordNonFatal(throwable: Throwable) = Unit

    override fun log(message: String) = Unit
}
