package core.widget.android

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.glance.ExperimentalGlanceApi
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.compose
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition

/**
 * Draws every placed widget of a [GlanceAppWidget] without waiting on WorkManager: update() runs the Glance session
 * as a WorkManager job, and a backgrounded app may not get to run it until it is opened again (Samsung, Android 16).
 * compose() renders here and the result goes straight to AppWidgetManager. update() is still called so a session
 * that is already running, or starts later, shows the same content. The widget must use
 * [PreferencesGlanceStateDefinition].
 */
object HomeScreenWidgetSessionlessDrawer {

    /** [prepareWidgetState] runs before each widget is drawn, e.g. to bump a content version in its Glance state. */
    @OptIn(ExperimentalGlanceApi::class)
    suspend fun drawAllWidgets(
        context: Context,
        glanceAppWidget: GlanceAppWidget,
        prepareWidgetState: suspend (GlanceId) -> Unit,
    ) {
        val glanceAppWidgetManager = GlanceAppWidgetManager(context)
        val appWidgetManager = AppWidgetManager.getInstance(context)
        glanceAppWidgetManager.getGlanceIds(glanceAppWidget::class.java).forEach { glanceId ->
            prepareWidgetState(glanceId)
            val appWidgetId = glanceAppWidgetManager.getAppWidgetId(glanceId)
            val widgetState = getAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId)
            val widgetRemoteViews = glanceAppWidget.compose(
                context = context,
                id = glanceId,
                options = appWidgetManager.getAppWidgetOptions(appWidgetId),
                state = widgetState,
            )

            appWidgetManager.updateAppWidget(appWidgetId, widgetRemoteViews)
            glanceAppWidget.update(context, glanceId)
        }
    }
}
