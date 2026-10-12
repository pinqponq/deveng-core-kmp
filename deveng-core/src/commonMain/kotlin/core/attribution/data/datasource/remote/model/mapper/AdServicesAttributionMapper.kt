package core.attribution.data.datasource.remote.model.mapper

import core.attribution.data.datasource.remote.model.response.AdServicesAttributionResponse
import core.attribution.domain.model.InstallAttribution

class AdServicesAttributionMapper {
    fun mapToDomain(response: AdServicesAttributionResponse): InstallAttribution {
        return InstallAttribution(
            attributed = response.attribution,
            organizationId = response.orgId,
            campaignId = response.campaignId,
            adGroupId = response.adGroupId,
            keywordId = response.keywordId,
            adId = response.adId,
            conversionType = response.conversionType,
            countryOrRegion = response.countryOrRegion,
            clickDate = response.clickDate,
            googleClickId = null,
            campaignSource = null,
            campaignMedium = null,
            campaignName = null,
            campaignTerm = null,
            campaignContent = null,
        )
    }
}
