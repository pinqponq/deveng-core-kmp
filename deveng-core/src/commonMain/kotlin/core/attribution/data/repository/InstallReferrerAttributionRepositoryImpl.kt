package core.attribution.data.repository

import core.attribution.data.datasource.remote.model.mapper.InstallReferrerAttributionMapper
import core.attribution.domain.model.InstallAttribution
import core.attribution.domain.repository.AttributionRepository

class InstallReferrerAttributionRepositoryImpl(
    private val installReferrerAttributionMapper: InstallReferrerAttributionMapper,
) : AttributionRepository {
    override suspend fun resolveAttribution(attributionToken: String): InstallAttribution {
        return installReferrerAttributionMapper.mapToDomain(installReferrer = attributionToken)
    }
}
