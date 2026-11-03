package core.crash.data

/**
 * Implemented in Swift with the Firebase Crashlytics iOS SDK and passed to
 * [IosCrashReporterBridge.setHandler].
 */
interface IosCrashReporterHandler {
    /**
     * A null [userId] clears the user.
     */
    fun setUserId(userId: String?)

    fun setCustomValue(key: String, value: String)

    /**
     * @param errorTypeName the Kotlin class name of the error, usable as the `NSError` domain.
     * @param stackTrace the Kotlin stack trace as text.
     */
    fun recordNonFatal(errorTypeName: String, message: String, stackTrace: String)

    fun log(message: String)
}
