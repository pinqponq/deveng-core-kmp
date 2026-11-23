package core.ads.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import core.ads.domain.model.RewardedAdOutcome
import core.ads.domain.model.RewardedAdSurface
import core.ads.domain.usecase.SendRewardedAdOutcomeUseCase
import core.ads.fake.FakeRewardedAdPlayer
import core.analytics.domain.ConversionEventTracker
import core.analytics.fake.FakeAnalyticsLogSink
import core.analytics.fake.FakeConversionAnalyticsClient
import core.analytics.fake.FakeConversionEventStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class RewardedAdEffectTest {

    private val analyticsLogSink = FakeAnalyticsLogSink()
    private val sendRewardedAdOutcomeUseCase = SendRewardedAdOutcomeUseCase(
        conversionEventTracker = ConversionEventTracker(
            analyticsLogSink = analyticsLogSink,
            conversionAnalyticsClient = FakeConversionAnalyticsClient(),
            conversionEventStore = FakeConversionEventStore(),
            applicationScope = CoroutineScope(Dispatchers.Unconfined),
        ),
        analyticsLogSink = analyticsLogSink,
    )
    private val adPlayerList = mutableListOf<FakeRewardedAdPlayer>()
    private val adFailureList = mutableListOf<RewardedAdOutcome.Failure>()
    private var rewardCount = 0
    private var isAdShowRequested by mutableStateOf(false)

    @Test
    fun setContent_withAdUnitId_preloadsAdWithoutShowingIt() = runComposeUiTest {
        setContent { AdEffect(adUnitId = AD_UNIT_ID) }
        waitForIdle()
        adPlayerList.single().finishLoad()
        waitForIdle()

        assertEquals(1, adPlayerList.single().loadCount)
        assertEquals(0, adPlayerList.single().showCount)
    }

    @Test
    fun show_withoutAdUnitId_failsAtOnceAndLogsFailure() = runComposeUiTest {
        setContent { AdEffect(adUnitId = null) }
        isAdShowRequested = true
        waitForIdle()

        assertTrue(adPlayerList.isEmpty())
        assertTrue(adFailureList.single() is RewardedAdOutcome.LoadFailed)
        assertEquals(false, isAdShowRequested)
        assertEquals(LOAD_FAILED_EVENT, analyticsLogSink.savedLogs.single().message)
    }

    @Test
    fun showAgain_afterLoadFailed_startsNewAttemptAndLogsFailure() = runComposeUiTest {
        setContent { AdEffect(adUnitId = AD_UNIT_ID) }
        isAdShowRequested = true
        waitForIdle()

        adPlayerList.single().failLoad(exception = IllegalStateException(NO_FILL_MESSAGE))
        waitForIdle()
        isAdShowRequested = true
        waitForIdle()

        assertEquals(1, adFailureList.size)
        assertEquals(2, adPlayerList.size)
        assertEquals(LOAD_FAILED_EVENT, analyticsLogSink.savedLogs.single().message)
    }

    @Test
    fun showAgain_afterAdClosedBeforeReward_showsPreloadedNextAd() = runComposeUiTest {
        setContent { AdEffect(adUnitId = AD_UNIT_ID) }
        isAdShowRequested = true
        waitForIdle()
        adPlayerList.first().finishLoad()
        adPlayerList.first().dismiss()
        adPlayerList.last().finishLoad()
        waitForIdle()

        isAdShowRequested = true
        waitForIdle()

        assertEquals(listOf<RewardedAdOutcome.Failure>(RewardedAdOutcome.ClosedBeforeReward), adFailureList)
        assertEquals(1, adPlayerList.last().showCount)
    }

    @Test
    fun show_whenRewardEarned_callsRewardCallbackOnce() = runComposeUiTest {
        setContent { AdEffect(adUnitId = AD_UNIT_ID) }
        isAdShowRequested = true
        waitForIdle()

        adPlayerList.single().finishLoad()
        adPlayerList.single().earnReward()
        adPlayerList.single().dismiss()
        waitForIdle()

        assertEquals(1, rewardCount)
        assertTrue(adFailureList.isEmpty())
    }

    @Composable
    private fun AdEffect(adUnitId: String?) {
        RewardedAdEffect(
            show = isAdShowRequested,
            adUnitId = adUnitId,
            surface = TestRewardedAdSurface.DECK,
            sendRewardedAdOutcomeUseCase = sendRewardedAdOutcomeUseCase,
            createAdPlayer = { FakeRewardedAdPlayer().also(adPlayerList::add) },
            onRewardEarned = {
                rewardCount++
                isAdShowRequested = false
            },
            onAdFailed = { failure ->
                adFailureList.add(failure)
                isAdShowRequested = false
            },
        )
    }

    private enum class TestRewardedAdSurface : RewardedAdSurface {
        DECK,
    }

    private companion object {
        const val AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"
        const val NO_FILL_MESSAGE = "No fill."
        const val LOAD_FAILED_EVENT = "RewardedAdLoadFailed"
    }
}
