package core.ads.player

import core.ads.domain.model.RewardedAdOutcome
import core.ads.fake.FakeRewardedAdPlayer
import kotlin.test.Test
import kotlin.test.assertEquals

class RewardedAdControllerTest {

    private val adPlayerList = mutableListOf<FakeRewardedAdPlayer>()
    private val adOutcomeList = mutableListOf<RewardedAdOutcome>()
    private val adController = RewardedAdController(
        createAdPlayer = { FakeRewardedAdPlayer().also(adPlayerList::add) },
        onAdOutcome = adOutcomeList::add,
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
        assertEquals(2, adPlayerList.size)
    }

    @Test
    fun requestAdShow_whenLoadFails_reportsLoadFailedAndAllowsNewAttempt() {
        val loadException = IllegalStateException(NO_FILL_MESSAGE)
        adController.requestAdShow()

        adPlayerList.single().failLoad(exception = loadException)
        adController.requestAdShow()

        assertEquals(listOf<RewardedAdOutcome>(RewardedAdOutcome.LoadFailed(exception = loadException)), adOutcomeList)
        assertEquals(2, adPlayerList.size)
    }

    @Test
    fun requestAdShow_whenRewardEarnedAndAdDismissed_reportsOnlyRewardAndPreloadsNextAd() {
        adController.requestAdShow()
        adPlayerList.single().finishLoad()

        adPlayerList.single().earnReward()
        adPlayerList.single().dismiss()

        assertEquals(listOf<RewardedAdOutcome>(RewardedAdOutcome.RewardEarned), adOutcomeList)
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

    private companion object {
        const val NO_FILL_MESSAGE = "No fill."
        const val SHOW_ERROR_MESSAGE = "Ad expired."
    }
}
