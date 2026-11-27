package core.util

/**
 * The languages an app ships, supplied by the app so core carries no language list of its own.
 *
 * @param defaultResourceLanguage language of the default `values/` resource set; it has no
 *   `values-xx` folder of its own, so it is always resolvable.
 * @param translatedLanguages languages with a translated `values-xx` resource set.
 * @param fallbackLanguage language used for any code outside both sets.
 */
class SupportedLanguages(
    private val defaultResourceLanguage: String,
    private val translatedLanguages: Set<String>,
    private val fallbackLanguage: String
) {
    fun normalize(language: String): String = language
        .substringBefore('-')
        .substringBefore('_')
        .lowercase()

    fun <T> resolve(systemLanguage: String, valuesByLanguage: Map<String, T>, fallback: T): T =
        valuesByLanguage[normalize(systemLanguage)] ?: fallback

    fun resolveUiResourceLanguage(selectedCode: String): String {
        val normalized = selectedCode.lowercase()
        if (normalized == defaultResourceLanguage || normalized in translatedLanguages) return normalized
        return fallbackLanguage
    }

    /**
     * Resolves the language for locale-aware formatting outside `stringResource`. A platform's
     * global system language ignores a per-app language override, so once the user has picked
     * a language [selectedLanguageCode] wins; without a choice [deviceSystemLanguage] passes through.
     */
    fun resolveUiLanguage(selectedLanguageCode: String?, deviceSystemLanguage: String): String =
        selectedLanguageCode?.let(::resolveUiResourceLanguage) ?: deviceSystemLanguage
}
