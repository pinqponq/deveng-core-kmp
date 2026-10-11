package core.ads.fake

import core.ads.config.domain.model.AdConfig
import core.ads.config.domain.repository.AdConfigRepository

class FakeAdConfigRepository : AdConfigRepository {

    var adConfig = AdConfig(rewardedAdUnitIdAndroid = "", rewardedAdUnitIdIos = "")
    var fetchAdConfigFailure: Exception? = null

    override suspend fun fetchAdConfig(): AdConfig {
        fetchAdConfigFailure?.let { failure -> throw failure }
        return adConfig
    }
}
