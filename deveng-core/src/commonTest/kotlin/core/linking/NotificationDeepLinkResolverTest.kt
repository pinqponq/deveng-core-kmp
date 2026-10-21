package core.linking

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class NotificationDeepLinkResolverTest {
    private val resolver = NotificationDeepLinkResolver(chatDeepLinkPrefix = CHAT_PREFIX)

    @Test
    fun resolve_withActionUrl_returnsItEvenForChatNotification() {
        val data = chatData() + (ACTION_URL_KEY to "app://gallery")

        assertEquals("app://gallery", resolver.resolve(data, notificationTitle = PARTNER_NAME))
    }

    @Test
    fun resolve_withLegacyDeepLink_returnsIt() {
        assertEquals("app://old", resolver.resolve(mapOf(LEGACY_DEEP_LINK_KEY to "app://old"), notificationTitle = null))
    }

    @Test
    fun resolve_withActionUrlAndLegacyDeepLink_prefersActionUrl() {
        val data = mapOf(ACTION_URL_KEY to "app://new", LEGACY_DEEP_LINK_KEY to "app://old")

        assertEquals("app://new", resolver.resolve(data, notificationTitle = null))
    }

    @Test
    fun resolve_withBlankActionUrl_fallsBackToChatLink() {
        val data = chatData() + (ACTION_URL_KEY to "  ")

        assertEquals(CHAT_LINK, resolver.resolve(data, notificationTitle = null))
    }

    @Test
    fun resolve_withNonChatNotificationWithoutAddress_returnsNull() {
        assertNull(resolver.resolve(mapOf(NOTIFICATION_TYPE_KEY to "reminder", SENDER_USER_ID_KEY to SENDER_ID), notificationTitle = OTHER_TITLE))
    }

    @Test
    fun resolve_withChatNotificationWithoutSender_returnsNull() {
        assertNull(resolver.resolve(mapOf(NOTIFICATION_TYPE_KEY to CHAT_NOTIFICATION_TYPE), notificationTitle = OTHER_TITLE))
        assertNull(resolver.resolve(mapOf(NOTIFICATION_TYPE_KEY to CHAT_NOTIFICATION_TYPE, SENDER_USER_ID_KEY to " "), notificationTitle = OTHER_TITLE))
    }

    @Test
    fun resolve_withChatNotificationAndNoName_returnsLinkWithoutPartnerName() {
        assertEquals(CHAT_LINK, resolver.resolve(chatData(), notificationTitle = null))
        assertEquals(CHAT_LINK, resolver.resolve(chatData(), notificationTitle = " "))
    }

    @Test
    fun resolve_withChatNotificationAndTitle_usesTitleAsPartnerName() {
        assertEquals("$CHAT_LINK?partnerName=Ada%20L", resolver.resolve(chatData(), notificationTitle = "Ada L"))
    }

    @Test
    fun resolve_withDisplayNameInData_prefersItOverTitle() {
        val data = chatData() + (SENDER_DISPLAY_NAME_KEY to "Grace")

        assertEquals("$CHAT_LINK?partnerName=Grace", resolver.resolve(data, notificationTitle = PARTNER_NAME))
    }

    @Test
    fun resolve_withBlankDisplayName_usesTitle() {
        val data = chatData() + (SENDER_DISPLAY_NAME_KEY to " ")

        assertEquals("$CHAT_LINK?partnerName=Ada", resolver.resolve(data, notificationTitle = PARTNER_NAME))
    }

    @Test
    fun resolve_withSenderIdNeedingEscape_encodesIt() {
        val data = mapOf(NOTIFICATION_TYPE_KEY to CHAT_NOTIFICATION_TYPE, SENDER_USER_ID_KEY to "a/b")

        assertEquals("app://chat/a%2Fb", resolver.resolve(data, notificationTitle = null))
    }

    private fun chatData() = mapOf(NOTIFICATION_TYPE_KEY to CHAT_NOTIFICATION_TYPE, SENDER_USER_ID_KEY to SENDER_ID)

    private companion object {
        const val CHAT_PREFIX = "app://chat/"
        const val SENDER_ID = "42"
        const val CHAT_LINK = "$CHAT_PREFIX$SENDER_ID"
        const val CHAT_NOTIFICATION_TYPE = "new_message"
        const val NOTIFICATION_TYPE_KEY = "notificationType"
        const val SENDER_USER_ID_KEY = "senderUserId"
        const val PARTNER_NAME = "Ada"
        const val OTHER_TITLE = "x"
        const val ACTION_URL_KEY = "actionUrl"
        const val LEGACY_DEEP_LINK_KEY = "deep_link"
        const val SENDER_DISPLAY_NAME_KEY = "senderDisplayName"
    }
}
