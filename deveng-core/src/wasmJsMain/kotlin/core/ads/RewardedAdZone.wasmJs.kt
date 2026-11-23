package core.ads

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import core.ads.config.domain.holder.AdConfigHolder
import core.ads.domain.model.RewardedAdOutcome
import core.ads.domain.model.RewardedAdSurface
import core.ads.domain.usecase.SendRewardedAdOutcomeUseCase

private const val UNSUPPORTED_PLATFORM_MESSAGE = "Rewarded ads are not supported on web."

@Composable
actual fun RewardedAdZone(
    show: Boolean,
    surface: RewardedAdSurface,
    adConfigHolder: AdConfigHolder,
    sendRewardedAdOutcomeUseCase: SendRewardedAdOutcomeUseCase,
    onRewardEarned: () -> Unit,
    onAdFailed: (RewardedAdOutcome.Failure) -> Unit
) {
    val currentOnAdFailed by rememberUpdatedState(onAdFailed)

    LaunchedEffect(show) {
        if (show) {
            currentOnAdFailed(
                RewardedAdOutcome.LoadFailed(exception = UnsupportedOperationException(UNSUPPORTED_PLATFORM_MESSAGE))
            )
        }
    }
}

