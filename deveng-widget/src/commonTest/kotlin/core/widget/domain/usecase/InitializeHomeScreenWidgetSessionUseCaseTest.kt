package core.widget.domain.usecase

import com.russhwolf.settings.MapSettings
import core.widget.data.keyvalue.HomeScreenWidgetSessionStorageImpl
import core.widget.fake.FakeHomeScreenWidgetReloader
import core.widget.fake.FakeHomeScreenWidgetTokenRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class InitializeHomeScreenWidgetSessionUseCaseTest {
    private val tokenRepository = FakeHomeScreenWidgetTokenRepository()
    private val widgetSettings = MapSettings()
    private val sessionStorage = HomeScreenWidgetSessionStorageImpl(widgetSettings = widgetSettings)
    private val widgetReloader = FakeHomeScreenWidgetReloader()
    private val initializeHomeScreenWidgetSessionUseCase = InitializeHomeScreenWidgetSessionUseCase(
        homeScreenWidgetTokenRepository = tokenRepository,
        homeScreenWidgetSessionStorage = sessionStorage,
        homeScreenWidgetReloader = widgetReloader,
        deviceIdentifierProvider = { DEVICE_IDENTIFIER },
        apiBaseUrl = API_BASE_URL,
    )

    @Test
    fun invoke_withoutWidgetToken_issuesAndStoresSessionThenReloads() = runTest {
        tokenRepository.issuedWidgetToken = ISSUED_WIDGET_TOKEN

        initializeHomeScreenWidgetSessionUseCase()

        assertEquals(listOf(DEVICE_IDENTIFIER), tokenRepository.issuedDeviceIdentifiers)
        assertEquals(ISSUED_WIDGET_TOKEN, sessionStorage.getWidgetToken())
        assertEquals(DEVICE_IDENTIFIER, widgetSettings.getStringOrNull(KEY_DEVICE_IDENTIFIER))
        assertEquals(API_BASE_URL, widgetSettings.getStringOrNull(KEY_API_BASE_URL))
        assertEquals(1, widgetReloader.reloadCount)
    }

    @Test
    fun invoke_withStoredWidgetToken_keepsTokenAndOnlyReloads() = runTest {
        sessionStorage.saveWidgetSession(widgetToken = STORED_WIDGET_TOKEN, deviceIdentifier = DEVICE_IDENTIFIER, apiBaseUrl = API_BASE_URL)

        initializeHomeScreenWidgetSessionUseCase()

        assertEquals(emptyList(), tokenRepository.issuedDeviceIdentifiers)
        assertEquals(STORED_WIDGET_TOKEN, sessionStorage.getWidgetToken())
        assertEquals(1, widgetReloader.reloadCount)
    }

    @Test
    fun invoke_whenIssueFails_rethrowsWithoutSavingOrReloading() = runTest {
        tokenRepository.issueFailure = IllegalStateException(ISSUE_FAILURE_MESSAGE)

        assertFailsWith<IllegalStateException> { initializeHomeScreenWidgetSessionUseCase() }

        assertNull(sessionStorage.getWidgetToken())
        assertEquals(0, widgetReloader.reloadCount)
    }

    private companion object {
        const val KEY_DEVICE_IDENTIFIER = "home_screen_widget_device_identifier"
        const val KEY_API_BASE_URL = "home_screen_widget_api_base_url"
        const val DEVICE_IDENTIFIER = "device-identifier"
        const val API_BASE_URL = "https://api.example.com/"
        const val ISSUED_WIDGET_TOKEN = "issued-widget-token"
        const val ISSUE_FAILURE_MESSAGE = "issue failed"
        const val STORED_WIDGET_TOKEN = "stored-widget-token"
    }
}
