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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

private val REWARDED_AD_LOAD_TIMEOUT = 15.seconds

@Composable
internal fun RewardedAdEffect(
    show: Boolean,
    adUnitId: String?,
    surface: RewardedAdSurface,
    sendRewardedAdOutcomeUseCase: SendRewardedAdOutcomeUseCase,
    createAdPlayer: (adUnitId: String) -> RewardedAdPlayer,
    onRewardEarned: () -> Unit,
    onAdFailed: (RewardedAdOutcome.Failure) -> Unit,
    loadTimeout: Duration = REWARDED_AD_LOAD_TIMEOUT,
) {
    val coroutineScope = rememberCoroutineScope()
    val currentSurface by rememberUpdatedState(surface)
    val currentOnRewardEarned by rememberUpdatedState(onRewardEarned)
    val currentOnAdFailed by rememberUpdatedState(onAdFailed)

    val adController = remember(adUnitId) {
        RewardedAdController(
            adUnitId = adUnitId,
            createAdPlayer = createAdPlayer,
            onAdOutcome = { outcome ->
                coroutineScope.launch {
                    sendRewardedAdOutcomeUseCase(outcome = outcome, surface = currentSurface)
                }
            },
            onRewardEarned = { currentOnRewardEarned() },
            onAdFailed = { failure -> currentOnAdFailed(failure) },
        )
    }

    LaunchedEffect(adController) {
        adController.preloadAd()
    }

    LaunchedEffect(show, adController) {
        if (!show) return@LaunchedEffect
        adController.requestAdShow()
        delay(loadTimeout)
        adController.expireShowRequest()
    }
}
