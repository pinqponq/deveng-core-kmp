package core.data.keyvalue

import com.russhwolf.settings.Settings
import core.util.SelectedLanguagePreference

class LanguageStorageImpl(
    private val dataStore: Settings
) : LanguageStorage {
    override fun getSelectedLanguageCode(): String? = SelectedLanguagePreference.read(settings = dataStore)

    override fun setSelectedLanguageCode(code: String) {
        SelectedLanguagePreference.write(settings = dataStore, code = code)
    }
}
