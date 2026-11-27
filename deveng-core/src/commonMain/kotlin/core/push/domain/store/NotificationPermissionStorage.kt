package core.push.domain.store

/**
 * Remembers how the notification permission ask went, because the platforms do not.
 *
 * Android cannot tell "never asked" apart from "denied for good" once the system stops showing
 * its dialog, so [isSystemPromptShown] is what makes that distinction.
 */
interface NotificationPermissionStorage {
    fun isSystemPromptShown(): Boolean
    fun setSystemPromptShown()
    fun getAskCount(): Int

    /** Null until the app has asked for the first time. */
    fun getLastAskedAtEpochMilliseconds(): Long?
    fun recordAsk(askedAtEpochMilliseconds: Long)
    fun clear()
}
