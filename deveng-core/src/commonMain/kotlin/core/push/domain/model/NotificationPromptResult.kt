package core.push.domain.model

/**
 * How a request to turn notifications on ended.
 */
enum class NotificationPromptResult {
    Granted,
    Denied,
    DeniedPermanently,
    Dismissed,

    /**
     * The system prompt failed and the permission state read afterwards did not tell the answer.
     */
    Unknown,

    /**
     * The platform would not show its dialog again, so the system notification settings were opened.
     */
    SettingsOpened
}
