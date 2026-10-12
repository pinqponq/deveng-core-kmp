package core.widget.domain.usecase

import co.touchlab.kermit.Logger
import core.widget.domain.HomeScreenWidgetReloader
import core.widget.domain.keyvalue.HomeScreenWidgetSessionStorage
import core.widget.domain.repository.HomeScreenWidgetTokenRepository
import kotlinx.coroutines.CancellationException

/** Revokes the widget token and empties the widgets, so nothing of this account stays on the home screen. */
class EndHomeScreenWidgetSessionUseCase(
    private val homeScreenWidgetTokenRepository: HomeScreenWidgetTokenRepository,
    private val homeScreenWidgetSessionStorage: HomeScreenWidgetSessionStorage,
    private val homeScreenWidgetReloader: HomeScreenWidgetReloader,
) {
    suspend operator fun invoke() {
        val widgetToken = homeScreenWidgetSessionStorage.getWidgetToken()
        if (widgetToken != null) revokeWidgetToken(widgetToken)

        homeScreenWidgetSessionStorage.clearWidgetSession()
        homeScreenWidgetReloader.reloadHomeScreenWidgets()
    }

    /**
     * The local copy is dropped even when the server call fails, so the widget empties either way; the server
     * also revokes the token itself on unpair, and a leftover one can only read the latest widget of a couple
     * the user may still belong to.
     */
    private suspend fun revokeWidgetToken(widgetToken: String) {
        try {
            homeScreenWidgetTokenRepository.revokeWidgetToken(widgetToken = widgetToken)
        } catch (cancellationException: CancellationException) {
            throw cancellationException
        } catch (revokeException: Exception) {
            Logger.w(revokeException) { "[HomeScreenWidget] Widget token revoke failed" }
        }
    }
}
