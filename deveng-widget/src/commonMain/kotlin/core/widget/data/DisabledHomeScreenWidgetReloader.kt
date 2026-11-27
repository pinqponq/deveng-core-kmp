package core.widget.data

import core.widget.domain.HomeScreenWidgetReloader

/** For platforms without home screen widgets (desktop, web). */
class DisabledHomeScreenWidgetReloader : HomeScreenWidgetReloader {
    override fun reloadHomeScreenWidgets() = Unit
}
