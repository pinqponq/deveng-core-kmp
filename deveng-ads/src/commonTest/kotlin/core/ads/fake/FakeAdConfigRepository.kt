package core.ads.fake

import core.ads.config.domain.model.AdConfig
import core.ads.config.domain.repository.AdConfigRepository

class FakeAdConfigRepository : AdConfigRepository {

    var adConfig = AdConfig(rewardedAdUnitIdAndroid = "", rewardedAdUnitIdIos = "")
    var getAdConfigFailure: Exception? = null

    override suspend fun getAdConfig(): AdConfig {
        getAdConfigFailure?.let { failure -> throw failure }
        return adConfig
    }
}
