package core.data.keyvalue

import com.russhwolf.settings.MapSettings
import core.util.SelectedLanguagePreference
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LanguageStorageImplTest {
    private val settings = MapSettings()
    private val storage = LanguageStorageImpl(dataStore = settings)

    @Test
    fun getSelectedLanguageCode_withoutChoice_returnsNull() {
        assertNull(storage.getSelectedLanguageCode())
    }

    @Test
    fun setSelectedLanguageCode_storesChoiceForLaterReads() {
        storage.setSelectedLanguageCode("de")

        assertEquals("de", storage.getSelectedLanguageCode())
    }

    @Test
    fun setSelectedLanguageCode_overwritesPreviousChoice() {
        storage.setSelectedLanguageCode("de")
        storage.setSelectedLanguageCode("fr")

        assertEquals("fr", storage.getSelectedLanguageCode())
    }

    @Test
    fun setSelectedLanguageCode_usesTheKeyPlatformStartupHooksRead() {
        storage.setSelectedLanguageCode("ja")

        assertEquals("ja", SelectedLanguagePreference.read(settings))
        assertEquals("ja", settings.getString("core_selected_language_code", ""))
    }
}
