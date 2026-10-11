package core.analytics.data.client

import core.analytics.domain.client.ConversionAnalyticsClient
import core.analytics.domain.model.AnalyticsEvent

/**
 * Analytics sink for platforms without a native SDK (Desktop, Web).
 * Conversion events still reach pinqloq through the AnalyticsLogSink; only the SDK legs are skipped.
 */
class NoOpConversionAnalyticsClient : ConversionAnalyticsClient {
    override fun logEvent(event: AnalyticsEvent, params: Map<String, String>) = Unit
}
