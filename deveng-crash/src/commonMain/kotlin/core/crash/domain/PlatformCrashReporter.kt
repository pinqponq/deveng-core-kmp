package core.crash.domain

/**
 * The crash SDK of one platform. [CrashReporter] calls it; an app implements it only to send crash
 * reports to another backend or to record the calls in a test.
 */
interface PlatformCrashReporter {
    /**
     * A null [userId] clears the user.
     */
    fun setUserId(userId: String?)

    fun setCustomKey(key: String, value: String)

    fun recordNonFatal(throwable: Throwable)

    fun log(message: String)
}
