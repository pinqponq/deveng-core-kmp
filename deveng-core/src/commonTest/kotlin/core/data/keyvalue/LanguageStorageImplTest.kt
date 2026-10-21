package core.data.keyvalue

import com.russhwolf.settings.MapSettings
import core.util.SelectedLanguagePreference
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LanguageStorageImplTest {
    private val settings = MapSettings()
    private val storage = LanguageStorageImpl(settings = settings)

    @Test
    fun getSelectedLanguageCode_withoutChoice_returnsNull() {
        assertNull(storage.getSelectedLanguageCode())
    }

    @Test
    fun setSelectedLanguageCode_withNoPriorChoice_storesItForLaterReads() {
        storage.setSelectedLanguageCode(GERMAN)

        assertEquals(GERMAN, storage.getSelectedLanguageCode())
    }

    @Test
    fun setSelectedLanguageCode_withPriorChoice_overwritesIt() {
        storage.setSelectedLanguageCode(GERMAN)
        storage.setSelectedLanguageCode(FRENCH)

        assertEquals(FRENCH, storage.getSelectedLanguageCode())
    }

    @Test
    fun setSelectedLanguageCode_withAnyChoice_usesTheKeyPlatformStartupHooksRead() {
        storage.setSelectedLanguageCode(JAPANESE)

        assertEquals(JAPANESE, SelectedLanguagePreference.read(settings))
        assertEquals(JAPANESE, settings.getString(SELECTED_LANGUAGE_KEY, ""))
    }

    private companion object {
        const val GERMAN = "de"
        const val FRENCH = "fr"
        const val JAPANESE = "ja"
        const val SELECTED_LANGUAGE_KEY = "core_selected_language_code"
    }
}
