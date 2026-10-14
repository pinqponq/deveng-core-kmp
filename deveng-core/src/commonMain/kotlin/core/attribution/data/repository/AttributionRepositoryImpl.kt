package core.attribution.data.repository

import core.attribution.data.datasource.remote.AppleAdServicesService
import core.attribution.data.datasource.remote.model.mapper.AdServicesAttributionMapper
import core.attribution.domain.model.InstallAttribution
import core.attribution.domain.repository.AttributionRepository

class AttributionRepositoryImpl(
    private val appleAdServicesService: AppleAdServicesService,
    private val adServicesAttributionMapper: AdServicesAttributionMapper,
) : AttributionRepository {
    override suspend fun resolveAttribution(attributionToken: String): InstallAttribution? {
        val response = appleAdServicesService.fetchAttribution(attributionToken = attributionToken)
            ?: return null

        return adServicesAttributionMapper.mapToDomain(response = response)
    }
}
