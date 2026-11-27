package core.analytics.domain.client

import core.analytics.domain.model.AnalyticsEvent

/**
 * Platform seam for a single analytics sink of a conversion event. Each implementation reads the
 * name it needs off [AnalyticsEvent] (Firebase reads [AnalyticsEvent.firebaseName], Meta reads
 * [AnalyticsEvent.metaEventName]). A no-op on platforms without the given SDK.
 */
interface ConversionAnalyticsClient {
    fun logEvent(event: AnalyticsEvent, params: Map<String, String>)
}
