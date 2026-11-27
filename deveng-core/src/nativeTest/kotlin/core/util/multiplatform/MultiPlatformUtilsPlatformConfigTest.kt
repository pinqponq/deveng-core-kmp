package core.util.multiplatform

import platform.UIKit.UIDevice
import kotlin.test.Test
import kotlin.test.assertEquals

class MultiPlatformUtilsPlatformConfigTest {

    @Test
    fun getPlatformConfig_onDevice_returnsDeviceSystemVersion() {
        val platformConfig = MultiPlatformUtils().getPlatformConfig()

        assertEquals(UIDevice.currentDevice.systemVersion, platformConfig.operatingSystemVersion)
    }
}
