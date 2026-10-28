package core.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import co.touchlab.kermit.platformLogWriter
import core.testing.RecordingLogWriter
import kotlinx.serialization.Serializable
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class NavigationExtensionsTest {
    private val recordingLogWriter = RecordingLogWriter()

    @BeforeTest
    fun setUp() {
        Logger.setLogWriters(recordingLogWriter)
    }

    @AfterTest
    fun tearDown() {
        Logger.setLogWriters(platformLogWriter())
    }

    @Test
    fun navigateSingle_withTargetAlreadyCurrent_keepsSingleEntry() = runComposeUiTest {
        val navController = setUpHost()

        navigateOnIdle { navController.navigateSingle(DetailRoute) }
        navigateOnIdle { navController.navigateSingle(DetailRoute) }

        runOnIdle {
            assertTrue(navController.isCurrent<DetailRoute>())
            assertTrue(navController.previousBackStackEntry.isRoute<HomeRoute>())
        }
    }

    @Test
    fun navigateSingle_withSameRouteClassAndOtherArguments_opensNewArguments() = runComposeUiTest {
        val navController = setUpHost()

        navigateOnIdle { navController.navigateSingle(ItemRoute(itemId = FIRST_ITEM_ID)) }
        navigateOnIdle { navController.navigateSingle(ItemRoute(itemId = SECOND_ITEM_ID)) }

        runOnIdle {
            val currentItemRoute = navController.currentBackStackEntry?.toRoute<ItemRoute>()
            assertEquals(SECOND_ITEM_ID, currentItemRoute?.itemId)
            assertTrue(navController.previousBackStackEntry.isRoute<HomeRoute>())
        }
    }

    @Test
    fun navigateSingle_withoutClearBackStack_keepsPreviousScreen() = runComposeUiTest {
        val navController = setUpHost()

        navigateOnIdle { navController.navigateSingle(DetailRoute) }

        runOnIdle { assertTrue(navController.previousBackStackEntry.isRoute<HomeRoute>()) }
    }

    @Test
    fun navigateSingle_withClearBackStack_leavesOnlyTarget() = runComposeUiTest {
        val navController = setUpHost()

        navigateOnIdle { navController.navigateSingle(DetailRoute) }
        navigateOnIdle { navController.navigateSingle(destination = LoginRoute, clearBackStack = true) }

        runOnIdle {
            assertTrue(navController.isCurrent<LoginRoute>())
            assertNull(navController.previousBackStackEntry)
        }
    }

    @Test
    fun navigateSingle_withClearBackStack_replacesGraphEntry() = runComposeUiTest {
        val navController = setUpHost()
        val graphEntryBefore = runOnIdle { navController.getBackStackEntry<HostGraphRoute>() }

        navigateOnIdle { navController.navigateSingle(destination = LoginRoute, clearBackStack = true) }

        runOnIdle {
            val graphEntryAfter = navController.getBackStackEntry<HostGraphRoute>()
            assertNotSame(graphEntryBefore, graphEntryAfter)
        }
    }

    @Test
    fun navigateClearingBackStack_withScreensInStack_leavesOnlyTarget() = runComposeUiTest {
        val navController = setUpHost()

        navigateOnIdle { navController.navigateSingle(DetailRoute) }
        navigateOnIdle { navController.navigateClearingBackStack(LoginRoute) }

        runOnIdle {
            assertTrue(navController.isCurrent<LoginRoute>())
            assertNull(navController.previousBackStackEntry)
        }
    }

    @Test
    fun navigateToTab_withRestoreState_bringsBackScreensOfReopenedTab() = runComposeUiTest {
        val navController = setUpHost()

        navigateOnIdle { navController.navigateToTab(SettingsRoute) }
        navigateOnIdle { navController.navigateSingle(DetailRoute) }
        navigateOnIdle { navController.navigateToTab(LoginRoute) }
        navigateOnIdle { navController.navigateToTab(SettingsRoute) }

        runOnIdle {
            assertTrue(navController.isCurrent<DetailRoute>())
            assertTrue(navController.previousBackStackEntry.isRoute<SettingsRoute>())
        }
    }

    @Test
    fun navigateToTab_withoutRestoreState_opensRootOfReopenedTab() = runComposeUiTest {
        val navController = setUpHost()

        navigateOnIdle { navController.navigateToTab(destination = SettingsRoute, restoreState = false) }
        navigateOnIdle { navController.navigateSingle(DetailRoute) }
        navigateOnIdle { navController.navigateToTab(destination = LoginRoute, restoreState = false) }
        navigateOnIdle { navController.navigateToTab(destination = SettingsRoute, restoreState = false) }

        runOnIdle {
            assertTrue(navController.isCurrent<SettingsRoute>())
            assertTrue(navController.previousBackStackEntry.isRoute<HomeRoute>())
        }
    }

    @Test
    fun navigateToTab_withAnchor_removesScreensAboveAnchor() = runComposeUiTest {
        val navController = setUpHost()

        navigateOnIdle { navController.navigateSingle(SettingsRoute) }
        navigateOnIdle { navController.navigateSingle(DetailRoute) }
        navigateOnIdle {
            navController.navigateToTab(destination = LoginRoute, anchor = SettingsRoute, restoreState = false)
        }

        runOnIdle {
            assertTrue(navController.isCurrent<LoginRoute>())
            assertTrue(navController.previousBackStackEntry.isRoute<SettingsRoute>())
        }
    }

    @Test
    fun navigateToTab_withAnchorAndRestoreState_bringsBackScreensOfReopenedTab() = runComposeUiTest {
        val navController = setUpHost()

        navigateOnIdle { navController.navigateToTab(destination = SettingsRoute, anchor = HomeRoute) }
        navigateOnIdle { navController.navigateSingle(DetailRoute) }
        navigateOnIdle { navController.navigateToTab(destination = LoginRoute, anchor = HomeRoute) }
        navigateOnIdle { navController.navigateToTab(destination = SettingsRoute, anchor = HomeRoute) }

        runOnIdle {
            assertTrue(navController.isCurrent<DetailRoute>())
            assertTrue(navController.previousBackStackEntry.isRoute<SettingsRoute>())
        }
    }

    @Test
    fun navigateToTab_withStartScreenAsTarget_keepsSingleStartEntry() = runComposeUiTest {
        val navController = setUpHost()

        navigateOnIdle { navController.navigateSingle(DetailRoute) }
        navigateOnIdle { navController.navigateToTab(destination = HomeRoute, restoreState = false) }

        runOnIdle {
            assertTrue(navController.isCurrent<HomeRoute>())
            assertNull(navController.previousBackStackEntry)
        }
    }

    @Test
    fun navigateToTab_withTargetAlreadyCurrent_keepsScreenBelow() = runComposeUiTest {
        val navController = setUpHost()

        navigateOnIdle { navController.navigateSingle(SettingsRoute) }
        navigateOnIdle { navController.navigateToTab(destination = SettingsRoute, restoreState = false) }

        runOnIdle {
            assertTrue(navController.isCurrent<SettingsRoute>())
            assertTrue(navController.previousBackStackEntry.isRoute<HomeRoute>())
        }
    }

    @Test
    fun navigateSingle_withDestinationMissingFromGraph_logsErrorAndStays() = runComposeUiTest {
        val navController = setUpHost()

        navigateOnIdle { navController.navigateSingle(UnknownRoute) }

        runOnIdle {
            assertTrue(navController.isCurrent<HomeRoute>())
            assertNavigationFailureLogged()
        }
    }

    @Test
    fun navigateToTab_withDestinationMissingFromGraph_logsErrorAndStays() = runComposeUiTest {
        val navController = setUpHost()

        navigateOnIdle { navController.navigateToTab(UnknownRoute) }

        runOnIdle {
            assertTrue(navController.isCurrent<HomeRoute>())
            assertNavigationFailureLogged()
        }
    }

    @Test
    fun navigateSingle_beforeGraphIsSet_returnsWithoutLogging() = runComposeUiTest {
        var navController: NavController? = null
        setContent { navController = rememberNavController() }

        navigateOnIdle { navController?.navigateSingle(DetailRoute) }
        navigateOnIdle { navController?.navigateToTab(DetailRoute) }

        runOnIdle {
            assertNull(navController?.currentBackStackEntry)
            assertTrue(recordingLogWriter.logEntries.isEmpty())
        }
    }

    private fun ComposeUiTest.setUpHost(): NavController {
        var navController: NavController? = null
        setContent { navController = host() }
        waitForIdle()
        // Safe: setContent ran the composition above, which assigned the controller.
        return navController!!
    }

    private fun ComposeUiTest.navigateOnIdle(navigation: () -> Unit) {
        runOnIdle(navigation)
        waitForIdle()
    }

    private fun assertNavigationFailureLogged() {
        val errorEntry = recordingLogWriter.logEntries.single { it.severity == Severity.Error }
        assertTrue(errorEntry.message.contains(UnknownRoute.toString()))
        assertTrue(errorEntry.throwable is IllegalArgumentException)
    }

    private inline fun <reified T : Any> NavController.isCurrent(): Boolean =
        currentBackStackEntry.isRoute<T>()

    private inline fun <reified T : Any> NavBackStackEntry?.isRoute(): Boolean =
        this?.destination?.hasRoute(T::class) == true

    @Composable
    private fun host(): NavController {
        val navController = rememberNavController()
        NavHost(navController = navController, startDestination = HomeRoute, route = HostGraphRoute::class) {
            composable<HomeRoute> {}
            composable<DetailRoute> {}
            composable<SettingsRoute> {}
            composable<LoginRoute> {}
            composable<ItemRoute> {}
        }

        return navController
    }

    @Serializable
    data object HostGraphRoute

    @Serializable
    data object HomeRoute

    @Serializable
    data object DetailRoute

    @Serializable
    data object SettingsRoute

    @Serializable
    data object LoginRoute

    @Serializable
    data object UnknownRoute

    @Serializable
    data class ItemRoute(val itemId: Int)

    private companion object {
        const val FIRST_ITEM_ID = 1
        const val SECOND_ITEM_ID = 2
    }
}
