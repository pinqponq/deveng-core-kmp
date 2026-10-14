package core.analytics.data.client

import android.content.Context
import android.os.Bundle
import com.facebook.FacebookSdk
import com.facebook.appevents.AppEventsLogger
import core.analytics.domain.client.ConversionAnalyticsClient
import core.analytics.domain.model.AnalyticsEvent

/**
 * Sends conversion events to Meta App Events via the Facebook SDK. Silently skips logging until the
 * SDK is initialized (see [MetaAnalyticsInitializer]), which only happens once a Facebook App ID is
 * configured — so the app runs unchanged when Meta is not yet set up.
 */
class MetaConversionAnalyticsClient(
    private val context: Context
) : ConversionAnalyticsClient {
    private val logger by lazy { AppEventsLogger.newLogger(context) }

    override fun logEvent(event: AnalyticsEvent, params: Map<String, String>) {
        if (!FacebookSdk.isInitialized()) return

        val bundle = Bundle().apply {
            params.forEach { (key, value) -> putString(key, value) }
        }
        logger.logEvent(event.metaEventName, bundle)
    }
}
