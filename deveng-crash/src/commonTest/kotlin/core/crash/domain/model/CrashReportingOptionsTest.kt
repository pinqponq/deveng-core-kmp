package core.crash.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class CrashReportingOptionsTest {

    @Test
    fun create_withDefaults_usesNeutralInstallIdKeyAndIsEnabled() {
        val crashReportingOptions = CrashReportingOptions()

        assertEquals("install_id", crashReportingOptions.installIdKey)
        assertTrue(crashReportingOptions.isEnabled)
    }

    @Test
    fun create_withBlankInstallIdKey_fails() {
        assertFailsWith<IllegalArgumentException> { CrashReportingOptions(installIdKey = " ") }
    }
}
