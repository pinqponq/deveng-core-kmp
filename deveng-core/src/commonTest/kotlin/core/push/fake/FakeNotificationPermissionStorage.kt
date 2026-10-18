package core.push.fake

import core.push.domain.store.NotificationPermissionStorage

class FakeNotificationPermissionStorage : NotificationPermissionStorage {
    var storedIsSystemPromptShown = false
    var storedAskCount = 0
    var storedLastAskedAtEpochMilliseconds: Long? = null

    override fun isSystemPromptShown(): Boolean = storedIsSystemPromptShown

    override fun setSystemPromptShown() {
        storedIsSystemPromptShown = true
    }

    override fun getAskCount(): Int = storedAskCount

    override fun getLastAskedAtEpochMilliseconds(): Long? = storedLastAskedAtEpochMilliseconds

    override fun recordAsk(askedAtEpochMilliseconds: Long) {
        storedAskCount += 1
        storedLastAskedAtEpochMilliseconds = askedAtEpochMilliseconds
    }

    override fun clear() {
        storedIsSystemPromptShown = false
        storedAskCount = 0
        storedLastAskedAtEpochMilliseconds = null
    }
}
