package core.analytics.domain

import core.analytics.domain.log.AnalyticsLogLevel
import core.analytics.domain.model.AnalyticsEvent
import core.analytics.domain.model.CommonAnalyticsEvent
import core.analytics.fake.FakeAnalyticsLogSink
import core.analytics.fake.FakeConversionAnalyticsClient
import core.analytics.fake.FakeConversionEventStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ConversionEventTrackerTest {

    private val analyticsLogSink = FakeAnalyticsLogSink()
    private val conversionAnalyticsClient = FakeConversionAnalyticsClient()
    private val conversionEventStore = FakeConversionEventStore()
    private val conversionEventTracker = ConversionEventTracker(
        analyticsLogSink = analyticsLogSink,
        conversionAnalyticsClient = conversionAnalyticsClient,
        conversionEventStore = conversionEventStore,
        applicationScope = CoroutineScope(UnconfinedTestDispatcher()),
    )

    @Test
    fun track_withParams_logsConversionToSinkAndAnalyticsClient() {
        conversionEventTracker.track(event = CommonAnalyticsEvent.LOGIN_COMPLETED, params = LOGIN_PARAMS)

        val conversionLog = analyticsLogSink.savedLogs.single()
        assertEquals(AnalyticsLogLevel.INFORMATION, conversionLog.logLevel)
        assertEquals("LOGIN_COMPLETED", conversionLog.message)
        assertEquals(mapOf("source" to "ConversionEventTracker", "tag" to "CONVERSION"), conversionLog.metadata)
        assertEquals(LOGIN_PARAMS, conversionLog.detail)
        assertEquals(listOf<AnalyticsEvent>(CommonAnalyticsEvent.LOGIN_COMPLETED), conversionAnalyticsClient.loggedEvents)
        assertEquals(listOf(LOGIN_PARAMS), conversionAnalyticsClient.loggedParams)
    }

    @Test
    fun track_repeatableEventTwice_firesBothWithoutMarkingStore() {
        conversionEventTracker.track(event = CommonAnalyticsEvent.LOGIN_COMPLETED)
        conversionEventTracker.track(event = CommonAnalyticsEvent.LOGIN_COMPLETED)

        assertEquals(2, conversionAnalyticsClient.loggedEvents.size)
        assertEquals(2, analyticsLogSink.savedLogs.size)
        assertFalse(conversionEventStore.hasFired("LOGIN_COMPLETED"))
    }

    @Test
    fun track_fireOnceEventTwice_firesOnlyOnceAndMarksStore() {
        conversionEventTracker.track(event = CommonAnalyticsEvent.SIGNUP_COMPLETED)
        conversionEventTracker.track(event = CommonAnalyticsEvent.SIGNUP_COMPLETED)

        assertEquals(listOf<AnalyticsEvent>(CommonAnalyticsEvent.SIGNUP_COMPLETED), conversionAnalyticsClient.loggedEvents)
        assertEquals(1, analyticsLogSink.savedLogs.size)
        assertTrue(conversionEventStore.hasFired("SIGNUP_COMPLETED"))
    }

    @Test
    fun track_fireOnceEventAlreadyFiredOnInstall_firesNothing() {
        conversionEventStore.markFired("SIGNUP_COMPLETED")

        conversionEventTracker.track(event = CommonAnalyticsEvent.SIGNUP_COMPLETED)

        assertTrue(conversionAnalyticsClient.loggedEvents.isEmpty())
        assertTrue(analyticsLogSink.savedLogs.isEmpty())
    }

    @Test
    fun track_appEventImplementingInterface_isDeduplicatedByPinqloqName() {
        conversionEventTracker.track(event = AppSpecificEvent)
        conversionEventTracker.track(event = AppSpecificEvent)

        assertEquals(listOf<AnalyticsEvent>(AppSpecificEvent), conversionAnalyticsClient.loggedEvents)
        assertTrue(conversionEventStore.hasFired(AppSpecificEvent.pinqloqName))
    }

    @Test
    fun track_whenAnalyticsClientThrows_stillLogsToSinkAndMarksFired() {
        conversionAnalyticsClient.logEventFailure = IllegalStateException(SDK_NOT_READY_MESSAGE)

        conversionEventTracker.track(event = CommonAnalyticsEvent.ACCOUNT_VERIFIED)

        assertEquals("ACCOUNT_VERIFIED", analyticsLogSink.savedLogs.single().message)
        assertTrue(conversionEventStore.hasFired("ACCOUNT_VERIFIED"))
    }

    private object AppSpecificEvent : AnalyticsEvent {
        override val pinqloqName = "FIRST_POST_CREATED"
        override val firebaseName = "first_post_created"
        override val metaEventName = "first_post_created"
        override val shouldFireOnce = true
    }

    private companion object {
        val LOGIN_PARAMS = mapOf("method" to "email")
        const val SDK_NOT_READY_MESSAGE = "SDK not ready."
    }
}
