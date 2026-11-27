package core.attribution.data.datasource.remote.model.response

import kotlinx.serialization.Serializable

@Serializable
data class AdServicesAttributionResponse(
    val attribution: Boolean = false,
    val orgId: Long? = null,
    val campaignId: Long? = null,
    val conversionType: String? = null,
    val adGroupId: Long? = null,
    val countryOrRegion: String? = null,
    val keywordId: Long? = null,
    val adId: Long? = null,
    val clickDate: String? = null,
)
