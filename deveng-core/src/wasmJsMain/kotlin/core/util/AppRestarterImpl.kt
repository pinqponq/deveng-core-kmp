package core.util

import kotlinx.browser.window

class AppRestarterImpl : AppRestarter {
    override fun restart() {
        window.location.reload()
    }
}
