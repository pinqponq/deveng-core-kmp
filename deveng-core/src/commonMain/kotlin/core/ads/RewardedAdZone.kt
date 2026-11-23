package core.ads

import androidx.compose.runtime.Composable
import core.ads.config.domain.holder.AdConfigHolder
import core.ads.domain.model.RewardedAdOutcome
import core.ads.domain.model.RewardedAdSurface
import core.ads.domain.usecase.SendRewardedAdOutcomeUseCase

/**
 * Loads a rewarded ad in advance, once [adConfigHolder] has an ad unit id for this platform, and
 * shows it each time [show] turns true. The caller sets [show] back to false in [onRewardEarned]
 * and in [onAdFailed], so the next request can start a new attempt. Every outcome is reported
 * through [sendRewardedAdOutcomeUseCase]. A no-op on Desktop and Web.
 *
 * @param show Whether the ad should be shown now. The zone shows nothing itself while the ad
 * loads, so the caller shows its own loading state while [show] is true.
 * @param surface The screen the ad is shown on, sent with every outcome.
 * @param adConfigHolder Holds the remote ad unit ids.
 * @param sendRewardedAdOutcomeUseCase Reports the reward or the failure.
 * @param onRewardEarned Called once the user earned the reward.
 * @param onAdFailed Called when the ad failed to load, failed to show or was closed before the
 * reward.
 */
@Composable
expect fun RewardedAdZone(
    show: Boolean,
    surface: RewardedAdSurface,
    adConfigHolder: AdConfigHolder,
    sendRewardedAdOutcomeUseCase: SendRewardedAdOutcomeUseCase,
    onRewardEarned: () -> Unit,
    onAdFailed: (RewardedAdOutcome.Failure) -> Unit
)
