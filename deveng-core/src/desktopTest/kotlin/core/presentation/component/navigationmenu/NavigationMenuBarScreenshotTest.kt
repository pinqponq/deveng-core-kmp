package core.presentation.component.navigationmenu

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import core.presentation.component.navigationmenu.bar.NavigationMenuBarItemIcon
import core.presentation.theme.AppTheme
import core.testing.SCREENSHOT_DENSITY
import core.testing.screenshotPath
import io.github.takahirom.roborazzi.captureRoboImage
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class NavigationMenuBarScreenshotTest {

    @Test
    fun bottomBar_withIndicatorAndCustomCenterItem_matchesBaseline() = captureBar(
        name = "navigation_menu_bottom_bar",
        placement = NavigationBarPlacement.Bottom,
        canvasSize = BOTTOM_BAR_CANVAS_SIZE,
        barShape = RoundedCornerShape(topStart = CORNER_RADIUS, topEnd = CORNER_RADIUS)
    )

    @Test
    fun sideBarAtStart_withIndicatorAndCustomCenterItem_matchesBaseline() = captureBar(
        name = "navigation_menu_side_bar_start",
        placement = NavigationBarPlacement.Start,
        canvasSize = SIDE_BAR_CANVAS_SIZE,
        barShape = RoundedCornerShape(topEnd = CORNER_RADIUS, bottomEnd = CORNER_RADIUS)
    )

    @Test
    fun sideBarAtEnd_withIndicatorAndCustomCenterItem_matchesBaseline() = captureBar(
        name = "navigation_menu_side_bar_end",
        placement = NavigationBarPlacement.End,
        canvasSize = SIDE_BAR_CANVAS_SIZE,
        barShape = RoundedCornerShape(topStart = CORNER_RADIUS, bottomStart = CORNER_RADIUS)
    )

    private fun captureBar(
        name: String,
        placement: NavigationBarPlacement,
        canvasSize: DpSize,
        barShape: RoundedCornerShape
    ) = runDesktopComposeUiTest(width = CANVAS_MAX_PX, height = CANVAS_MAX_PX) {
        setContent {
            CompositionLocalProvider(LocalDensity provides Density(density = SCREENSHOT_DENSITY)) {
                AppTheme(darkTheme = false) {
                    Box(modifier = Modifier.size(canvasSize)) {
                        NavigationMenu(
                            isExpanded = false,
                            menuMode = placement.menuMode,
                            menuAlignment = placement.menuAlignment,
                            modifier = Modifier.align(placement.alignment()),
                            backgroundColor = BAR_COLOR,
                            shape = barShape,
                            barThickness = BAR_THICKNESS,
                            barIndicator = NavigationMenuIndicator(color = INDICATOR_COLOR),
                            barItemContent = { destination, isSelected ->
                                if (destination == BarDestination.Calendar) {
                                    Text(text = CENTER_ITEM_TEXT, color = CENTER_ITEM_COLOR)
                                } else {
                                    NavigationMenuBarItemIcon(
                                        icon = destination.icon,
                                        iconTint = if (isSelected) SELECTED_ICON_COLOR else UNSELECTED_ICON_COLOR,
                                        contentDescription = destination.title
                                    )
                                }
                            },
                            itemList = BarDestination.entries,
                            isItemSelected = { destination -> destination == SELECTED_DESTINATION },
                            itemText = { destination -> destination.title },
                            itemTextStyle = { TextStyle() },
                            itemIcon = { destination -> destination.icon },
                            itemIconTint = { UNSELECTED_ICON_COLOR },
                            itemIconDescription = { destination -> destination.title },
                            onItemClick = {}
                        )
                    }
                }
            }
        }

        onRoot().captureRoboImage(filePath = screenshotPath(name = name))
    }

    private fun NavigationBarPlacement.alignment(): Alignment = when (this) {
        NavigationBarPlacement.Bottom -> Alignment.BottomCenter
        NavigationBarPlacement.Start -> Alignment.CenterStart
        NavigationBarPlacement.End -> Alignment.CenterEnd
    }

    private companion object {
        const val CANVAS_MAX_PX = 800
        const val CENTER_ITEM_TEXT = "R"
        val BOTTOM_BAR_CANVAS_SIZE = DpSize(width = 360.dp, height = 120.dp)
        val SIDE_BAR_CANVAS_SIZE = DpSize(width = 160.dp, height = 360.dp)
        val BAR_THICKNESS = 54.dp
        val CORNER_RADIUS = 28.dp
        val BAR_COLOR = Color(0xFF0E0E0E)
        val INDICATOR_COLOR = Color(0xFF262626)
        val SELECTED_ICON_COLOR = Color(0xFFFFC964)
        val UNSELECTED_ICON_COLOR = Color(0xFFE7E7E7)
        val CENTER_ITEM_COLOR = Color(0xFFFFC964)
        val SELECTED_DESTINATION = BarDestination.Profile
    }
}
