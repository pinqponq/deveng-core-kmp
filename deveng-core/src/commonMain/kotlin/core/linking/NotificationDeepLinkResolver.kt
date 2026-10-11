package core.linking

import core.util.percentEncodeUriComponent

/**
 * Resolves the address a push notification should open.
 *
 * An address sent by the server ("actionUrl") always wins, because the sender knowing the route is the
 * preferred design. Chat notifications carry only the notification type and the sender id, so their
 * address is built from [chatDeepLinkPrefix]; once the chat SDK sends "actionUrl" that branch stops
 * being reached, since the server value is checked first. The payload keys (`actionUrl`, `deep_link`,
 * `notificationType`, `senderUserId`, `senderDisplayName`), the `new_message` type and the `partnerName`
 * query parameter are the shared chat notification contract of Deveng apps, not per-app settings.
 */
class NotificationDeepLinkResolver(
    private val chatDeepLinkPrefix: String
) {
    /**
     * @param notificationData the notification's data map.
     * @param notificationTitle the notification title when available. For chat notifications it carries
     *   the sender's display name, which is the only source unless the server sends the name separately.
     *   Without either the address is still built; only the chat title opens empty.
     */
    fun resolve(notificationData: Map<String, String>, notificationTitle: String?): String? {
        val actionUrl = notificationData[ACTION_URL_KEY] ?: notificationData[LEGACY_ACTION_URL_KEY]
        if (!actionUrl.isNullOrBlank()) return actionUrl

        return resolveChatDeepLink(notificationData, notificationTitle)
    }

    private fun resolveChatDeepLink(
        notificationData: Map<String, String>,
        notificationTitle: String?
    ): String? {
        if (notificationData[NOTIFICATION_TYPE_KEY] != NEW_MESSAGE_NOTIFICATION_TYPE) return null

        val senderUserId = notificationData[SENDER_USER_ID_KEY]
        if (senderUserId.isNullOrBlank()) return null

        // The chat opens by the sender's user id, not a conversation id: for the recipient the
        // sender is by definition the other side of the chat.
        val chatDeepLink = chatDeepLinkPrefix + senderUserId.percentEncodeUriComponent()

        // Firebase delivers only the data map, never the title, while the app is closed or in the
        // background on Android, so the name is read from the data first.
        val partnerName = notificationData[SENDER_DISPLAY_NAME_KEY]?.takeIf { it.isNotBlank() }
            ?: notificationTitle?.takeIf { it.isNotBlank() }
            ?: return chatDeepLink

        return "$chatDeepLink?$PARTNER_NAME_QUERY_PARAMETER=${partnerName.percentEncodeUriComponent()}"
    }

    private companion object {
        const val ACTION_URL_KEY = "actionUrl"
        const val LEGACY_ACTION_URL_KEY = "deep_link"
        const val NOTIFICATION_TYPE_KEY = "notificationType"
        const val SENDER_USER_ID_KEY = "senderUserId"
        const val SENDER_DISPLAY_NAME_KEY = "senderDisplayName"
        const val NEW_MESSAGE_NOTIFICATION_TYPE = "new_message"
        const val PARTNER_NAME_QUERY_PARAMETER = "partnerName"
    }
}
