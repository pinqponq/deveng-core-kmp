package core.ads

import androidx.compose.runtime.Composable
import core.ads.config.domain.holder.AdConfigHolder
import core.ads.domain.model.RewardedAdSurface
import core.ads.domain.usecase.SendRewardedAdOutcomeUseCase

@Composable
actual fun RewardedAdZone(
    show: Boolean,
    surface: RewardedAdSurface,
    adConfigHolder: AdConfigHolder,
    sendRewardedAdOutcomeUseCase: SendRewardedAdOutcomeUseCase,
    onRewardEarned: () -> Unit
) {
    // No-op: ads not supported on web
}
