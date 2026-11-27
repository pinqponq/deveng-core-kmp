package core.ads.fake

import core.ads.player.RewardedAdPlayer

internal class FakeRewardedAdPlayer : RewardedAdPlayer {

    var loadCount = 0
        private set
    var showCount = 0
        private set

    private var onLoad: (() -> Unit)? = null
    private var onLoadFailure: ((Throwable) -> Unit)? = null
    private var onRewardEarned: (() -> Unit)? = null
    private var onShowFailed: ((Throwable) -> Unit)? = null
    private var onDismissed: (() -> Unit)? = null

    override fun load(onLoad: () -> Unit, onFailure: (Throwable) -> Unit) {
        loadCount++
        this.onLoad = onLoad
        onLoadFailure = onFailure
    }

    override fun show(onRewardEarned: () -> Unit, onShowFailed: (Throwable) -> Unit, onDismissed: () -> Unit) {
        showCount++
        this.onRewardEarned = onRewardEarned
        this.onShowFailed = onShowFailed
        this.onDismissed = onDismissed
    }

    fun finishLoad() {
        checkNotNull(onLoad).invoke()
    }

    fun failLoad(exception: Throwable) {
        checkNotNull(onLoadFailure).invoke(exception)
    }

    fun earnReward() {
        checkNotNull(onRewardEarned).invoke()
    }

    fun failShow(exception: Throwable) {
        checkNotNull(onShowFailed).invoke(exception)
    }

    fun dismiss() {
        checkNotNull(onDismissed).invoke()
    }
}
