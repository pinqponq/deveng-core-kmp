package core.pinqloq.data

import core.pinqloq.domain.PinqloqPlatformInfoProvider
import core.pinqloq.domain.model.PinqloqPlatformInfo
import core.util.multiplatform.MultiPlatformUtils

class MultiPlatformPinqloqPlatformInfoProvider(
    private val multiPlatformUtils: MultiPlatformUtils
) : PinqloqPlatformInfoProvider {
    override fun providePlatformInfo(): PinqloqPlatformInfo {
        val platformConfig = multiPlatformUtils.getPlatformConfig()

        return PinqloqPlatformInfo(
            appVersionName = platformConfig.packageVersionName,
            deviceUid = platformConfig.uuid,
            deviceName = platformConfig.deviceName
        )
    }
}
