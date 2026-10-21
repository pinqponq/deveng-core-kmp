package core.util

import kotlin.test.Test
import kotlin.test.assertEquals

class SupportedLanguagesTest {
    private val languages = SupportedLanguages(
        defaultResourceLanguage = TURKISH,
        translatedLanguages = setOf(ENGLISH, GERMAN),
        fallbackLanguage = ENGLISH
    )

    @Test
    fun normalize_withRegionOrScript_keepsLowercasedLanguageOnly() {
        assertEquals("pt", languages.normalize("PT-br"))
        assertEquals("zh", languages.normalize("zh_Hant"))
        assertEquals(GERMAN, languages.normalize(GERMAN))
    }

    @Test
    fun resolve_withKnownLanguage_returnsMappedValue() {
        val result = languages.resolve("de-AT", mapOf(GERMAN to 1, TURKISH to 2), fallback = 0)

        assertEquals(1, result)
    }

    @Test
    fun resolve_withUnknownLanguage_returnsFallback() {
        val result = languages.resolve("xx", mapOf(GERMAN to 1), fallback = 0)

        assertEquals(0, result)
    }

    @Test
    fun resolveUiResourceLanguage_withDefaultLanguage_returnsDefaultLanguage() {
        assertEquals(TURKISH, languages.resolveUiResourceLanguage("TR"))
    }

    @Test
    fun resolveUiResourceLanguage_withTranslatedLanguage_returnsLowercasedCode() {
        assertEquals(GERMAN, languages.resolveUiResourceLanguage("DE"))
    }

    @Test
    fun resolveUiResourceLanguage_withUntranslatedLanguage_returnsFallback() {
        assertEquals(ENGLISH, languages.resolveUiResourceLanguage("fr"))
    }

    @Test
    fun resolveUiLanguage_withSelectedLanguage_ignoresDeviceLanguage() {
        assertEquals(GERMAN, languages.resolveUiLanguage(selectedLanguageCode = GERMAN, deviceSystemLanguage = TURKISH))
        assertEquals(ENGLISH, languages.resolveUiLanguage(selectedLanguageCode = "fr", deviceSystemLanguage = TURKISH))
    }

    @Test
    fun resolveUiLanguage_withoutSelectedLanguage_returnsDeviceLanguageUntouched() {
        assertEquals("fr-FR", languages.resolveUiLanguage(selectedLanguageCode = null, deviceSystemLanguage = "fr-FR"))
    }

    private companion object {
        const val TURKISH = "tr"
        const val ENGLISH = "en"
        const val GERMAN = "de"
    }
}
