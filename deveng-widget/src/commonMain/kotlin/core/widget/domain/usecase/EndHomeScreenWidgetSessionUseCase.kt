package core.widget.domain.usecase

import co.touchlab.kermit.Logger
import core.widget.domain.HomeScreenWidgetReloader
import core.widget.domain.WIDGET_LOG_TAG
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
        if (widgetToken != null) {
            // The local copy is dropped even when the server call fails, so the widget empties either way; a leftover
            // token on the server can only make the widget's read-only calls until the server revokes it.
            try {
                homeScreenWidgetTokenRepository.revokeWidgetToken(widgetToken = widgetToken)
            } catch (cancellationException: CancellationException) {
                throw cancellationException
            } catch (revokeException: Exception) {
                Logger.w(revokeException) { "$WIDGET_LOG_TAG Widget token revoke failed" }
            }
        }

        homeScreenWidgetSessionStorage.clearWidgetSession()
        homeScreenWidgetReloader.reloadHomeScreenWidgets()
    }
}
