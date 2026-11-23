package core.ads.player

import app.lexilabs.basic.ads.DependsOnGoogleMobileAds
import app.lexilabs.basic.ads.RewardedAdHandler
import co.touchlab.kermit.Logger

// Keeps the handler of the latest load strongly referenced from a top-level property, so the
// Kotlin Native GC cannot collect it while Google Mobile Ads still has to call its callbacks.
private var activeRewardedAdHandler: RewardedAdHandler? = null

@OptIn(DependsOnGoogleMobileAds::class)
internal class IosRewardedAdPlayer(
    private val adUnitId: String,
) : RewardedAdPlayer {
    private val rewardedAdHandler = RewardedAdHandler(activity = null)

    override fun load(onLoad: () -> Unit, onFailure: (Throwable) -> Unit) {
        activeRewardedAdHandler = rewardedAdHandler
        rewardedAdHandler.load(adUnitId = adUnitId, onLoad = onLoad, onFailure = onFailure)
    }

    /**
     * The iOS [RewardedAdHandler.show] requires listeners to be registered beforehand and throws
     * otherwise; since it runs inside a Google Mobile Ads completion handler, an escaping exception
     * would terminate the process instead of unwinding.
     */
    override fun show(onRewardEarned: () -> Unit, onShowFailed: (Throwable) -> Unit, onDismissed: () -> Unit) {
        runCatching {
            rewardedAdHandler.setListeners(
                onFailure = { exception ->
                    Logger.w(exception) { "Rewarded ad failed to present." }
                    onShowFailed(exception)
                },
                onDismissed = onDismissed,
            )
            rewardedAdHandler.show { onRewardEarned() }
        }.onFailure { throwable ->
            Logger.e(throwable) { "Rewarded ad could not be shown." }
            onShowFailed(throwable)
        }
    }
}
