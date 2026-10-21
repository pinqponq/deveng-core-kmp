package core.data.keyvalue

import com.russhwolf.settings.Settings
import core.util.SelectedLanguagePreference

class LanguageStorageImpl(
    private val settings: Settings
) : LanguageStorage {
    override fun getSelectedLanguageCode(): String? = SelectedLanguagePreference.read(settings = settings)

    override fun setSelectedLanguageCode(code: String) {
        SelectedLanguagePreference.write(settings = settings, code = code)
    }
}
