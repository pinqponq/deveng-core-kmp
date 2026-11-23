package core.ads.player

import co.touchlab.kermit.Logger
import core.ads.domain.model.RewardedAdOutcome

/**
 * A load that fails while nobody asked for the ad is only written to the local log, not reported
 * as an outcome: the next request loads again and reports its own result.
 */
internal class RewardedAdController(
    private val createAdPlayer: () -> RewardedAdPlayer,
    private val onAdOutcome: (RewardedAdOutcome) -> Unit,
    private val onRewardEarned: () -> Unit,
    private val onAdFailed: (RewardedAdOutcome.Failure) -> Unit,
) {
    private var adState: AdState = AdState.Idle
    private var isShowRequested = false

    fun preloadAd() {
        if (adState != AdState.Idle) return
        val adPlayer = createAdPlayer()
        adState = AdState.Loading(adPlayer = adPlayer)
        adPlayer.load(
            onLoad = { handleAdLoaded(adPlayer = adPlayer) },
            onFailure = { exception -> handleAdLoadFailed(exception = exception) },
        )
    }

    fun requestAdShow() {
        if (isShowRequested) return
        isShowRequested = true
        when (val currentAdState = adState) {
            AdState.Idle -> preloadAd()
            is AdState.Loaded -> showAd(adPlayer = currentAdState.adPlayer)
            is AdState.Loading, AdState.Showing -> Unit
        }
    }

    private fun handleAdLoaded(adPlayer: RewardedAdPlayer) {
        adState = AdState.Loaded(adPlayer = adPlayer)
        if (isShowRequested) showAd(adPlayer = adPlayer)
    }

    private fun handleAdLoadFailed(exception: Throwable) {
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
