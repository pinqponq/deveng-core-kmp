package core.crash.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class IosCrashReporterBridgeTest {

    @Test
    fun setHandler_afterCallsWereMade_replaysThemAsHandlerCallsInOrder() {
        val handler = RecordingIosCrashReporterHandler()
        val platformCrashReporter = createPlatformCrashReporter()
        val throwable = IllegalStateException(ERROR_MESSAGE)

        platformCrashReporter.setUserId(USER_ID)
        platformCrashReporter.setCustomKey(CUSTOM_KEY, CUSTOM_VALUE)
        platformCrashReporter.recordNonFatal(throwable)
        IosCrashReporterBridge.setHandler(handler)
        platformCrashReporter.log(LOG_MESSAGE)
        platformCrashReporter.setUserId(null)

        assertEquals(
            listOf(
                "setUserId:$USER_ID",
                "setCustomValue:$CUSTOM_KEY=$CUSTOM_VALUE",
                "recordNonFatal:kotlin.IllegalStateException:$ERROR_MESSAGE",
                "log:$LOG_MESSAGE",
                "setUserId:null",
            ),
            handler.calls,
        )
        assertTrue(handler.lastStackTrace.contains(ERROR_MESSAGE))
    }

    private class RecordingIosCrashReporterHandler : IosCrashReporterHandler {
        val calls = mutableListOf<String>()
        var lastStackTrace = ""

        override fun setUserId(userId: String?) {
            calls += "setUserId:$userId"
        }

        override fun setCustomValue(key: String, value: String) {
            calls += "setCustomValue:$key=$value"
        }

        override fun recordNonFatal(errorTypeName: String, message: String, stackTrace: String) {
            calls += "recordNonFatal:$errorTypeName:$message"
            lastStackTrace = stackTrace
        }

        override fun log(message: String) {
            calls += "log:$message"
        }
    }

    private companion object {
        const val USER_ID = "user-42"
        const val CUSTOM_KEY = "screen"
        const val CUSTOM_VALUE = "gallery"
        const val ERROR_MESSAGE = "upload failed"
        const val LOG_MESSAGE = "opened gallery"
    }
}
