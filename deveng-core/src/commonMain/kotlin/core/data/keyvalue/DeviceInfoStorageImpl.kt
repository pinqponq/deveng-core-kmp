package core.data.keyvalue

import com.russhwolf.settings.Settings
import com.russhwolf.settings.get
import com.russhwolf.settings.set

class DeviceInfoStorageImpl(
    private val prefs: Settings
) : DeviceInfoStorage {
    override fun getGeneratedPlatformIdentifier(): String? {
        return prefs[Keys.GENERATED_PLATFORM_IDENTIFIER.key]
    }

    override fun setGeneratedPlatformIdentifier(uuid: String) {
        prefs[Keys.GENERATED_PLATFORM_IDENTIFIER.key] = uuid
    }


    enum class Keys {
        GENERATED_PLATFORM_IDENTIFIER;

        val key get() = this.name
    }
}
