package core.util.multiplatform

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MultiPlatformUtilsPlatformConfigTest {

    private val originalOperatingSystemVersion = System.getProperty(OPERATING_SYSTEM_VERSION_PROPERTY)
    private val multiPlatformUtils = MultiPlatformUtils()

    @AfterTest
    fun tearDown() {
        System.setProperty(OPERATING_SYSTEM_VERSION_PROPERTY, originalOperatingSystemVersion)
    }

    @Test
    fun getPlatformConfig_withOperatingSystemVersion_returnsSystemPropertyValue() {
        System.setProperty(OPERATING_SYSTEM_VERSION_PROPERTY, OPERATING_SYSTEM_VERSION)

        val platformConfig = multiPlatformUtils.getPlatformConfig()

        assertEquals(OPERATING_SYSTEM_VERSION, platformConfig.operatingSystemVersion)
    }

    @Test
    fun getPlatformConfig_withBlankOperatingSystemVersion_returnsNullOperatingSystemVersion() {
        System.setProperty(OPERATING_SYSTEM_VERSION_PROPERTY, BLANK_OPERATING_SYSTEM_VERSION)

        val platformConfig = multiPlatformUtils.getPlatformConfig()

        assertNull(platformConfig.operatingSystemVersion)
    }

    @Test
    fun getPlatformConfig_onDesktop_returnsDesktopPackageVersionName() {
        val platformConfig = multiPlatformUtils.getPlatformConfig()

        assertEquals(DESKTOP_PACKAGE_VERSION_NAME, platformConfig.packageVersionName)
    }

    private companion object {
        const val OPERATING_SYSTEM_VERSION_PROPERTY = "os.version"
        const val OPERATING_SYSTEM_VERSION = "15.4.1"
        const val BLANK_OPERATING_SYSTEM_VERSION = "  "
        const val DESKTOP_PACKAGE_VERSION_NAME = "0.0.0"
    }
}
