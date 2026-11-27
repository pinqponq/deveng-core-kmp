package core.widget.domain.keyvalue

/**
 * What a home screen widget needs to call the API on its own: the widget token and the API address.
 * On iOS the widget runs in a separate extension process, so this lives in the App Group defaults the
 * extension reads.
 */
interface HomeScreenWidgetSessionStorage {
    fun getWidgetToken(): String?

    fun saveWidgetSession(widgetToken: String, deviceIdentifier: String, apiBaseUrl: String)

    fun clearWidgetSession()
}
