package core.push.data.store

import com.russhwolf.settings.Settings
import core.push.domain.store.NotificationPermissionStorage

private const val KEY_IS_SYSTEM_PROMPT_SHOWN = "is_notification_system_prompt_shown"
private const val KEY_ASK_COUNT = "notification_permission_ask_count"
private const val KEY_LAST_ASKED_AT = "notification_permission_last_asked_at"

class NotificationPermissionStorageImpl(
    private val settings: Settings,
) : NotificationPermissionStorage {

    override fun isSystemPromptShown(): Boolean {
        return settings.getBoolean(KEY_IS_SYSTEM_PROMPT_SHOWN, false)
    }

    override fun setSystemPromptShown() {
        settings.putBoolean(KEY_IS_SYSTEM_PROMPT_SHOWN, true)
    }

    override fun getAskCount(): Int {
        return settings.getInt(KEY_ASK_COUNT, 0)
    }

    override fun getLastAskedAtEpochMilliseconds(): Long? {
        return settings.getLongOrNull(KEY_LAST_ASKED_AT)
    }

    override fun recordAsk(askedAtEpochMilliseconds: Long) {
        settings.putInt(KEY_ASK_COUNT, getAskCount() + 1)
        settings.putLong(KEY_LAST_ASKED_AT, askedAtEpochMilliseconds)
    }

    override fun clear() {
        settings.remove(KEY_IS_SYSTEM_PROMPT_SHOWN)
        settings.remove(KEY_ASK_COUNT)
        settings.remove(KEY_LAST_ASKED_AT)
    }
}
