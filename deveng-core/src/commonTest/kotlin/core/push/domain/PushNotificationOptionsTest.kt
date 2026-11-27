package core.push.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days

class PushNotificationOptionsTest {

    @Test
    fun create_withChannelOnly_usesNeutralDefaults() {
        val options = PushNotificationOptions(androidChannelId = CHANNEL_ID, androidChannelName = CHANNEL_NAME)

        assertEquals(CHANNEL_ID, options.androidChannelId)
        assertEquals(CHANNEL_NAME, options.androidChannelName)
        assertNull(options.androidChannelDescription)
        assertEquals(SoftAskPolicy(maxAskCount = 7, minimumInterval = 3.days), options.softAskPolicy)
        assertTrue(options.isAppMessageShownBeforeSystemPrompt)
    }

    @Test
    fun create_withChannelDescription_keepsDescription() {
        val options = PushNotificationOptions(
            androidChannelId = CHANNEL_ID,
            androidChannelName = CHANNEL_NAME,
            androidChannelDescription = CHANNEL_DESCRIPTION,
        )

        assertEquals(CHANNEL_DESCRIPTION, options.androidChannelDescription)
    }

    private companion object {
        const val CHANNEL_ID = "app_notifications"
        const val CHANNEL_NAME = "Notifications"
        const val CHANNEL_DESCRIPTION = "Messages from the app"
    }
}
