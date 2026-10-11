package core.linking

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.navigation.NavController
import androidx.navigation.NavUri
import androidx.navigation.navOptions
import co.touchlab.kermit.Logger

/**
 * Maps an incoming link to the app's own navigation. Returns true when the link was handled; false
 * hands it to [DeepLinkListener], which navigates to it as a plain destination deep link.
 */
fun interface DeepLinkRouter {
    fun route(navController: NavController, uri: String): Boolean
}

@Composable
fun DeepLinkListener(
    navController: NavController,
    router: DeepLinkRouter
) {
    val currentRouter by rememberUpdatedState(router)

    DisposableEffect(navController) {
        ExternalUriHandler.listener = { uri ->
            navController.openDeepLink(uri = uri, router = currentRouter)
        }

        onDispose {
            ExternalUriHandler.listener = null
        }
    }
}

internal fun NavController.openDeepLink(uri: String, router: DeepLinkRouter) {
    try {
        if (router.route(navController = this, uri = uri)) return

        navigate(
            deepLink = NavUri(uri),
            navOptions = navOptions {
                launchSingleTop = true
            }
        )
    } catch (e: Exception) {
        Logger.e(e) { "[DeepLink] Could not open uri=$uri" }
    }
}
