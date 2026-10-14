package core.attribution.domain.model

data class InstallAttribution(
    val isAttributed: Boolean,
    val organizationId: Long?,
    val campaignId: Long?,
    val adGroupId: Long?,
    val keywordId: Long?,
    val adId: Long?,
    val conversionType: String?,
    val countryOrRegion: String?,
    val clickDate: String?,
    val googleClickId: String?,
    val campaignSource: String?,
    val campaignMedium: String?,
    val campaignName: String?,
    val campaignTerm: String?,
    val campaignContent: String?,
    val inviteCode: String?,
)
