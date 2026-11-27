package core.widget.domain.usecase

import core.widget.domain.HomeScreenWidgetReloader
import core.widget.domain.keyvalue.HomeScreenWidgetSessionStorage
import core.widget.domain.repository.HomeScreenWidgetTokenRepository

/**
 * Makes sure the widgets can reach the API on their own. Run it whenever a signed-in session starts, so
 * users who were signed in before the widget shipped get a widget token too, not only fresh logins.
 */
class InitializeHomeScreenWidgetSessionUseCase(
    private val homeScreenWidgetTokenRepository: HomeScreenWidgetTokenRepository,
    private val homeScreenWidgetSessionStorage: HomeScreenWidgetSessionStorage,
    private val homeScreenWidgetReloader: HomeScreenWidgetReloader,
    private val deviceIdentifierProvider: () -> String,
    private val apiBaseUrl: String,
) {
    suspend operator fun invoke() {
        if (homeScreenWidgetSessionStorage.getWidgetToken() == null) {
            val deviceIdentifier = deviceIdentifierProvider()
            val widgetToken = homeScreenWidgetTokenRepository.issueWidgetToken(deviceIdentifier = deviceIdentifier)
            homeScreenWidgetSessionStorage.saveWidgetSession(
                widgetToken = widgetToken,
                deviceIdentifier = deviceIdentifier,
                apiBaseUrl = apiBaseUrl,
            )
        }

        homeScreenWidgetReloader.reloadHomeScreenWidgets()
    }
}
