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

/**
 * Preloads a rewarded ad as soon as [adUnitId] is known and shows it each time [show] turns true.
 * Every outcome is reported through [sendRewardedAdOutcomeUseCase] before the matching callback
 * runs.
 */
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
        adUnitId?.let { loadedAdUnitId ->
            RewardedAdController(
                createAdPlayer = { createAdPlayer(loadedAdUnitId) },
                onAdOutcome = { outcome ->
                    coroutineScope.launch {
                        sendRewardedAdOutcomeUseCase(outcome = outcome, surface = currentSurface)
                    }
                    when (outcome) {
                        RewardedAdOutcome.RewardEarned -> currentOnRewardEarned()
                        is RewardedAdOutcome.Failure -> currentOnAdFailed(outcome)
                    }
                },
            )
        }
    }

    LaunchedEffect(adController) {
        adController?.preloadAd()
    }

    LaunchedEffect(show, adController) {
        if (show) adController?.requestAdShow()
    }
}
