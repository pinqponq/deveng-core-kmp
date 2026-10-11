package core.util

import kotlin.test.Test
import kotlin.test.assertEquals

class SupportedLanguagesTest {
    private val languages = SupportedLanguages(
        defaultResourceLanguage = "tr",
        translatedLanguages = setOf("en", "de"),
        fallbackLanguage = "en"
    )

    @Test
    fun normalize_withRegionOrScript_keepsLowercasedLanguageOnly() {
        assertEquals("pt", languages.normalize("PT-br"))
        assertEquals("zh", languages.normalize("zh_Hant"))
        assertEquals("de", languages.normalize("de"))
    }

    @Test
    fun resolve_withKnownLanguage_returnsMappedValue() {
        val result = languages.resolve("de-AT", mapOf("de" to 1, "tr" to 2), fallback = 0)

        assertEquals(1, result)
    }

    @Test
    fun resolve_withUnknownLanguage_returnsFallback() {
        val result = languages.resolve("xx", mapOf("de" to 1), fallback = 0)

        assertEquals(0, result)
    }

    @Test
    fun resolveUiResourceLanguage_withDefaultLanguage_returnsDefaultLanguage() {
        assertEquals("tr", languages.resolveUiResourceLanguage("TR"))
    }

    @Test
    fun resolveUiResourceLanguage_withTranslatedLanguage_returnsLowercasedCode() {
        assertEquals("de", languages.resolveUiResourceLanguage("DE"))
    }

    @Test
    fun resolveUiResourceLanguage_withUntranslatedLanguage_returnsFallback() {
        assertEquals("en", languages.resolveUiResourceLanguage("fr"))
    }

    @Test
    fun resolveUiLanguage_withSelectedLanguage_ignoresDeviceLanguage() {
        assertEquals("de", languages.resolveUiLanguage(selectedLanguageCode = "de", deviceSystemLanguage = "tr"))
        assertEquals("en", languages.resolveUiLanguage(selectedLanguageCode = "fr", deviceSystemLanguage = "tr"))
    }

    @Test
    fun resolveUiLanguage_withoutSelectedLanguage_returnsDeviceLanguageUntouched() {
        assertEquals("fr-FR", languages.resolveUiLanguage(selectedLanguageCode = null, deviceSystemLanguage = "fr-FR"))
    }
}
