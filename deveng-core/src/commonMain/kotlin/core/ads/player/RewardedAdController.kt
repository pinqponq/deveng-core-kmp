package core.ads.player

import co.touchlab.kermit.Logger
import core.ads.domain.model.RewardedAdOutcome

private const val MISSING_AD_UNIT_ID_MESSAGE = "No rewarded ad unit id in the ad config."
private const val LOAD_TIMED_OUT_MESSAGE = "The rewarded ad did not load in time."

/**
 * A load that fails while nobody asked for the ad is only written to the local log, not reported
 * as an outcome: the next request loads again and reports its own result. The ad SDK may never
 * answer a load (seen offline), so the caller ends a request that waits too long with
 * [expireShowRequest]; callbacks from that abandoned load are ignored.
 */
internal class RewardedAdController(
    private val adUnitId: String?,
    private val createAdPlayer: (adUnitId: String) -> RewardedAdPlayer,
    private val onAdOutcome: (RewardedAdOutcome) -> Unit,
    private val onRewardEarned: () -> Unit,
    private val onAdFailed: (RewardedAdOutcome.Failure) -> Unit,
) {
    private var adState: AdState = AdState.Idle
    private var isShowRequested = false

    fun preloadAd() {
        val rewardedAdUnitId = adUnitId ?: return
        if (adState != AdState.Idle) return
        val adPlayer = createAdPlayer(rewardedAdUnitId)
        adState = AdState.Loading(adPlayer = adPlayer)
        adPlayer.load(
            onLoad = { handleAdLoaded(adPlayer = adPlayer) },
            onFailure = { exception -> handleAdLoadFailed(adPlayer = adPlayer, exception = exception) },
        )
    }

    fun requestAdShow() {
        if (adUnitId == null) {
            reportFailure(failure = RewardedAdOutcome.LoadFailed(exception = IllegalStateException(MISSING_AD_UNIT_ID_MESSAGE)))
            return
        }
        if (isShowRequested) return
        isShowRequested = true
        when (val currentAdState = adState) {
            AdState.Idle -> preloadAd()
            is AdState.Loaded -> showAd(adPlayer = currentAdState.adPlayer)
            is AdState.Loading, AdState.Showing -> Unit
        }
    }

    fun expireShowRequest() {
        if (!isShowRequested || adState !is AdState.Loading) return
        adState = AdState.Idle
        isShowRequested = false
        reportFailure(failure = RewardedAdOutcome.LoadFailed(exception = IllegalStateException(LOAD_TIMED_OUT_MESSAGE)))
    }

    private fun isLoading(adPlayer: RewardedAdPlayer): Boolean {
        return (adState as? AdState.Loading)?.adPlayer === adPlayer
    }

    private fun handleAdLoaded(adPlayer: RewardedAdPlayer) {
        if (!isLoading(adPlayer = adPlayer)) return
        adState = AdState.Loaded(adPlayer = adPlayer)
        if (isShowRequested) showAd(adPlayer = adPlayer)
    }

    private fun handleAdLoadFailed(adPlayer: RewardedAdPlayer, exception: Throwable) {
        if (!isLoading(adPlayer = adPlayer)) return
        adState = AdState.Idle
        if (!isShowRequested) {
            Logger.w(exception) { "Rewarded ad preload failed; the next request loads again." }
            return
        }
        isShowRequested = false
        reportFailure(failure = RewardedAdOutcome.LoadFailed(exception = exception))
    }

    private fun showAd(adPlayer: RewardedAdPlayer) {
        adState = AdState.Showing
        var isRewardEarned = false
        var isShowFinished = false
        val finishShow: (RewardedAdOutcome.Failure?) -> Unit = { failure ->
            if (!isShowFinished) {
                isShowFinished = true
                adState = AdState.Idle
                isShowRequested = false
                failure?.let { adFailure -> reportFailure(failure = adFailure) }
                preloadAd()
            }
        }
        adPlayer.show(
            onRewardEarned = {
                isRewardEarned = true
                onAdOutcome(RewardedAdOutcome.RewardEarned)
                onRewardEarned()
            },
            onShowFailed = { exception -> finishShow(RewardedAdOutcome.ShowFailed(exception = exception)) },
            onDismissed = { finishShow(if (isRewardEarned) null else RewardedAdOutcome.ClosedBeforeReward) },
        )
    }

    private fun reportFailure(failure: RewardedAdOutcome.Failure) {
        onAdOutcome(failure)
        onAdFailed(failure)
    }

    private sealed interface AdState {
        data object Idle : AdState

        data class Loading(val adPlayer: RewardedAdPlayer) : AdState

        data class Loaded(val adPlayer: RewardedAdPlayer) : AdState

        data object Showing : AdState
    }
}
