package core.crash.data

import core.crash.domain.CrashReporter

internal object NoOpCrashReporter : CrashReporter {
    override fun identify(userId: String?) = Unit

    override fun setCustomKey(key: String, value: String) = Unit

    override fun recordNonFatal(throwable: Throwable) = Unit

    override fun log(message: String) = Unit
}
