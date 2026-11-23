package core.ads.player

import core.ads.domain.model.RewardedAdOutcome

/**
 * Keeps one rewarded ad loaded ahead of time and shows it when it is requested.
 *
 * A load that fails while nobody asked for the ad is not reported: the next request loads again
 * and reports that result. After an ad was shown, dismissed or failed to show, the next ad starts
 * loading at once.
 */
internal class RewardedAdController(
    private val createAdPlayer: () -> RewardedAdPlayer,
    private val onAdOutcome: (RewardedAdOutcome) -> Unit,
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
        if (!isShowRequested) return
        isShowRequested = false
        onAdOutcome(RewardedAdOutcome.LoadFailed(exception = exception))
    }

    private fun showAd(adPlayer: RewardedAdPlayer) {
        adState = AdState.Showing
        var isRewardEarned = false
        var isShowFinished = false
        val finishShow: (RewardedAdOutcome?) -> Unit = { outcome ->
            if (!isShowFinished) {
                isShowFinished = true
                adState = AdState.Idle
                isShowRequested = false
                outcome?.let(onAdOutcome)
                preloadAd()
            }
        }
        adPlayer.show(
            onRewardEarned = {
                isRewardEarned = true
                onAdOutcome(RewardedAdOutcome.RewardEarned)
            },
            onShowFailed = { exception -> finishShow(RewardedAdOutcome.ShowFailed(exception = exception)) },
            onDismissed = { finishShow(if (isRewardEarned) null else RewardedAdOutcome.ClosedBeforeReward) },
        )
    }

    private sealed interface AdState {
        data object Idle : AdState

        data class Loading(val adPlayer: RewardedAdPlayer) : AdState

        data class Loaded(val adPlayer: RewardedAdPlayer) : AdState

        data object Showing : AdState
    }
}
