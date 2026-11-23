package core.ads.player

import core.ads.domain.model.RewardedAdOutcome
import core.ads.fake.FakeRewardedAdPlayer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RewardedAdControllerTest {

    private val adPlayerList = mutableListOf<FakeRewardedAdPlayer>()
    private val adOutcomeList = mutableListOf<RewardedAdOutcome>()
    private val adFailureList = mutableListOf<RewardedAdOutcome.Failure>()
    private var rewardCount = 0
    private val adController = createAdController(adUnitId = AD_UNIT_ID)

    private fun createAdController(adUnitId: String?) = RewardedAdController(
        adUnitId = adUnitId,
        createAdPlayer = { FakeRewardedAdPlayer().also(adPlayerList::add) },
        onAdOutcome = adOutcomeList::add,
        onRewardEarned = { rewardCount++ },
        onAdFailed = adFailureList::add,
    )

    @Test
    fun requestAdShow_afterPreloadedAdLoaded_showsItWithoutLoadingAgain() {
        adController.preloadAd()
        adPlayerList.single().finishLoad()

        adController.requestAdShow()

        assertEquals(1, adPlayerList.single().loadCount)
        assertEquals(1, adPlayerList.single().showCount)
    }

    @Test
    fun requestAdShow_whilePreloadIsLoading_showsAdWhenLoadFinishes() {
        adController.preloadAd()
        adController.requestAdShow()
        assertEquals(0, adPlayerList.single().showCount)

        adPlayerList.single().finishLoad()

        assertEquals(1, adPlayerList.single().showCount)
    }

    @Test
    fun preloadAd_whenAdAlreadyLoading_doesNotStartSecondLoad() {
        adController.preloadAd()

        adController.preloadAd()

        assertEquals(1, adPlayerList.size)
    }

    @Test
    fun requestAdShow_twiceWhileLoading_showsAdOnce() {
        adController.requestAdShow()
        adController.requestAdShow()

        adPlayerList.single().finishLoad()

        assertEquals(1, adPlayerList.single().showCount)
    }

    @Test
    fun preloadAd_whenLoadFailsWithoutRequest_reportsNothingAndNextRequestLoadsAgain() {
        adController.preloadAd()
        adPlayerList.single().failLoad(exception = IllegalStateException(NO_FILL_MESSAGE))

        adController.requestAdShow()

        assertEquals(emptyList(), adOutcomeList)
        assertEquals(emptyList(), adFailureList)
        assertEquals(2, adPlayerList.size)
    }

    @Test
    fun requestAdShow_whenLoadFails_reportsLoadFailedAndAllowsNewAttempt() {
        val loadException = IllegalStateException(NO_FILL_MESSAGE)
        adController.requestAdShow()

        adPlayerList.single().failLoad(exception = loadException)
        adController.requestAdShow()

        assertEquals(listOf<RewardedAdOutcome>(RewardedAdOutcome.LoadFailed(exception = loadException)), adOutcomeList)
        assertEquals(listOf<RewardedAdOutcome.Failure>(RewardedAdOutcome.LoadFailed(exception = loadException)), adFailureList)
        assertEquals(2, adPlayerList.size)
    }

    @Test
    fun requestAdShow_whenRewardEarnedAndAdDismissed_reportsOnlyRewardAndPreloadsNextAd() {
        adController.requestAdShow()
        adPlayerList.single().finishLoad()

        adPlayerList.single().earnReward()
        adPlayerList.single().dismiss()

        assertEquals(listOf<RewardedAdOutcome>(RewardedAdOutcome.RewardEarned), adOutcomeList)
        assertEquals(1, rewardCount)
        assertEquals(emptyList(), adFailureList)
        assertEquals(2, adPlayerList.size)
    }

    @Test
    fun requestAdShow_whenAdClosedBeforeReward_reportsClosedAndNextRequestShowsPreloadedAd() {
        adController.requestAdShow()
        adPlayerList.first().finishLoad()
        adPlayerList.first().dismiss()
        adPlayerList.last().finishLoad()

        adController.requestAdShow()

        assertEquals(listOf<RewardedAdOutcome>(RewardedAdOutcome.ClosedBeforeReward), adOutcomeList)
        assertEquals(listOf<RewardedAdOutcome.Failure>(RewardedAdOutcome.ClosedBeforeReward), adFailureList)
        assertEquals(0, rewardCount)
        assertEquals(1, adPlayerList.last().showCount)
    }

    @Test
    fun requestAdShow_whenAdFailsToShowAndIsDismissed_reportsShowFailedOnce() {
        val showException = IllegalArgumentException(SHOW_ERROR_MESSAGE)
        adController.requestAdShow()
        adPlayerList.single().finishLoad()

        adPlayerList.single().failShow(exception = showException)
        adPlayerList.first().dismiss()

        assertEquals(listOf<RewardedAdOutcome>(RewardedAdOutcome.ShowFailed(exception = showException)), adOutcomeList)
        assertEquals(listOf<RewardedAdOutcome.Failure>(RewardedAdOutcome.ShowFailed(exception = showException)), adFailureList)
        assertEquals(2, adPlayerList.size)
    }

    @Test
    fun requestAdShow_whileAdIsShowing_doesNotShowSecondAd() {
        adController.requestAdShow()
        adPlayerList.single().finishLoad()

        adController.requestAdShow()

        assertEquals(1, adPlayerList.single().showCount)
        assertEquals(1, adPlayerList.size)
    }

    @Test
    fun expireShowRequest_whileLoading_reportsLoadFailedAndIgnoresLateLoad() {
        adController.requestAdShow()

        adController.expireShowRequest()
        adPlayerList.first().finishLoad()
        adController.requestAdShow()

        assertTrue(adFailureList.single() is RewardedAdOutcome.LoadFailed)
        assertTrue(adOutcomeList.single() is RewardedAdOutcome.LoadFailed)
        assertEquals(0, adPlayerList.first().showCount)
        assertEquals(2, adPlayerList.size)
    }

    @Test
    fun expireShowRequest_afterLateLoadFailure_reportsOnlyTimeout() {
        adController.requestAdShow()
        adController.expireShowRequest()

        adPlayerList.single().failLoad(exception = IllegalStateException(NO_FILL_MESSAGE))

        assertEquals(1, adFailureList.size)
        assertEquals(1, adOutcomeList.size)
    }

    @Test
    fun expireShowRequest_whileAdIsShowing_reportsNothing() {
        adController.requestAdShow()
        adPlayerList.single().finishLoad()

        adController.expireShowRequest()

        assertEquals(emptyList(), adOutcomeList)
        assertEquals(emptyList(), adFailureList)
    }

    @Test
    fun expireShowRequest_withoutRequest_keepsPreloadedAd() {
        adController.preloadAd()

        adController.expireShowRequest()
        adPlayerList.single().finishLoad()
        adController.requestAdShow()

        assertEquals(emptyList(), adFailureList)
        assertEquals(1, adPlayerList.single().showCount)
    }

    @Test
    fun requestAdShow_withoutAdUnitId_reportsLoadFailedEachTimeWithoutLoading() {
        val adControllerWithoutAdUnitId = createAdController(adUnitId = null)
        adControllerWithoutAdUnitId.preloadAd()

        adControllerWithoutAdUnitId.requestAdShow()
        adControllerWithoutAdUnitId.requestAdShow()

        assertEquals(2, adFailureList.count { failure -> failure is RewardedAdOutcome.LoadFailed })
        assertEquals(2, adOutcomeList.size)
        assertTrue(adPlayerList.isEmpty())
    }

    private companion object {
        const val AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"
        const val NO_FILL_MESSAGE = "No fill."
        const val SHOW_ERROR_MESSAGE = "Ad expired."
    }
}
