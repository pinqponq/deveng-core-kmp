package core.crash.data

import core.crash.domain.PlatformCrashReporter
import core.util.CustomLogger
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized

private const val DEFAULT_MAXIMUM_PENDING_CALL_COUNT = 100

/**
 * Keeps the calls made before [attach] and replays them in order on the attached reporter. The app
 * sets the user during start-up, which can run before the platform side is ready.
 */
internal class DeferredPlatformCrashReporter(
    private val maximumPendingCallCount: Int = DEFAULT_MAXIMUM_PENDING_CALL_COUNT,
) : PlatformCrashReporter {
    private val lock = SynchronizedObject()
    private val pendingCalls = ArrayDeque<(PlatformCrashReporter) -> Unit>()
    private var attachedReporter: PlatformCrashReporter? = null

    fun attach(platformCrashReporter: PlatformCrashReporter) {
        synchronized(lock) {
            attachedReporter = platformCrashReporter
            pendingCalls.forEach { pendingCall -> pendingCall(platformCrashReporter) }
            pendingCalls.clear()
        }
    }

    override fun setUserId(userId: String?) {
        dispatch { reporter -> reporter.setUserId(userId) }
    }

    override fun setCustomKey(key: String, value: String) {
        dispatch { reporter -> reporter.setCustomKey(key, value) }
    }

    override fun recordNonFatal(throwable: Throwable) {
        dispatch { reporter -> reporter.recordNonFatal(throwable) }
    }

    override fun log(message: String) {
        dispatch { reporter -> reporter.log(message) }
    }

    private fun dispatch(call: (PlatformCrashReporter) -> Unit) {
        synchronized(lock) {
            val reporter = attachedReporter
            if (reporter != null) {
                call(reporter)
                return
            }

            if (pendingCalls.size == maximumPendingCallCount) {
                pendingCalls.removeFirst()
                CustomLogger.log("Crash reporter: no platform handler yet, dropped the oldest of $maximumPendingCallCount waiting calls")
            }

            pendingCalls.addLast(call)
        }
    }
}
