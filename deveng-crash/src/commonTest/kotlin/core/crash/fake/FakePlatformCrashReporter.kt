package core.crash.fake

import core.crash.domain.PlatformCrashReporter

class FakePlatformCrashReporter : PlatformCrashReporter {
    val calls = mutableListOf<RecordedCrashCall>()

    override fun setUserId(userId: String?) {
        calls += RecordedCrashCall.SetUserId(userId)
    }

    override fun setCustomKey(key: String, value: String) {
        calls += RecordedCrashCall.SetCustomKey(key, value)
    }

    override fun recordNonFatal(throwable: Throwable) {
        calls += RecordedCrashCall.RecordNonFatal(throwable)
    }

    override fun log(message: String) {
        calls += RecordedCrashCall.Log(message)
    }
}

sealed interface RecordedCrashCall {
    data class SetUserId(val userId: String?) : RecordedCrashCall

    data class SetCustomKey(val key: String, val value: String) : RecordedCrashCall

    data class RecordNonFatal(val throwable: Throwable) : RecordedCrashCall

    data class Log(val message: String) : RecordedCrashCall
}
