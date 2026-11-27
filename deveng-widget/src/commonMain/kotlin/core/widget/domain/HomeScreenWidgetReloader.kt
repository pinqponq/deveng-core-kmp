package core.widget.domain

/**
 * Asks the platform to redraw the home screen widgets. Android refetches the widget content in the background;
 * iOS reloads the WidgetKit timelines and the extension fetches the content itself. Reloads made while the app
 * is in the foreground do not count against the iOS widget refresh budget.
 */
interface HomeScreenWidgetReloader {
    fun reloadHomeScreenWidgets()
}
