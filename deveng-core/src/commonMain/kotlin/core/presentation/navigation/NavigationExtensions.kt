package core.presentation.navigation

import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavOptionsBuilder
import co.touchlab.kermit.Logger

/**
 * Navigates to [destination] unless it is already the current screen.
 *
 * The current screen counts as the target when it has the same route class and that route has no
 * arguments. A route with arguments is always navigated to, so the same screen can open again with
 * other arguments (for example another item id). When the target is already current, nothing
 * happens, also when [clearBackStack] is true. The call does nothing while the navigation graph is
 * not set yet. A failed navigation is logged.
 *
 * @param destination The type-safe route object of the target screen.
 * @param clearBackStack When true, every screen in the back stack is removed before the target opens.
 */
fun NavController.navigateSingle(destination: Any, clearBackStack: Boolean = false) {
    val currentEntry = currentBackStackEntry ?: return
    if (currentEntry.isDestination(destination)) return

    navigateLoggingFailure(destination) {
        if (clearBackStack) {
            popUpTo(graph.id) { inclusive = true }
        }
        launchSingleTop = true
    }
}

/**
 * Navigates to [destination] after removing every screen in the back stack, so back leaves the app
 * from the target. Skips the call in the same cases as [navigateSingle].
 *
 * @param destination The type-safe route object of the target screen.
 */
fun NavController.navigateClearingBackStack(destination: Any) {
    navigateSingle(destination = destination, clearBackStack = true)
}

/**
 * Switches to a tab root screen the way a tab bar does. Every screen above [anchor] is removed, so
 * no other tab's screens stay buried in the back stack. Skips the call in the same cases as
 * [navigateSingle].
 *
 * @param destination The type-safe route object of the tab root screen.
 * @param anchor The route object of the screen every tab returns to; null uses the graph's start screen.
 * @param restoreState When true, the tab being left keeps its screens and state, and a tab opened
 * again gets back the screens and state it had. This also applies to the anchor screen itself:
 * switching to it brings back the screens that were above it when it was left. When false, the
 * tab always opens on its root screen.
 */
fun NavController.navigateToTab(destination: Any, anchor: Any? = null, restoreState: Boolean = true) {
    val currentEntry = currentBackStackEntry ?: return
    if (currentEntry.isDestination(destination)) return

    navigateLoggingFailure(destination) {
        if (anchor == null) {
            popUpTo(graph.findStartDestination().id) { saveState = restoreState }
        } else {
            popUpTo(anchor) { saveState = restoreState }
        }
        this.restoreState = restoreState
        launchSingleTop = true
    }
}

private fun NavBackStackEntry.isDestination(destination: Any): Boolean {
    val hasSameRouteClass = this.destination.hasRoute(destination::class)
    val hasRouteArguments = this.destination.arguments.isNotEmpty()
    return hasSameRouteClass && !hasRouteArguments
}

private fun NavController.navigateLoggingFailure(destination: Any, builder: NavOptionsBuilder.() -> Unit) {
    try {
        navigate(route = destination, builder = builder)
    } catch (exception: Exception) {
        Logger.e(exception) { "[Navigation] Could not navigate to destination=$destination" }
    }
}
