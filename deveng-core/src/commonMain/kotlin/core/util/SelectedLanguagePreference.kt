package core.util

import com.russhwolf.settings.Settings
import com.russhwolf.settings.get
import com.russhwolf.settings.set

/**
 * Read/write the persisted in-app language selection using the exact same key as
 * [core.data.keyvalue.LanguageStorage], so platform startup hooks can read the choice
 * before Koin (and therefore before [core.data.keyvalue.LanguageStorage]) exists yet.
 */
object SelectedLanguagePreference {
    private const val PREFERENCE_KEY = "core_selected_language_code"

    fun read(settings: Settings): String? = settings[PREFERENCE_KEY] as String?

    fun write(settings: Settings, code: String) {
        settings[PREFERENCE_KEY] = code
    }
}
