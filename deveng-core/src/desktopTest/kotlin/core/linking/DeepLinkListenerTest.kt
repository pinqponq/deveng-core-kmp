package core.linking

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import androidx.navigation.NavController
import androidx.navigation.NavDeepLink
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navDeepLink
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@OptIn(ExperimentalTestApi::class)
class DeepLinkListenerTest {
    @AfterTest
    fun tearDown() {
        ExternalUriHandler.listener = null
    }

    @Test
    fun onNewUri_withRouterHandlingLink_skipsPlainNavigation() = runComposeUiTest {
        val routed = mutableListOf<String>()
        var navController: NavController? = null
        setContent {
            Host(router = { _, uri -> routed += uri; true }, onController = { navController = it })
        }

        ExternalUriHandler.onNewUri("app://detail")
        waitForIdle()

        assertEquals(listOf("app://detail"), routed)
        assertEquals("start", navController?.currentBackStackEntry?.destination?.route)
    }

    @Test
    fun onNewUri_withRouterDecliningLink_navigatesToMatchingDestination() = runComposeUiTest {
        var navController: NavController? = null
        setContent {
            Host(router = { _, _ -> false }, onController = { navController = it })
        }

        ExternalUriHandler.onNewUri("app://detail")
        waitForIdle()

        assertEquals("detail", navController?.currentBackStackEntry?.destination?.route)
        runOnIdle { navController?.popBackStack() }
    }

    @Test
    fun onNewUri_withDefaultRouter_navigatesToMatchingDestination() = runComposeUiTest {
        var navController: NavController? = null
        setContent {
            Host(router = null, onController = { navController = it })
        }

        ExternalUriHandler.onNewUri("app://detail")
        waitForIdle()

        assertEquals("detail", navController?.currentBackStackEntry?.destination?.route)
        runOnIdle { navController?.popBackStack() }
    }

    @Test
    fun onNewUri_withSameLinkTwice_needsOnlyOneBackToLeave() = runComposeUiTest {
        var navController: NavController? = null
        setContent {
            Host(router = null, onController = { navController = it })
        }

        ExternalUriHandler.onNewUri("app://detail")
        waitForIdle()
        ExternalUriHandler.onNewUri("app://detail")
        waitForIdle()

        runOnIdle { navController?.popBackStack() }
        waitForIdle()

        assertNotEquals("detail", navController?.currentBackStackEntry?.destination?.route)
    }

    @Test
    fun onNewUri_withLinkMatchingNothing_isIgnoredWithoutCrashing() = runComposeUiTest {
        var navController: NavController? = null
        setContent {
            Host(router = { _, _ -> false }, onController = { navController = it })
        }

        ExternalUriHandler.onNewUri("app://unknown")
        waitForIdle()

        assertEquals("start", navController?.currentBackStackEntry?.destination?.route)
    }

    @Test
    fun onNewUri_withRouterThrowing_isIgnoredWithoutCrashing() = runComposeUiTest {
        var navController: NavController? = null
        setContent {
            Host(router = { _, _ -> error("boom") }, onController = { navController = it })
        }

        ExternalUriHandler.onNewUri("app://detail")
        waitForIdle()

        assertEquals("start", navController?.currentBackStackEntry?.destination?.route)
    }

    @Test
    fun dispose_removesListener() = runComposeUiTest {
        val showHost = mutableStateOf(true)
        setContent {
            if (showHost.value) Host(router = null, onController = {})
        }
        waitForIdle()
        assertNotNull(ExternalUriHandler.listener)

        showHost.value = false
        waitForIdle()

        assertNull(ExternalUriHandler.listener)
    }

    @Composable
    private fun Host(router: DeepLinkRouter?, onController: (NavController) -> Unit) {
        val navController = rememberNavController()
        onController(navController)
        NavHost(navController = navController, startDestination = "start") {
            composable("start") {}
            composable("detail", deepLinks = listOf<NavDeepLink>(navDeepLink { uriPattern = "app://detail" })) {}
        }
        if (router == null) DeepLinkListener(navController) else DeepLinkListener(navController, router)
    }
}
