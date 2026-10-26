package core.util.multiplatform

data class PlatformConfig(
    val platform: Platform,
    val systemLanguage: String,
    val uuid: String,
    val deviceName: String,
    val packageVersionName: String,
    val operatingSystemVersion: String? = null,
)
