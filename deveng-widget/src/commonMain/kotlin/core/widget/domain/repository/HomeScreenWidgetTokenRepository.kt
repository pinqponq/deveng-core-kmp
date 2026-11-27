package core.widget.domain.repository

interface HomeScreenWidgetTokenRepository {
    /** Issues a widget token for this device with the current session; it replaces the device's previous one. */
    suspend fun issueWidgetToken(deviceIdentifier: String): String

    suspend fun revokeWidgetToken(widgetToken: String)
}
