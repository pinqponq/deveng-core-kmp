package core.crash.data

import core.crash.domain.PlatformCrashReporter

private const val UNKNOWN_ERROR_TYPE_NAME = "UnknownKotlinError"

/**
 * Connects the Kotlin crash reporter to Firebase Crashlytics, which lives in the iOS app. The app's
 * `AppDelegate` calls [setHandler] once at launch. Calls made before that are kept and replayed in order.
 */
object IosCrashReporterBridge {
    internal val platformCrashReporter = DeferredPlatformCrashReporter()

    fun setHandler(handler: IosCrashReporterHandler) {
        platformCrashReporter.attach(HandlerPlatformCrashReporter(handler))
    }

    private class HandlerPlatformCrashReporter(
        private val handler: IosCrashReporterHandler,
    ) : PlatformCrashReporter {
        override fun setUserId(userId: String?) {
            handler.setUserId(userId)
        }

        override fun setCustomKey(key: String, value: String) {
            handler.setCustomValue(key, value)
        }

        override fun recordNonFatal(throwable: Throwable) {
            handler.recordNonFatal(
                errorTypeName = throwable::class.qualifiedName ?: UNKNOWN_ERROR_TYPE_NAME,
                message = throwable.message.orEmpty(),
                stackTrace = throwable.stackTraceToString(),
            )
        }

        override fun log(message: String) {
            handler.log(message)
        }
    }
}
