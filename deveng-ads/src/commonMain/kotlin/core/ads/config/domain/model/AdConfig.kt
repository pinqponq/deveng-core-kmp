package core.ads.config.domain.model

/**
 * Remote AdMob configuration fetched from the backend.
 * Only ad unit ids are remote; app ids stay hardcoded in the manifest/Info.plist.
 */
data class AdConfig(
    val rewardedAdUnitIdAndroid: String,
    val rewardedAdUnitIdIos: String,
)
