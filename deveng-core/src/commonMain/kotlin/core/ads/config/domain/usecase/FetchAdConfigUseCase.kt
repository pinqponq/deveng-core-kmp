package core.ads.config.domain.usecase

import core.ads.config.domain.holder.AdConfigHolder
import core.ads.config.domain.model.AdConfig
import core.ads.config.domain.repository.AdConfigRepository

/**
 * Fetches the remote ad configuration and publishes it into [AdConfigHolder].
 * Called once at app startup. With [isTestAdUnitsEnabled] (debug builds) it always uses Google's
 * public test ad units instead of the backend value, so debug ad requests never risk serving
 * (and invalidating) a live ad unit.
 */
class FetchAdConfigUseCase(
    private val adConfigRepository: AdConfigRepository,
    private val adConfigHolder: AdConfigHolder,
    private val isTestAdUnitsEnabled: Boolean,
) {
    suspend operator fun invoke() {
        val adConfig = adConfigRepository.fetchAdConfig()
        adConfigHolder.update(if (isTestAdUnitsEnabled) TEST_AD_CONFIG else adConfig)
    }

    private companion object {
        val TEST_AD_CONFIG = AdConfig(
            rewardedAdUnitIdAndroid = "ca-app-pub-3940256099942544/5224354917",
            rewardedAdUnitIdIos = "ca-app-pub-3940256099942544/1712485313",
        )
    }
}
