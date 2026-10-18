package core.push.domain

/**
 * The per-app settings of [PushNotifications].
 *
 * [isAppMessageShownBeforeSystemPrompt]: App Store Guideline 5.1.1(iv) rejects an app's own message
 * in front of a system permission prompt, so iOS apps pass false and go straight to the system
 * prompt while it can still be shown.
 */
data class PushNotificationOptions(
    val androidChannelId: String,
    val androidChannelName: String,
    val androidChannelDescription: String? = null,
    val softAskPolicy: SoftAskPolicy = SoftAskPolicy(),
    val isAppMessageShownBeforeSystemPrompt: Boolean = true,
)
