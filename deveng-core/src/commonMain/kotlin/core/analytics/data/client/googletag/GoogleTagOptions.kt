package core.analytics.data.client.googletag

import core.analytics.domain.model.AnalyticsEvent

/**
 * Settings of the Google Analytics 4 web client. The client does nothing on a page whose hostname is not
 * in [enabledHostnames], so a development server never sends data.
 *
 * @property eventName picks which name of an [AnalyticsEvent] Google Analytics receives. A name that does
 * is not 1-40 characters of lower case letters, digits and underscores starting with a letter is dropped.
 * @property globalObjectName name of the object the client adds to `window`. A product page can call
 * `window.<globalObjectName>.showConsentBanner()` from its own "cookie settings" link.
 * @property numericParameterNames parameters whose values Google Analytics expects as numbers.
 */
class GoogleTagOptions(
    val measurementId: String,
    val enabledHostnames: Set<String> = emptySet(),
    val consent: ConsentOptions? = null,
    val screenView: ScreenViewOptions = ScreenViewOptions(),
    val eventName: (AnalyticsEvent) -> String = { event -> event.firebaseName },
    val globalObjectName: String = DEFAULT_GLOBAL_OBJECT_NAME,
    val numericParameterNames: Set<String> = DEFAULT_NUMERIC_PARAMETER_NAMES
) {
    init {
        require(measurementId.isNotBlank()) { "The Google Analytics measurement id must not be blank." }
        require(enabledHostnames.none { hostname -> hostname.isBlank() }) {
            "The enabled hostnames must not contain a blank hostname."
        }
        require(GLOBAL_OBJECT_NAME_PATTERN.matches(globalObjectName)) {
            "The global object name must be a JavaScript identifier but was '$globalObjectName'."
        }
    }

    fun isEnabledOn(hostname: String): Boolean = hostname in enabledHostnames

    companion object {
        const val DEFAULT_GLOBAL_OBJECT_NAME = "devengGoogleTag"
        val DEFAULT_NUMERIC_PARAMETER_NAMES = setOf("value", "tax")
        private val GLOBAL_OBJECT_NAME_PATTERN = Regex("^[A-Za-z_$][A-Za-z0-9_$]*$")
    }
}

/**
 * The consent banner and the stored choice. Analytics storage stays denied until the visitor grants it, and
 * Google then receives only pings without cookies. Without this object the choice cannot be granted.
 *
 * @property storageKey `localStorage` key that remembers the visitor's choice.
 * @property privacyUrl target of the details link in the banner; no link when null.
 * @property showBanner false when the product shows its own banner and calls
 * `window.<globalObjectName>.grantConsent()` or `denyConsent()` itself.
 * @property cssClassPrefix prefix of the banner's CSS classes (`<prefix>`, `<prefix>__text`,
 * `<prefix>__actions`, `<prefix>__button`, `<prefix>__button--accept`); the product styles them.
 */
class ConsentOptions(
    val storageKey: String,
    val privacyUrl: String? = null,
    val texts: ConsentTexts = ConsentTexts(),
    val showBanner: Boolean = true,
    val cssClassPrefix: String = DEFAULT_CSS_CLASS_PREFIX
) {
    init {
        require(storageKey.isNotBlank()) { "The consent storage key must not be blank." }
        require(cssClassPrefix.isNotBlank()) { "The consent CSS class prefix must not be blank." }
    }

    companion object {
        const val DEFAULT_CSS_CLASS_PREFIX = "analytics-consent"
    }
}

class ConsentTexts(
    val bannerLabel: String = "Analytics cookies",
    val bannerMessage: String = "Allow Google Analytics cookies?",
    val details: String = "Details",
    val decline: String = "Decline",
    val accept: String = "Accept"
)

/**
 * How a screen change becomes a `page_view` event. The reported page location is the page origin plus
 * [pathPrefix] plus the screen name, and the page title is [titlePrefix] plus the screen name.
 */
class ScreenViewOptions(
    val pathPrefix: String = "/",
    val titlePrefix: String = ""
)
