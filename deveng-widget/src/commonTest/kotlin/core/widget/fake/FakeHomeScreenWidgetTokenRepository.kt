package core.widget.fake

import core.widget.domain.repository.HomeScreenWidgetTokenRepository

class FakeHomeScreenWidgetTokenRepository : HomeScreenWidgetTokenRepository {
    var issuedWidgetToken: String = DEFAULT_ISSUED_WIDGET_TOKEN
    var revokeFailure: Exception? = null
    val issuedDeviceIdentifiers = mutableListOf<String>()
    val revokedWidgetTokens = mutableListOf<String>()

    override suspend fun issueWidgetToken(deviceIdentifier: String): String {
        issuedDeviceIdentifiers += deviceIdentifier
        return issuedWidgetToken
    }

    override suspend fun revokeWidgetToken(widgetToken: String) {
        revokedWidgetTokens += widgetToken
        revokeFailure?.let { failure -> throw failure }
    }

    private companion object {
        const val DEFAULT_ISSUED_WIDGET_TOKEN = "issued-widget-token"
    }
}
