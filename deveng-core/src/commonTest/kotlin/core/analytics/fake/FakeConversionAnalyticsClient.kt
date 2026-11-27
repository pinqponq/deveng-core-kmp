package core.analytics.fake

import core.analytics.domain.client.ConversionAnalyticsClient
import core.analytics.domain.model.AnalyticsEvent

class FakeConversionAnalyticsClient : ConversionAnalyticsClient {

    val loggedEvents = mutableListOf<AnalyticsEvent>()
    val loggedParams = mutableListOf<Map<String, String>>()
    var logEventFailure: Exception? = null

    override fun logEvent(event: AnalyticsEvent, params: Map<String, String>) {
        loggedEvents += event
        loggedParams += params
        logEventFailure?.let { failure -> throw failure }
    }
}
