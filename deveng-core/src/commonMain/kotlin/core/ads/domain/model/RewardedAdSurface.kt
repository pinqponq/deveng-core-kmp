package core.ads.domain.model

/**
 * The screen a rewarded ad is shown on, reported with every ad outcome. Apps implement it with an
 * enum of their own surfaces; [name] is the value sent to the logs.
 */
interface RewardedAdSurface {
    val name: String
}
