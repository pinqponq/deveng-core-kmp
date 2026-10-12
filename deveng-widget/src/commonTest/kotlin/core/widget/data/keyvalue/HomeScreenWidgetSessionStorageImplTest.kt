package core.widget.data.keyvalue

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class HomeScreenWidgetSessionStorageImplTest {
    private val widgetSettings = MapSettings()
    private val sessionStorage = HomeScreenWidgetSessionStorageImpl(widgetSettings = widgetSettings)

    @Test
    fun saveWidgetSession_writesTheKeysTheSwiftExtensionReads() {
        sessionStorage.saveWidgetSession(widgetToken = WIDGET_TOKEN, deviceIdentifier = DEVICE_IDENTIFIER, apiBaseUrl = API_BASE_URL)

        assertEquals(WIDGET_TOKEN, widgetSettings.getStringOrNull(KEY_WIDGET_TOKEN))
        assertEquals(DEVICE_IDENTIFIER, widgetSettings.getStringOrNull(KEY_DEVICE_IDENTIFIER))
        assertEquals(API_BASE_URL, widgetSettings.getStringOrNull(KEY_API_BASE_URL))
        assertEquals(WIDGET_TOKEN, sessionStorage.getWidgetToken())
    }

    @Test
    fun getWidgetToken_withBlankStoredToken_returnsNull() {
        widgetSettings.putString(KEY_WIDGET_TOKEN, " ")

        assertNull(sessionStorage.getWidgetToken())
    }

    @Test
    fun getWidgetToken_withoutSession_returnsNull() {
        assertNull(sessionStorage.getWidgetToken())
    }

    @Test
    fun clearWidgetSession_removesOnlyTheWidgetSessionKeys() {
        widgetSettings.putString(UNRELATED_KEY, UNRELATED_VALUE)
        sessionStorage.saveWidgetSession(widgetToken = WIDGET_TOKEN, deviceIdentifier = DEVICE_IDENTIFIER, apiBaseUrl = API_BASE_URL)

        sessionStorage.clearWidgetSession()

        assertEquals(setOf(UNRELATED_KEY), widgetSettings.keys)
        assertNull(sessionStorage.getWidgetToken())
    }

    private companion object {
        const val KEY_WIDGET_TOKEN = "home_screen_widget_token"
        const val KEY_DEVICE_IDENTIFIER = "home_screen_widget_device_identifier"
        const val KEY_API_BASE_URL = "home_screen_widget_api_base_url"
        const val WIDGET_TOKEN = "widget-token"
        const val DEVICE_IDENTIFIER = "device-identifier"
        const val API_BASE_URL = "https://api.example.com/"
        const val UNRELATED_KEY = "TOKEN"
        const val UNRELATED_VALUE = "app-session-token"
    }
}
