package core.analytics.data.client

import core.analytics.domain.client.ConversionAnalyticsClient
import core.analytics.domain.model.AnalyticsEvent
import core.util.CustomLogger.log

/**
 * Fans a conversion event out to every platform sink, isolating each behind its own try/catch so a
 * failure in one (e.g. Meta not yet initialized) never blocks the others.
 */
class CompositeConversionAnalyticsClient(
    private val clients: List<ConversionAnalyticsClient>
) : ConversionAnalyticsClient {
    override fun logEvent(event: AnalyticsEvent, params: Map<String, String>) {
        clients.forEach { client ->
            try {
                client.logEvent(event, params)
            } catch (e: Exception) {
                log("CompositeConversionAnalyticsClient sink error: $e")
            }
        }
    }
}
