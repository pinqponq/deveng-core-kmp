package core.widget.domain.usecase

import com.russhwolf.settings.MapSettings
import core.widget.data.keyvalue.HomeScreenWidgetSessionStorageImpl
import core.widget.fake.FakeHomeScreenWidgetReloader
import core.widget.fake.FakeHomeScreenWidgetTokenRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class EndHomeScreenWidgetSessionUseCaseTest {
    private val tokenRepository = FakeHomeScreenWidgetTokenRepository()
    private val sessionStorage = HomeScreenWidgetSessionStorageImpl(widgetSettings = MapSettings())
    private val widgetReloader = FakeHomeScreenWidgetReloader()
    private val endHomeScreenWidgetSessionUseCase = EndHomeScreenWidgetSessionUseCase(
        homeScreenWidgetTokenRepository = tokenRepository,
        homeScreenWidgetSessionStorage = sessionStorage,
        homeScreenWidgetReloader = widgetReloader,
    )

    @Test
    fun invoke_withWidgetToken_revokesClearsAndReloads() = runTest {
        sessionStorage.saveWidgetSession(widgetToken = WIDGET_TOKEN, deviceIdentifier = DEVICE_IDENTIFIER, apiBaseUrl = API_BASE_URL)

        endHomeScreenWidgetSessionUseCase()

        assertEquals(listOf(WIDGET_TOKEN), tokenRepository.revokedWidgetTokens)
        assertNull(sessionStorage.getWidgetToken())
        assertEquals(1, widgetReloader.reloadCount)
    }

    @Test
    fun invoke_whenRevokeFails_stillClearsAndReloads() = runTest {
        sessionStorage.saveWidgetSession(widgetToken = WIDGET_TOKEN, deviceIdentifier = DEVICE_IDENTIFIER, apiBaseUrl = API_BASE_URL)
        tokenRepository.revokeFailure = IllegalStateException("revoke failed")

        endHomeScreenWidgetSessionUseCase()

        assertNull(sessionStorage.getWidgetToken())
        assertEquals(1, widgetReloader.reloadCount)
    }

    @Test
    fun invoke_whenRevokeIsCancelled_rethrowsWithoutClearing() = runTest {
        sessionStorage.saveWidgetSession(widgetToken = WIDGET_TOKEN, deviceIdentifier = DEVICE_IDENTIFIER, apiBaseUrl = API_BASE_URL)
        tokenRepository.revokeFailure = CancellationException("cancelled")

        assertFailsWith<CancellationException> { endHomeScreenWidgetSessionUseCase() }

        assertEquals(WIDGET_TOKEN, sessionStorage.getWidgetToken())
        assertEquals(0, widgetReloader.reloadCount)
    }

    @Test
    fun invoke_withoutWidgetToken_skipsRevokeButReloads() = runTest {
        endHomeScreenWidgetSessionUseCase()

        assertEquals(emptyList(), tokenRepository.revokedWidgetTokens)
        assertEquals(1, widgetReloader.reloadCount)
    }

    private companion object {
        const val WIDGET_TOKEN = "widget-token"
        const val DEVICE_IDENTIFIER = "device-identifier"
        const val API_BASE_URL = "https://api.example.com/"
    }
}
