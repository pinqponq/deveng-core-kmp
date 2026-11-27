package core.widget.fake

import core.widget.domain.HomeScreenWidgetReloader

class FakeHomeScreenWidgetReloader : HomeScreenWidgetReloader {
    var reloadCount: Int = 0
        private set

    override fun reloadHomeScreenWidgets() {
        reloadCount++
    }
}
