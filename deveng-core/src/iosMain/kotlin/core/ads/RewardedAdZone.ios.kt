package core.ads

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import app.lexilabs.basic.ads.BasicAds
import app.lexilabs.basic.ads.DependsOnGoogleMobileAds
import core.ads.config.domain.holder.AdConfigHolder
import core.ads.domain.model.RewardedAdOutcome
import core.ads.domain.model.RewardedAdSurface
import core.ads.domain.usecase.SendRewardedAdOutcomeUseCase
import core.ads.player.IosRewardedAdPlayer
import core.ads.player.RewardedAdEffect

@OptIn(DependsOnGoogleMobileAds::class)
@Composable
actual fun RewardedAdZone(
    show: Boolean,
    surface: RewardedAdSurface,
    adConfigHolder: AdConfigHolder,
    sendRewardedAdOutcomeUseCase: SendRewardedAdOutcomeUseCase,
    onRewardEarned: () -> Unit,
    onAdFailed: (RewardedAdOutcome.Failure) -> Unit
) {
    BasicAds.Initialize()

    val adConfig by adConfigHolder.adConfig.collectAsState()

    RewardedAdEffect(
        show = show,
        adUnitId = adConfig?.rewardedAdUnitIdIos,
        surface = surface,
        sendRewardedAdOutcomeUseCase = sendRewardedAdOutcomeUseCase,
        createAdPlayer = { adUnitId -> IosRewardedAdPlayer(adUnitId = adUnitId) },
        onRewardEarned = onRewardEarned,
        onAdFailed = onAdFailed,
    )
}
