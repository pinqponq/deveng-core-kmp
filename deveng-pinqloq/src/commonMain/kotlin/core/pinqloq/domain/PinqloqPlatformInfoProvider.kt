package core.pinqloq.domain

import core.pinqloq.domain.model.PinqloqPlatformInfo

fun interface PinqloqPlatformInfoProvider {
    fun providePlatformInfo(): PinqloqPlatformInfo
}
