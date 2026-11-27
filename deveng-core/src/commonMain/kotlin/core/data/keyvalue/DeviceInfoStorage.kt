package core.data.keyvalue

interface DeviceInfoStorage {
    fun getGeneratedPlatformIdentifier(): String?
    fun setGeneratedPlatformIdentifier(uuid: String)
}
