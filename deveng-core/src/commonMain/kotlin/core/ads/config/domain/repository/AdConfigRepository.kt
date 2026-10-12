package core.ads.config.domain.repository

import core.ads.config.domain.model.AdConfig

interface AdConfigRepository {
    suspend fun fetchAdConfig(): AdConfig
}
