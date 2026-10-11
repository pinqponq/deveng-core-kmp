package core.analytics.data.client

import core.analytics.domain.client.ConversionAnalyticsClient
import core.analytics.domain.model.AnalyticsEvent

/**
 * Sends conversion events to Meta App Events. The actual FBSDKCoreKit call lives in Swift and is
 * reached through [IosMetaEventLogger], which no-ops until Swift registers a handler (i.e. until Meta
 * is configured) — so the app runs unchanged when Meta is not yet set up.
 */
class MetaConversionAnalyticsClient : ConversionAnalyticsClient {
    override fun logEvent(event: AnalyticsEvent, params: Map<String, String>) {
        IosMetaEventLogger.log(event.metaEventName, params)
    }
}
