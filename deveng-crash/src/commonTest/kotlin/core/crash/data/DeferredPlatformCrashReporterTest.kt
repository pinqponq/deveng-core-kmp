package core.crash.data

import core.crash.fake.FakePlatformCrashReporter
import core.crash.fake.RecordedCrashCall
import kotlin.test.Test
import kotlin.test.assertEquals

class DeferredPlatformCrashReporterTest {

    private val attachedReporter = FakePlatformCrashReporter()

    @Test
    fun attach_afterCallsWereMade_replaysThemInOrder() {
        val deferredReporter = DeferredPlatformCrashReporter()
        val throwable = IllegalStateException(ERROR_MESSAGE)

        deferredReporter.setUserId(USER_ID)
        deferredReporter.setCustomKey(CUSTOM_KEY, CUSTOM_VALUE)
        deferredReporter.recordNonFatal(throwable)
        deferredReporter.log(LOG_MESSAGE)
        deferredReporter.setUserId(null)
        deferredReporter.attach(attachedReporter)

        assertEquals(
            listOf(
                RecordedCrashCall.SetUserId(USER_ID),
                RecordedCrashCall.SetCustomKey(CUSTOM_KEY, CUSTOM_VALUE),
                RecordedCrashCall.RecordNonFatal(throwable),
                RecordedCrashCall.Log(LOG_MESSAGE),
                RecordedCrashCall.SetUserId(null),
            ),
            attachedReporter.calls,
        )
    }

    @Test
    fun call_afterAttach_goesToAttachedReporterAfterReplayedCalls() {
        val deferredReporter = DeferredPlatformCrashReporter()

        deferredReporter.log(LOG_MESSAGE)
        deferredReporter.attach(attachedReporter)
        deferredReporter.setUserId(USER_ID)

        assertEquals(
            listOf(RecordedCrashCall.Log(LOG_MESSAGE), RecordedCrashCall.SetUserId(USER_ID)),
            attachedReporter.calls,
        )
    }

    @Test
    fun attach_secondTime_doesNotReplayCallsAgain() {
        val deferredReporter = DeferredPlatformCrashReporter()
        val secondReporter = FakePlatformCrashReporter()

        deferredReporter.log(LOG_MESSAGE)
        deferredReporter.attach(attachedReporter)
        deferredReporter.attach(secondReporter)
        deferredReporter.setUserId(USER_ID)

        assertEquals(listOf<RecordedCrashCall>(RecordedCrashCall.Log(LOG_MESSAGE)), attachedReporter.calls)
        assertEquals(listOf<RecordedCrashCall>(RecordedCrashCall.SetUserId(USER_ID)), secondReporter.calls)
    }

    @Test
    fun call_whenPendingCallsAreFull_dropsOldestCall() {
        val deferredReporter = DeferredPlatformCrashReporter(maximumPendingCallCount = 2)

        deferredReporter.log(FIRST_MESSAGE)
        deferredReporter.log(SECOND_MESSAGE)
        deferredReporter.log(THIRD_MESSAGE)
        deferredReporter.attach(attachedReporter)

        assertEquals(
            listOf<RecordedCrashCall>(RecordedCrashCall.Log(SECOND_MESSAGE), RecordedCrashCall.Log(THIRD_MESSAGE)),
            attachedReporter.calls,
        )
    }

    private companion object {
        const val USER_ID = "user-42"
        const val CUSTOM_KEY = "screen"
        const val CUSTOM_VALUE = "gallery"
        const val ERROR_MESSAGE = "upload failed"
        const val LOG_MESSAGE = "opened gallery"
        const val FIRST_MESSAGE = "first"
        const val SECOND_MESSAGE = "second"
        const val THIRD_MESSAGE = "third"
    }
}
