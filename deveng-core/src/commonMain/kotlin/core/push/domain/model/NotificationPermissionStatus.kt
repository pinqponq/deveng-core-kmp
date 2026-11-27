package core.push.domain.model

/**
 * What the app is allowed to do about notifications right now.
 */
enum class NotificationPermissionStatus {
    Granted,

    /**
     * The system dialog can still be shown, so the app may ask for itself.
     */
    Askable,

    /**
     * The platform will not show its dialog again; only the system settings page can turn
     * notifications back on.
     */
    BlockedBySystem
}
