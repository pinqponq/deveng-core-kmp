package core.analytics.data.client

import core.analytics.domain.model.AnalyticsEvent
import core.analytics.domain.model.CommonAnalyticsEvent
import core.analytics.fake.FakeConversionAnalyticsClient
import kotlin.test.Test
import kotlin.test.assertEquals

class CompositeConversionAnalyticsClientTest {

    private val firebaseClient = FakeConversionAnalyticsClient()
    private val metaClient = FakeConversionAnalyticsClient()
    private val compositeClient = CompositeConversionAnalyticsClient(clients = listOf(firebaseClient, metaClient))

    @Test
    fun logEvent_sendsEventAndParamsToEveryClient() {
        compositeClient.logEvent(event = CommonAnalyticsEvent.SUBSCRIPTION_PURCHASED, params = PURCHASE_PARAMS)

        listOf(firebaseClient, metaClient).forEach { client ->
            assertEquals(listOf<AnalyticsEvent>(CommonAnalyticsEvent.SUBSCRIPTION_PURCHASED), client.loggedEvents)
            assertEquals(listOf(PURCHASE_PARAMS), client.loggedParams)
        }
    }

    @Test
    fun logEvent_whenFirstClientThrows_stillSendsToNextClient() {
        firebaseClient.logEventFailure = IllegalStateException(SDK_NOT_READY_MESSAGE)

        compositeClient.logEvent(event = CommonAnalyticsEvent.SUBSCRIPTION_PURCHASED, params = PURCHASE_PARAMS)

        assertEquals(listOf<AnalyticsEvent>(CommonAnalyticsEvent.SUBSCRIPTION_PURCHASED), metaClient.loggedEvents)
    }

    private companion object {
        val PURCHASE_PARAMS = mapOf("productId" to "premium_monthly")
        const val SDK_NOT_READY_MESSAGE = "SDK not ready."
    }
}
