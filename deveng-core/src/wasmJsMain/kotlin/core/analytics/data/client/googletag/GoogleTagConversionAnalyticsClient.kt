package core.analytics.data.client.googletag

import core.analytics.domain.client.ConversionAnalyticsClient
import core.analytics.domain.model.AnalyticsEvent
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Sends conversion events to Google Analytics 4 from a browser page. It loads the Google tag script itself,
 * applies Google consent mode, optionally shows a consent banner and removes the `_ga` cookies when the
 * visitor declines. Nothing is sent, and no script is loaded, on a hostname outside
 * [GoogleTagOptions.enabledHostnames].
 */
class GoogleTagConversionAnalyticsClient(
    private val options: GoogleTagOptions
) : ConversionAnalyticsClient {

    private val screenViewDeduplicator = ScreenViewDeduplicator()
    private val isEnabled = options.isEnabledOn(readPageHostname())

    init {
        if (isEnabled) installGoogleTag(buildConfigurationJson())
    }

    override fun logEvent(event: AnalyticsEvent, params: Map<String, String>) {
        if (!isEnabled) return
        val eventName = GoogleTagEventFormatter.sanitizeEventName(options.eventName(event)) ?: return
        val parametersJson = GoogleTagEventFormatter.formatParameters(params, options.numericParameterNames)
        sendGoogleTagEvent(options.globalObjectName, eventName, parametersJson)
    }

    /**
     * Reports a `page_view` for [screenName] unless it is the screen reported last. The product calls this
     * from its navigation listener.
     */
    fun trackScreenView(screenName: String) {
        if (!isEnabled || !screenViewDeduplicator.shouldReport(screenName)) return
        sendGoogleTagScreenView(
            globalObjectName = options.globalObjectName,
            pageLocation = options.screenView.pathPrefix + screenName,
            pageTitle = options.screenView.titlePrefix + screenName
        )
    }

    private fun buildConfigurationJson(): String {
        val consent = options.consent
        return JsonObject(
            mapOf(
                "measurementId" to JsonPrimitive(options.measurementId),
                "globalObjectName" to JsonPrimitive(options.globalObjectName),
                "grantedValue" to JsonPrimitive(ConsentChoice.GRANTED.storageValue),
                "deniedValue" to JsonPrimitive(ConsentChoice.DENIED.storageValue),
                "analyticsCookiePrefix" to JsonPrimitive(ConsentChoice.ANALYTICS_COOKIE_PREFIX),
                "consent" to if (consent == null) JsonNull else JsonObject(
                    mapOf(
                        "storageKey" to JsonPrimitive(consent.storageKey),
                        "privacyUrl" to (consent.privacyUrl?.let(::JsonPrimitive) ?: JsonNull),
                        "showBanner" to JsonPrimitive(consent.showBanner),
                        "cssClassPrefix" to JsonPrimitive(consent.cssClassPrefix),
                        "texts" to JsonObject(
                            mapOf(
                                "bannerLabel" to JsonPrimitive(consent.texts.bannerLabel),
                                "bannerMessage" to JsonPrimitive(consent.texts.bannerMessage),
                                "details" to JsonPrimitive(consent.texts.details),
                                "decline" to JsonPrimitive(consent.texts.decline),
                                "accept" to JsonPrimitive(consent.texts.accept)
                            )
                        )
                    )
                )
            )
        ).toString()
    }
}
