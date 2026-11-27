package core.crash.domain

interface CrashReporter {
    /**
     * Sets the user that later crash reports belong to and adds the installation id under
     * `CrashReportingOptions.installIdKey`. A null [userId] clears the user.
     */
    fun identify(userId: String?)

    fun setCustomKey(key: String, value: String)

    fun recordNonFatal(throwable: Throwable)

    /**
     * Adds [message] to the log that is sent with the next crash or non-fatal error.
     */
    fun log(message: String)
}
