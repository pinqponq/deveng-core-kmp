package global.deveng.core

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import core.presentation.component.CustomButton
import core.presentation.navigation.navigateClearingBackStack
import core.presentation.navigation.navigateSingle
import core.presentation.navigation.navigateToTab
import core.presentation.theme.CoreMediumTextStyle
import core.presentation.theme.CoreRegularTextStyle
import kotlinx.serialization.Serializable

@Serializable
internal data object NavigationDemoHomeRoute

@Serializable
internal data object NavigationDemoSettingsTabRoute

@Serializable
internal data object NavigationDemoProfileTabRoute

@Serializable
internal data class NavigationDemoDetailRoute(val detailNumber: Int)

@Composable
internal fun NavigationHelpersDemoScreen(onBack: () -> Unit) {
    val navController = rememberNavController()
    val currentEntry by navController.currentBackStackEntryAsState()
    val currentRouteName = currentEntry?.destination?.route.toDemoRouteName()
    val previousRouteName = navController.previousBackStackEntry?.destination?.route.toDemoRouteName()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(color = Color.White)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(all = 16.dp),
        verticalArrangement = Arrangement.spacedBy(space = 12.dp),
    ) {
        CustomButton(text = "Back", onClick = onBack)
        Text(text = "Current screen: $currentRouteName", style = CoreMediumTextStyle().copy(fontSize = 18.sp))
        Text(text = "Screen below: $previousRouteName", style = CoreRegularTextStyle().copy(fontSize = 14.sp))

        NavHost(navController = navController, startDestination = NavigationDemoHomeRoute) {
            composable<NavigationDemoHomeRoute> {}
            composable<NavigationDemoSettingsTabRoute> {}
            composable<NavigationDemoProfileTabRoute> {}
            composable<NavigationDemoDetailRoute> {}
        }

        NavigationHelpersDemoActions(navController = navController)
    }
}

@Composable
private fun NavigationHelpersDemoActions(navController: NavController) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(space = 8.dp),
    ) {
        CustomButton(
            text = "navigateSingle(Detail 1)",
            onClick = { navController.navigateSingle(NavigationDemoDetailRoute(detailNumber = 1)) },
        )
        CustomButton(
            text = "navigateSingle(Detail 2)",
            onClick = { navController.navigateSingle(NavigationDemoDetailRoute(detailNumber = 2)) },
        )
        CustomButton(
            text = "navigateToTab(Settings)",
            onClick = { navController.navigateToTab(NavigationDemoSettingsTabRoute) },
        )
        CustomButton(
            text = "navigateToTab(Profile)",
            onClick = { navController.navigateToTab(NavigationDemoProfileTabRoute) },
        )
        CustomButton(
            text = "navigateToTab(Profile, restoreState = false)",
            onClick = { navController.navigateToTab(destination = NavigationDemoProfileTabRoute, restoreState = false) },
        )
        CustomButton(
            text = "navigateToTab(Profile, anchor = Settings)",
            onClick = {
                navController.navigateToTab(
                    destination = NavigationDemoProfileTabRoute,
                    anchor = NavigationDemoSettingsTabRoute,
                )
            },
        )
        CustomButton(
            text = "navigateClearingBackStack(Home)",
            onClick = { navController.navigateClearingBackStack(NavigationDemoHomeRoute) },
        )
    }
}

private fun String?.toDemoRouteName(): String =
    this?.substringAfterLast('.') ?: "none"
