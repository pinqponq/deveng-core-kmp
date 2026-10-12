package core.widget.ios

import co.touchlab.kermit.Logger
import core.widget.domain.HomeScreenWidgetReloader

/**
 * WidgetCenter is a Swift-only API, so the reload itself is registered from Swift at launch through
 * [setReloadHandler] (the app exposes it to Swift through its own framework). Until then — or on a build without
 * the widget — a reload is only logged.
 */
class IosHomeScreenWidgetReloader : HomeScreenWidgetReloader {

    override fun reloadHomeScreenWidgets() {
        val reloadHandler = reloadHomeScreenWidgetsHandler
        if (reloadHandler == null) {
            Logger.w { "[HomeScreenWidget] Reload requested before the Swift handler was registered" }
            return
        }
        reloadHandler()
    }

    companion object {
        private var reloadHomeScreenWidgetsHandler: (() -> Unit)? = null

        fun setReloadHandler(reloadHandler: () -> Unit) {
            reloadHomeScreenWidgetsHandler = reloadHandler
        }
    }
}
