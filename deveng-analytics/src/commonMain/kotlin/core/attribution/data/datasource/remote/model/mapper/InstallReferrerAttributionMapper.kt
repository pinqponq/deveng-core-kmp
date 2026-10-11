package core.attribution.data.datasource.remote.model.mapper

import core.attribution.domain.model.InstallAttribution
import io.ktor.http.parseQueryString

private const val KEY_GOOGLE_CLICK_ID = "gclid"
private const val KEY_CAMPAIGN_SOURCE = "utm_source"
private const val KEY_CAMPAIGN_MEDIUM = "utm_medium"
private const val KEY_CAMPAIGN_NAME = "utm_campaign"
private const val KEY_CAMPAIGN_TERM = "utm_term"
private const val KEY_CAMPAIGN_CONTENT = "utm_content"

/**
 * Google Play reports installs without a campaign as `utm_medium=organic` (store search / browse)
 * or `(not set)` (sideloaded or unknown source); neither is an ad attribution.
 */
private val UNATTRIBUTED_CAMPAIGN_MEDIUMS = setOf("organic", "(not set)")

class InstallReferrerAttributionMapper {
    fun mapToDomain(installReferrer: String): InstallAttribution {
        val referrerParameters = parseQueryString(installReferrer)
        val googleClickId = referrerParameters[KEY_GOOGLE_CLICK_ID]?.takeIf { it.isNotBlank() }
        val campaignMedium = referrerParameters[KEY_CAMPAIGN_MEDIUM]?.takeIf { it.isNotBlank() }
        val hasPaidCampaignMedium = campaignMedium != null && campaignMedium !in UNATTRIBUTED_CAMPAIGN_MEDIUMS

        return InstallAttribution(
            attributed = googleClickId != null || hasPaidCampaignMedium,
            orgId = null,
            campaignId = null,
            adGroupId = null,
            keywordId = null,
            adId = null,
            conversionType = null,
            countryOrRegion = null,
            clickDate = null,
            googleClickId = googleClickId,
            campaignSource = referrerParameters[KEY_CAMPAIGN_SOURCE],
            campaignMedium = campaignMedium,
            campaignName = referrerParameters[KEY_CAMPAIGN_NAME],
            campaignTerm = referrerParameters[KEY_CAMPAIGN_TERM],
            campaignContent = referrerParameters[KEY_CAMPAIGN_CONTENT],
        )
    }
}
