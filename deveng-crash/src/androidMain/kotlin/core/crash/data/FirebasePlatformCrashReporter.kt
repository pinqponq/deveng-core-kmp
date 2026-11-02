package core.crash.data

import core.crash.domain.PlatformCrashReporter
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.crashlytics.crashlytics

private const val CLEARED_USER_ID = ""

// Crashlytics has no call that removes the user id; setting an empty string clears it.
internal fun mapToCrashlyticsUserId(userId: String?): String = userId ?: CLEARED_USER_ID

internal class FirebasePlatformCrashReporter : PlatformCrashReporter {
    override fun setUserId(userId: String?) {
        Firebase.crashlytics.setUserId(mapToCrashlyticsUserId(userId))
    }

    override fun setCustomKey(key: String, value: String) {
        Firebase.crashlytics.setCustomKey(key, value)
    }

    override fun recordNonFatal(throwable: Throwable) {
        Firebase.crashlytics.recordException(throwable)
    }

    override fun log(message: String) {
        Firebase.crashlytics.log(message)
    }
}
