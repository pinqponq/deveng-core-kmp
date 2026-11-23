package core.ads.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import core.ads.domain.model.RewardedAdOutcome
import core.ads.domain.model.RewardedAdSurface
import core.ads.domain.usecase.SendRewardedAdOutcomeUseCase
import kotlinx.coroutines.launch

@Composable
internal fun RewardedAdEffect(
    show: Boolean,
    adUnitId: String?,
    surface: RewardedAdSurface,
    sendRewardedAdOutcomeUseCase: SendRewardedAdOutcomeUseCase,
    createAdPlayer: (adUnitId: String) -> RewardedAdPlayer,
    onRewardEarned: () -> Unit,
    onAdFailed: (RewardedAdOutcome.Failure) -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val currentSurface by rememberUpdatedState(surface)
    val currentOnRewardEarned by rememberUpdatedState(onRewardEarned)
    val currentOnAdFailed by rememberUpdatedState(onAdFailed)

    val adController = remember(adUnitId) {
        adUnitId?.let { rewardedAdUnitId ->
            RewardedAdController(
                createAdPlayer = { createAdPlayer(rewardedAdUnitId) },
                onAdOutcome = { outcome ->
                    coroutineScope.launch {
                        sendRewardedAdOutcomeUseCase(outcome = outcome, surface = currentSurface)
                    }
                },
                onRewardEarned = { currentOnRewardEarned() },
                onAdFailed = { failure -> currentOnAdFailed(failure) },
            )
        }
    }

    LaunchedEffect(adController) {
        adController?.preloadAd()
    }

    LaunchedEffect(show, adController) {
        if (!show) return@LaunchedEffect
        if (adController != null) {
            adController.requestAdShow()
        } else {
            val failure = RewardedAdOutcome.LoadFailed(IllegalStateException(MISSING_AD_UNIT_ID_MESSAGE))
            sendRewardedAdOutcomeUseCase(outcome = failure, surface = currentSurface)
            currentOnAdFailed(failure)
        }
    }
}

private const val MISSING_AD_UNIT_ID_MESSAGE = "No rewarded ad unit id in the ad config."

