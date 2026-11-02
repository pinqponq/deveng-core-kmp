package core.crash.data

import core.crash.domain.model.CrashReportingOptions
import core.crash.fake.FakePlatformCrashReporter
import core.crash.fake.RecordedCrashCall
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CrashReporterImplTest {

    private val platformCrashReporter = FakePlatformCrashReporter()

    @Test
    fun identify_withUserId_setsUserAndInstallId() {
        val crashReporter = createEnabledCrashReporter()

        crashReporter.identify(USER_ID)

        assertEquals(
            listOf(
                RecordedCrashCall.SetUserId(USER_ID),
                RecordedCrashCall.SetCustomKey(INSTALL_ID_KEY, INSTALL_ID),
            ),
            platformCrashReporter.calls,
        )
    }

    @Test
    fun identify_withNullUserId_clearsUserAndKeepsInstallId() {
        val crashReporter = createEnabledCrashReporter()

        crashReporter.identify(null)

        assertEquals(
            listOf(
                RecordedCrashCall.SetUserId(null),
                RecordedCrashCall.SetCustomKey(INSTALL_ID_KEY, INSTALL_ID),
            ),
            platformCrashReporter.calls,
        )
    }

    @Test
    fun identify_afterInstallIdChanges_sendsCurrentInstallId() {
        var installId = INSTALL_ID
        val crashReporter = createCrashReporter(
            crashReportingOptions = CrashReportingOptions(installIdKey = INSTALL_ID_KEY),
            installIdProvider = { installId },
            platformCrashReporter = platformCrashReporter,
        )

        installId = OTHER_INSTALL_ID
        crashReporter.identify(USER_ID)

        assertEquals(RecordedCrashCall.SetCustomKey(INSTALL_ID_KEY, OTHER_INSTALL_ID), platformCrashReporter.calls.last())
    }

    @Test
    fun setCustomKey_recordNonFatalAndLog_forwardToPlatformReporterInOrder() {
        val crashReporter = createEnabledCrashReporter()
        val throwable = IllegalStateException(ERROR_MESSAGE)

        crashReporter.setCustomKey(CUSTOM_KEY, CUSTOM_VALUE)
        crashReporter.recordNonFatal(throwable)
        crashReporter.log(LOG_MESSAGE)

        assertEquals(
            listOf(
                RecordedCrashCall.SetCustomKey(CUSTOM_KEY, CUSTOM_VALUE),
                RecordedCrashCall.RecordNonFatal(throwable),
                RecordedCrashCall.Log(LOG_MESSAGE),
            ),
            platformCrashReporter.calls,
        )
    }

    @Test
    fun everyCall_withDisabledOptions_reachesNoPlatformReporter() {
        var isInstallIdRead = false
        val crashReporter = createCrashReporter(
            crashReportingOptions = CrashReportingOptions(isEnabled = false),
            installIdProvider = {
                isInstallIdRead = true
                INSTALL_ID
            },
            platformCrashReporter = platformCrashReporter,
        )

        crashReporter.identify(USER_ID)
        crashReporter.setCustomKey(CUSTOM_KEY, CUSTOM_VALUE)
        crashReporter.recordNonFatal(IllegalStateException(ERROR_MESSAGE))
        crashReporter.log(LOG_MESSAGE)

        assertTrue(platformCrashReporter.calls.isEmpty())
        assertFalse(isInstallIdRead)
    }

    private fun createEnabledCrashReporter() = createCrashReporter(
        crashReportingOptions = CrashReportingOptions(installIdKey = INSTALL_ID_KEY),
        installIdProvider = { INSTALL_ID },
        platformCrashReporter = platformCrashReporter,
    )

    private companion object {
        const val USER_ID = "user-42"
        const val INSTALL_ID = "install-1"
        const val OTHER_INSTALL_ID = "install-2"
        const val INSTALL_ID_KEY = "app_install_id"
        const val CUSTOM_KEY = "screen"
        const val CUSTOM_VALUE = "gallery"
        const val ERROR_MESSAGE = "upload failed"
        const val LOG_MESSAGE = "opened gallery"
    }
}
