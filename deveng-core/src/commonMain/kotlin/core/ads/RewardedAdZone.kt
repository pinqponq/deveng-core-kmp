package core.ads

import androidx.compose.runtime.Composable
import core.ads.config.domain.holder.AdConfigHolder
import core.ads.domain.model.RewardedAdSurface
import core.ads.domain.usecase.SendRewardedAdOutcomeUseCase

/**
 * Loads and shows a rewarded ad while [show] is true, once [adConfigHolder] has an ad unit id for
 * this platform. Every outcome is reported through [sendRewardedAdOutcomeUseCase]. A no-op on
 * Desktop and Web.
 *
 * @param show Whether the ad should be loaded and presented now.
 * @param surface The screen the ad is shown on, sent with every outcome.
 * @param adConfigHolder Holds the remote ad unit ids.
 * @param sendRewardedAdOutcomeUseCase Reports the reward or the failure.
 * @param onRewardEarned Called once the user earned the reward.
 */
@Composable
expect fun RewardedAdZone(
    show: Boolean,
    surface: RewardedAdSurface,
    adConfigHolder: AdConfigHolder,
    sendRewardedAdOutcomeUseCase: SendRewardedAdOutcomeUseCase,
    onRewardEarned: () -> Unit
)
