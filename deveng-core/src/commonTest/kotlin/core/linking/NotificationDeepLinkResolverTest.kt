package core.linking

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class NotificationDeepLinkResolverTest {
    private val resolver = NotificationDeepLinkResolver(chatDeepLinkPrefix = "app://chat/")

    @Test
    fun resolve_withActionUrl_returnsItEvenForChatNotification() {
        val data = chatData() + ("actionUrl" to "app://gallery")

        assertEquals("app://gallery", resolver.resolve(data, notificationTitle = "Ada"))
    }

    @Test
    fun resolve_withLegacyDeepLink_returnsIt() {
        assertEquals("app://old", resolver.resolve(mapOf("deep_link" to "app://old"), notificationTitle = null))
    }

    @Test
    fun resolve_withActionUrlPreferredOverLegacyDeepLink() {
        val data = mapOf("actionUrl" to "app://new", "deep_link" to "app://old")

        assertEquals("app://new", resolver.resolve(data, notificationTitle = null))
    }

    @Test
    fun resolve_withBlankActionUrl_fallsBackToChatLink() {
        val data = chatData() + ("actionUrl" to "  ")

        assertEquals("app://chat/42", resolver.resolve(data, notificationTitle = null))
    }

    @Test
    fun resolve_withNonChatNotificationWithoutAddress_returnsNull() {
        assertNull(resolver.resolve(mapOf("notificationType" to "reminder", "senderUserId" to "42"), notificationTitle = "x"))
    }

    @Test
    fun resolve_withChatNotificationWithoutSender_returnsNull() {
        assertNull(resolver.resolve(mapOf("notificationType" to "new_message"), notificationTitle = "x"))
        assertNull(resolver.resolve(mapOf("notificationType" to "new_message", "senderUserId" to " "), notificationTitle = "x"))
    }

    @Test
    fun resolve_withChatNotificationAndNoName_returnsLinkWithoutPartnerName() {
        assertEquals("app://chat/42", resolver.resolve(chatData(), notificationTitle = null))
        assertEquals("app://chat/42", resolver.resolve(chatData(), notificationTitle = " "))
    }

    @Test
    fun resolve_withChatNotificationAndTitle_usesTitleAsPartnerName() {
        assertEquals("app://chat/42?partnerName=Ada%20L", resolver.resolve(chatData(), notificationTitle = "Ada L"))
    }

    @Test
    fun resolve_withDisplayNameInData_prefersItOverTitle() {
        val data = chatData() + ("senderDisplayName" to "Grace")

        assertEquals("app://chat/42?partnerName=Grace", resolver.resolve(data, notificationTitle = "Ada"))
    }

    @Test
    fun resolve_withBlankDisplayName_usesTitle() {
        val data = chatData() + ("senderDisplayName" to " ")

        assertEquals("app://chat/42?partnerName=Ada", resolver.resolve(data, notificationTitle = "Ada"))
    }

    @Test
    fun resolve_withSenderIdNeedingEscape_encodesIt() {
        val data = mapOf("notificationType" to "new_message", "senderUserId" to "a/b")

        assertEquals("app://chat/a%2Fb", resolver.resolve(data, notificationTitle = null))
    }

    private fun chatData() = mapOf("notificationType" to "new_message", "senderUserId" to "42")
}
