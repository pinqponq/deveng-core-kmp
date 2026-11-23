package core.ads.player

import android.app.Activity
import app.lexilabs.basic.ads.DependsOnGoogleMobileAds
import app.lexilabs.basic.ads.RewardedAdHandler

@OptIn(DependsOnGoogleMobileAds::class)
internal class AndroidRewardedAdPlayer(
    activity: Activity,
    private val adUnitId: String,
) : RewardedAdPlayer {
    private val rewardedAdHandler = RewardedAdHandler(activity)

    override fun load(onLoad: () -> Unit, onFailure: (Throwable) -> Unit) {
        rewardedAdHandler.load(adUnitId = adUnitId, onLoad = onLoad, onFailure = onFailure)
    }

    override fun show(onRewardEarned: () -> Unit, onShowFailed: (Throwable) -> Unit, onDismissed: () -> Unit) {
        runCatching {
            rewardedAdHandler.setListeners(onFailure = onShowFailed, onDismissed = onDismissed)
            rewardedAdHandler.show { onRewardEarned() }
        }.onFailure(onShowFailed)
    }
}
