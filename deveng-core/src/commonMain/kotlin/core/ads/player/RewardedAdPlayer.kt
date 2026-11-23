package core.ads.player

/**
 * A player is used for a single ad: after it was shown, a new player loads the next ad.
 */
internal interface RewardedAdPlayer {
    fun load(onLoad: () -> Unit, onFailure: (Throwable) -> Unit)

    fun show(onRewardEarned: () -> Unit, onShowFailed: (Throwable) -> Unit, onDismissed: () -> Unit)
}
