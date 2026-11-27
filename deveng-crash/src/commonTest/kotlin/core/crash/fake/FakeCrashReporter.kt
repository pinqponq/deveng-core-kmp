package core.crash.fake

import core.crash.domain.CrashReporter

class FakeCrashReporter : CrashReporter {
    val loggedMessages = mutableListOf<String>()
    val recordedNonFatals = mutableListOf<Throwable>()

    override fun identify(userId: String?) = Unit

    override fun setCustomKey(key: String, value: String) = Unit

    override fun recordNonFatal(throwable: Throwable) {
        recordedNonFatals += throwable
    }

    override fun log(message: String) {
        loggedMessages += message
    }
}
