package core.widget.data.keyvalue

import com.russhwolf.settings.Settings
import core.widget.domain.keyvalue.HomeScreenWidgetSessionStorage

/** Read by the iOS widget extension under these exact names (pinq-doq widget template HomeScreenWidgetSession.swift). */
private const val KEY_WIDGET_TOKEN = "home_screen_widget_token"
private const val KEY_DEVICE_IDENTIFIER = "home_screen_widget_device_identifier"
private const val KEY_API_BASE_URL = "home_screen_widget_api_base_url"

class HomeScreenWidgetSessionStorageImpl(
    private val widgetSettings: Settings,
) : HomeScreenWidgetSessionStorage {

    override fun getWidgetToken(): String? {
        return widgetSettings.getStringOrNull(KEY_WIDGET_TOKEN)?.ifBlank { null }
    }

    override fun saveWidgetSession(widgetToken: String, deviceIdentifier: String, apiBaseUrl: String) {
        widgetSettings.putString(KEY_WIDGET_TOKEN, widgetToken)
        widgetSettings.putString(KEY_DEVICE_IDENTIFIER, deviceIdentifier)
        widgetSettings.putString(KEY_API_BASE_URL, apiBaseUrl)
    }

    override fun clearWidgetSession() {
        widgetSettings.remove(KEY_WIDGET_TOKEN)
        widgetSettings.remove(KEY_DEVICE_IDENTIFIER)
        widgetSettings.remove(KEY_API_BASE_URL)
    }
}
