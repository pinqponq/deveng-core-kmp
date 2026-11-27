package core.widget.domain.usecase

import com.russhwolf.settings.MapSettings
import core.widget.data.keyvalue.HomeScreenWidgetSessionStorageImpl
import core.widget.domain.WidgetTokenRejectedException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class SendWidgetTokenRequestUseCaseTest {
    private val sessionStorage = HomeScreenWidgetSessionStorageImpl(widgetSettings = MapSettings())
    private val sendWidgetTokenRequestUseCase = SendWidgetTokenRequestUseCase(homeScreenWidgetSessionStorage = sessionStorage)

    @Test
    fun invoke_withWidgetToken_sendsRequestWithTokenAndReturnsItsResult() = runTest {
        saveWidgetSession()
        val sentWidgetTokens = mutableListOf<String>()

        val requestResult = sendWidgetTokenRequestUseCase { widgetToken ->
            sentWidgetTokens += widgetToken
            REQUEST_RESULT
        }

        assertEquals(REQUEST_RESULT, requestResult)
        assertEquals(listOf(WIDGET_TOKEN), sentWidgetTokens)
    }

    @Test
    fun invoke_withoutWidgetToken_returnsNullWithoutSending() = runTest {
        var isRequestSent = false

        val requestResult = sendWidgetTokenRequestUseCase { isRequestSent = true }

        assertNull(requestResult)
        assertEquals(false, isRequestSent)
    }

    @Test
    fun invoke_whenTokenIsRejected_forgetsSessionAndRethrows() = runTest {
        saveWidgetSession()

        assertFailsWith<WidgetTokenRejectedException> {
            sendWidgetTokenRequestUseCase<Unit> { throw WidgetTokenRejectedException() }
        }

        assertNull(sessionStorage.getWidgetToken())
    }

    @Test
    fun invoke_whenRequestFailsOtherwise_keepsSessionAndRethrows() = runTest {
        saveWidgetSession()

        assertFailsWith<IllegalStateException> {
            sendWidgetTokenRequestUseCase<Unit> { throw IllegalStateException("network down") }
        }

        assertEquals(WIDGET_TOKEN, sessionStorage.getWidgetToken())
    }

    private fun saveWidgetSession() {
        sessionStorage.saveWidgetSession(widgetToken = WIDGET_TOKEN, deviceIdentifier = DEVICE_IDENTIFIER, apiBaseUrl = API_BASE_URL)
    }

    private companion object {
        const val WIDGET_TOKEN = "widget-token"
        const val DEVICE_IDENTIFIER = "device-identifier"
        const val API_BASE_URL = "https://api.example.com/"
        const val REQUEST_RESULT = "latest-widget"
    }
}
