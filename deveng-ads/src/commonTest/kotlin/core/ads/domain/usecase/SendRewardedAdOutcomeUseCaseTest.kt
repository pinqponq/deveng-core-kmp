package core.ads.domain.usecase

import core.ads.domain.model.RewardedAdOutcome
import core.ads.domain.model.RewardedAdSurface
import core.ads.fake.FakeAnalyticsLogSink
import core.ads.fake.FakeConversionAnalyticsClient
import core.ads.fake.FakeConversionEventStore
import core.analytics.domain.ConversionEventTracker
import core.analytics.domain.log.AnalyticsLogLevel
import core.analytics.domain.model.AnalyticsEvent
import core.analytics.domain.model.CommonAnalyticsEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class SendRewardedAdOutcomeUseCaseTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val analyticsLogSink = FakeAnalyticsLogSink()
    private val conversionAnalyticsClient = FakeConversionAnalyticsClient()
    private val sendRewardedAdOutcomeUseCase = SendRewardedAdOutcomeUseCase(
        conversionEventTracker = ConversionEventTracker(
            analyticsLogSink = analyticsLogSink,
            conversionAnalyticsClient = conversionAnalyticsClient,
            conversionEventStore = FakeConversionEventStore(),
            applicationScope = CoroutineScope(testDispatcher),
        ),
        analyticsLogSink = analyticsLogSink,
    )

    @Test
    fun invoke_whenRewardEarned_sendsOneConversionWithSurface() = runTest(testDispatcher) {
        sendRewardedAdOutcomeUseCase(
            outcome = RewardedAdOutcome.RewardEarned,
            surface = TestRewardedAdSurface.DAILY_QUESTION,
        )

        val conversionLog = analyticsLogSink.savedLogs.single()
        assertEquals(REWARDED_AD_COMPLETED_EVENT, conversionLog.message)
        assertEquals(CONVERSION_TAG, conversionLog.metadata["tag"])
        assertEquals(mapOf("surface" to "DAILY_QUESTION"), conversionLog.detail)
        assertEquals(listOf<AnalyticsEvent>(CommonAnalyticsEvent.REWARDED_AD_COMPLETED), conversionAnalyticsClient.loggedEvents)
    }

    @Test
    fun invoke_whenAdFailsToLoad_sendsWarningWithError() = runTest(testDispatcher) {
        sendRewardedAdOutcomeUseCase(
            outcome = RewardedAdOutcome.LoadFailed(exception = IllegalStateException(NO_FILL_MESSAGE)),
            surface = TestRewardedAdSurface.COLLECTIONS,
        )

        val failureLog = analyticsLogSink.savedLogs.single()
        assertEquals(AnalyticsLogLevel.WARNING, failureLog.logLevel)
        assertEquals("RewardedAdLoadFailed", failureLog.message)
        assertEquals(mapOf("source" to "SendRewardedAdOutcomeUseCase", "tag" to REWARDED_AD_TAG), failureLog.metadata)
        assertEquals(
            mapOf(
                "surface" to "COLLECTIONS",
                "exceptionType" to "IllegalStateException",
                "errorMessage" to NO_FILL_MESSAGE,
            ),
            failureLog.detail,
        )
        assertEquals(emptyList(), conversionAnalyticsClient.loggedEvents)
    }

    @Test
    fun invoke_whenAdFailsToShow_sendsShowFailedWarning() = runTest(testDispatcher) {
        sendRewardedAdOutcomeUseCase(
            outcome = RewardedAdOutcome.ShowFailed(exception = IllegalArgumentException()),
            surface = TestRewardedAdSurface.EVENT_SCHEDULE,
        )

        val failureLog = analyticsLogSink.savedLogs.single()
        assertEquals("RewardedAdShowFailed", failureLog.message)
        assertEquals(
            mapOf("surface" to "EVENT_SCHEDULE", "exceptionType" to "IllegalArgumentException"),
            failureLog.detail,
        )
    }

    @Test
    fun invoke_whenAdClosedBeforeReward_sendsWarningWithoutError() = runTest(testDispatcher) {
        sendRewardedAdOutcomeUseCase(
            outcome = RewardedAdOutcome.ClosedBeforeReward,
            surface = TestRewardedAdSurface.DAILY_QUESTION,
        )

        val failureLog = analyticsLogSink.savedLogs.single()
        assertEquals(AnalyticsLogLevel.WARNING, failureLog.logLevel)
        assertEquals("RewardedAdClosedBeforeReward", failureLog.message)
        assertEquals(mapOf("surface" to "DAILY_QUESTION"), failureLog.detail)
    }

    private enum class TestRewardedAdSurface : RewardedAdSurface {
        DAILY_QUESTION,
        COLLECTIONS,
        EVENT_SCHEDULE,
    }

    private companion object {
        const val REWARDED_AD_COMPLETED_EVENT = "REWARDED_AD_COMPLETED"
        const val CONVERSION_TAG = "CONVERSION"
        const val REWARDED_AD_TAG = "REWARDED_AD"
        const val NO_FILL_MESSAGE = "No fill."
    }
}
