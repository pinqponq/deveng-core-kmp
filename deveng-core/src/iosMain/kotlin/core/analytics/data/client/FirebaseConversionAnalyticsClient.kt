package core.analytics.data.client

import core.analytics.domain.client.ConversionAnalyticsClient
import core.analytics.domain.model.AnalyticsEvent
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.analytics.analytics

/**
 * Sends conversion events to Firebase Analytics via the GitLive Firebase SDK, backed by the native
 * FirebaseAnalytics pod already linked into the app (configured in AppDelegate).
 */
class FirebaseConversionAnalyticsClient : ConversionAnalyticsClient {
    override fun logEvent(event: AnalyticsEvent, params: Map<String, String>) {
        Firebase.analytics.logEvent(event.firebaseName, params.mapValues { (_, value) -> value as Any })
    }
}
