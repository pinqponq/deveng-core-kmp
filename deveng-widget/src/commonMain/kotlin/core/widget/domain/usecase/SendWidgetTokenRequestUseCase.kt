package core.widget.domain.usecase

import core.widget.domain.WidgetTokenRejectedException
import core.widget.domain.keyvalue.HomeScreenWidgetSessionStorage

/**
 * Runs a request authenticated with the widget token instead of the app session. Returns null when there is no
 * widget session (signed out). A rejected token is forgotten, so the next signed-in start issues a fresh one
 * instead of reusing the dead token forever.
 */
class SendWidgetTokenRequestUseCase(
    private val homeScreenWidgetSessionStorage: HomeScreenWidgetSessionStorage,
) {
    suspend operator fun <T> invoke(widgetRequest: suspend (widgetToken: String) -> T): T? {
        val widgetToken = homeScreenWidgetSessionStorage.getWidgetToken() ?: return null

        return try {
            widgetRequest(widgetToken)
        } catch (rejectedException: WidgetTokenRejectedException) {
            homeScreenWidgetSessionStorage.clearWidgetSession()
            throw rejectedException
        }
    }
}
