package core.push.data.store

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NotificationPermissionStorageImplTest {

    private val notificationPermissionStorage = NotificationPermissionStorageImpl(settings = MapSettings())

    @Test
    fun read_withEmptySettings_hasNoPromptAndNoAsks() {
        assertFalse(notificationPermissionStorage.isSystemPromptShown())
        assertEquals(0, notificationPermissionStorage.getAskCount())
        assertNull(notificationPermissionStorage.getLastAskedAtEpochMilliseconds())
    }

    @Test
    fun setSystemPromptShown_onEmptySettings_marksPromptShown() {
        notificationPermissionStorage.setSystemPromptShown()

        assertTrue(notificationPermissionStorage.isSystemPromptShown())
    }

    @Test
    fun recordAsk_twice_countsAsksAndKeepsLatestTime() {
        notificationPermissionStorage.recordAsk(askedAtEpochMilliseconds = FIRST_ASK_AT)
        notificationPermissionStorage.recordAsk(askedAtEpochMilliseconds = SECOND_ASK_AT)

        assertEquals(2, notificationPermissionStorage.getAskCount())
        assertEquals(SECOND_ASK_AT, notificationPermissionStorage.getLastAskedAtEpochMilliseconds())
    }

    @Test
    fun clear_afterAsks_resetsEverything() {
        notificationPermissionStorage.setSystemPromptShown()
        notificationPermissionStorage.recordAsk(askedAtEpochMilliseconds = FIRST_ASK_AT)

        notificationPermissionStorage.clear()

        assertFalse(notificationPermissionStorage.isSystemPromptShown())
        assertEquals(0, notificationPermissionStorage.getAskCount())
        assertNull(notificationPermissionStorage.getLastAskedAtEpochMilliseconds())
    }

    private companion object {
        const val FIRST_ASK_AT = 1_000L
        const val SECOND_ASK_AT = 2_000L
    }
}
