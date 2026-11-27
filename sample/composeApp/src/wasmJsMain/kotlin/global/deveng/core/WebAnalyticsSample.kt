package global.deveng.core

import core.analytics.data.client.googletag.ConsentOptions
import core.analytics.data.client.googletag.GoogleTagConversionAnalyticsClient
import core.analytics.data.client.googletag.GoogleTagOptions
import core.analytics.data.client.googletag.ScreenViewOptions
import core.analytics.domain.model.CommonAnalyticsEvent

private const val SAMPLE_MEASUREMENT_ID = "G-SAMPLE0000"
private const val SAMPLE_CONSENT_STORAGE_KEY = "sample.analytics.consent"

/**
 * Wires the Google Analytics 4 web client for the sample page. A product registers the same client in
 * its Koin module as `single<ConversionAnalyticsClient> { createSampleWebAnalytics() }`, and lists it in a
 * `CompositeConversionAnalyticsClient` when it also has other sinks.
 */
internal fun createSampleWebAnalytics(): GoogleTagConversionAnalyticsClient {
    val client = GoogleTagConversionAnalyticsClient(
        GoogleTagOptions(
            measurementId = SAMPLE_MEASUREMENT_ID,
            enabledHostnames = setOf("localhost"),
            consent = ConsentOptions(storageKey = SAMPLE_CONSENT_STORAGE_KEY, privacyUrl = "privacy.html#storage"),
            screenView = ScreenViewOptions(pathPrefix = "/sample/", titlePrefix = "Sample · ")
        )
    )
    client.trackScreenView("Home")
    client.logEvent(CommonAnalyticsEvent.SIGNUP_COMPLETED, mapOf("value" to "1.5", "method" to "sample"))
    return client
}
