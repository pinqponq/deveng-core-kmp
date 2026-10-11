package core.analytics.domain.model

/**
 * A conversion event fanned out to three sinks — pinqloq ([pinqloqName], SCREAMING_SNAKE_CASE
 * client-log), Firebase Analytics ([firebaseName], snake_case, <= 40 chars), and Meta App Events
 * ([metaEventName]).
 *
 * [metaEventName] is either a Meta standard event constant (e.g. "fb_mobile_complete_registration",
 * "Subscribe") when one fits, or a snake_case custom event otherwise. The same raw string is valid
 * for both the Android and iOS Facebook SDKs.
 *
 * [fireOnce] marks milestones whose value is the FIRST occurrence; they are de-duplicated per install
 * under [pinqloqName]. Events every product shares are in [CommonAnalyticsEvent]; an app declares its
 * own events by implementing this interface.
 */
interface AnalyticsEvent {
    val pinqloqName: String
    val firebaseName: String
    val metaEventName: String
    val fireOnce: Boolean
}
