package core.presentation.component.navigationmenu

import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Density
import core.presentation.theme.AppTheme
import core.testing.SCREENSHOT_DENSITY
import core.testing.screenshotPath
import global.deveng.deveng_core.generated.resources.Res
import global.deveng.deveng_core.generated.resources.shared_ic_calendar
import global.deveng.deveng_core.generated.resources.shared_ic_find
import global.deveng.deveng_core.generated.resources.shared_ic_person
import io.github.takahirom.roborazzi.captureRoboImage
import kotlin.test.Test
import org.jetbrains.compose.resources.DrawableResource

@OptIn(ExperimentalTestApi::class)
class NavigationMenuScreenshotTest {

    @Test
    fun verticalMode_expandedAtStart_matchesBaseline() = captureNavigationMenu(
        name = "navigation_menu_vertical_expanded_start",
        menuMode = MenuMode.Vertical,
        menuAlignment = MenuAlignment.Start,
        isExpanded = true
    )

    @Test
    fun verticalMode_collapsedAtStart_matchesBaseline() = captureNavigationMenu(
        name = "navigation_menu_vertical_collapsed_start",
        menuMode = MenuMode.Vertical,
        menuAlignment = MenuAlignment.Start,
        isExpanded = false
    )

    @Test
    fun verticalMode_expandedAtEnd_matchesBaseline() = captureNavigationMenu(
        name = "navigation_menu_vertical_expanded_end",
        menuMode = MenuMode.Vertical,
        menuAlignment = MenuAlignment.End,
        isExpanded = true
    )

    @Test
    fun horizontalMode_withSelectedItem_matchesBaseline() = captureNavigationMenu(
        name = "navigation_menu_horizontal",
        menuMode = MenuMode.Horizontal,
        menuAlignment = MenuAlignment.Start,
        isExpanded = false
    )

    private fun captureNavigationMenu(
        name: String,
        menuMode: MenuMode,
        menuAlignment: MenuAlignment,
        isExpanded: Boolean
    ) = runDesktopComposeUiTest(width = CANVAS_WIDTH_PX, height = CANVAS_HEIGHT_PX) {
        setContent {
            CompositionLocalProvider(LocalDensity provides Density(density = SCREENSHOT_DENSITY)) {
                AppTheme(darkTheme = false) {
                    NavigationMenu(
                        isExpanded = isExpanded,
                        menuMode = menuMode,
                        menuAlignment = menuAlignment,
                        expandedLeadingSlot = { Text(text = LEADING_SLOT_TEXT) },
                        itemList = MenuDestination.entries,
                        isItemSelected = { destination -> destination == SELECTED_DESTINATION },
                        itemText = { destination -> destination.title },
                        itemTextStyle = { TextStyle(color = Color.White) },
                        itemIcon = { destination -> destination.icon },
                        itemIconTint = { Color.White },
                        itemIconDescription = { destination -> destination.title },
                        onItemClick = {}
                    )
                }
            }
        }

        onRoot().captureRoboImage(filePath = screenshotPath(name = name))
    }

    private enum class MenuDestination(val title: String, val icon: DrawableResource) {
        Search(title = "Search", icon = Res.drawable.shared_ic_find),
        Calendar(title = "Calendar", icon = Res.drawable.shared_ic_calendar),
        Profile(title = "Profile", icon = Res.drawable.shared_ic_person)
    }

    private companion object {
        const val CANVAS_WIDTH_PX = 1200
        const val CANVAS_HEIGHT_PX = 800
        const val LEADING_SLOT_TEXT = "Menu"
        val SELECTED_DESTINATION = MenuDestination.Calendar
    }
}
