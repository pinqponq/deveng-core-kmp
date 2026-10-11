package core.ads

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import app.lexilabs.basic.ads.BasicAds
import app.lexilabs.basic.ads.DependsOnGoogleMobileAds
import app.lexilabs.basic.ads.RewardedAdHandler
import core.ads.config.domain.holder.AdConfigHolder
import core.ads.domain.model.RewardedAdOutcome
import core.ads.domain.model.RewardedAdSurface
import core.ads.domain.usecase.SendRewardedAdOutcomeUseCase
import kotlinx.coroutines.launch

@OptIn(DependsOnGoogleMobileAds::class)
@Composable
actual fun RewardedAdZone(
    show: Boolean,
    surface: RewardedAdSurface,
    adConfigHolder: AdConfigHolder,
    sendRewardedAdOutcomeUseCase: SendRewardedAdOutcomeUseCase,
    onRewardEarned: () -> Unit
) {
    BasicAds.Initialize()
    val activity = LocalActivity.current ?: return

    val coroutineScope = rememberCoroutineScope()
    val adConfig by adConfigHolder.adConfig.collectAsState()
    val adUnitId = adConfig?.rewardedAdUnitIdAndroid

    val sendAdOutcome: (RewardedAdOutcome) -> Unit = { outcome ->
        coroutineScope.launch { sendRewardedAdOutcomeUseCase(outcome = outcome, surface = surface) }
    }

    LaunchedEffect(show, adUnitId) {
        if (!show || adUnitId == null) return@LaunchedEffect
        val handler = RewardedAdHandler(activity)
        handler.load(
            adUnitId = adUnitId,
            onLoad = {
                presentRewardedAd(handler = handler, onRewardEarned = onRewardEarned, onAdOutcome = sendAdOutcome)
            },
            onFailure = { exception -> sendAdOutcome(RewardedAdOutcome.LoadFailed(exception = exception)) }
        )
    }
}

@OptIn(DependsOnGoogleMobileAds::class)
private fun presentRewardedAd(
    handler: RewardedAdHandler,
    onRewardEarned: () -> Unit,
    onAdOutcome: (RewardedAdOutcome) -> Unit
) {
    var isRewardEarned = false
    runCatching {
        handler.setListeners(
            onFailure = { exception -> onAdOutcome(RewardedAdOutcome.ShowFailed(exception = exception)) },
            onDismissed = {
                if (!isRewardEarned) onAdOutcome(RewardedAdOutcome.ClosedBeforeReward)
            }
        )
        handler.show {
            isRewardEarned = true
            onAdOutcome(RewardedAdOutcome.RewardEarned)
            onRewardEarned()
        }
    }.onFailure { throwable ->
        onAdOutcome(RewardedAdOutcome.ShowFailed(exception = throwable))
    }
}
