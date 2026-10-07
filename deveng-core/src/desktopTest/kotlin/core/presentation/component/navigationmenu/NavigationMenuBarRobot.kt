package core.presentation.component.navigationmenu

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import core.presentation.component.navigationmenu.bar.NavigationMenuBarItemIcon
import core.presentation.theme.AppTheme
import global.deveng.deveng_core.generated.resources.Res
import global.deveng.deveng_core.generated.resources.shared_ic_calendar
import global.deveng.deveng_core.generated.resources.shared_ic_find
import global.deveng.deveng_core.generated.resources.shared_ic_person
import kotlin.test.assertEquals
import org.jetbrains.compose.resources.DrawableResource

enum class BarDestination(val title: String, val icon: DrawableResource) {
    Search(title = "Search", icon = Res.drawable.shared_ic_find),
    Calendar(title = "Calendar", icon = Res.drawable.shared_ic_calendar),
    Profile(title = "Profile", icon = Res.drawable.shared_ic_person)
}

@OptIn(ExperimentalTestApi::class)
class NavigationMenuBarRobot(private val composeUiTest: ComposeUiTest) {
    val clickedDestinationList = mutableListOf<BarDestination>()
    private var selectedDestination by mutableStateOf<BarDestination?>(null)
    private var isVertical = false

    fun setContent(
        placement: NavigationBarPlacement,
        initiallySelectedDestination: BarDestination? = BarDestination.Search,
        indicatorSize: DpSize? = null,
        isIndicatorVisible: Boolean = true,
        customContentDestination: BarDestination? = null
    ) = apply {
        selectedDestination = initiallySelectedDestination
        isVertical = placement.menuMode == MenuMode.SideBar
        val containerSize = if (isVertical) DpSize(BAR_THICKNESS, BAR_LENGTH) else DpSize(BAR_LENGTH, BAR_THICKNESS)
        composeUiTest.setContent {
            CompositionLocalProvider(LocalDensity provides Density(density = 1f)) {
                AppTheme(darkTheme = false) {
                    Box(modifier = Modifier.size(containerSize).testTag(CONTAINER_TAG)) {
                        NavigationMenu(
                            isExpanded = false,
                            menuMode = placement.menuMode,
                            menuAlignment = placement.menuAlignment,
                            backgroundColor = BAR_COLOR,
                            barThickness = BAR_THICKNESS,
                            barIndicator = if (isIndicatorVisible) {
                                NavigationMenuIndicator(
                                    color = INDICATOR_COLOR,
                                    shape = RectangleShape,
                                    inset = INDICATOR_INSET,
                                    size = indicatorSize
                                )
                            } else {
                                null
                            },
                            barItemContent = if (customContentDestination == null) {
                                null
                            } else {
                                { destination, isSelected ->
                                    if (destination == customContentDestination) {
                                        Text(text = customContentText(isSelected = isSelected))
                                    } else {
                                        NavigationMenuBarItemIcon(
                                            icon = destination.icon,
                                            iconTint = Color.White,
                                            contentDescription = destination.title
                                        )
                                    }
                                }
                            },
                            itemList = BarDestination.entries,
                            isItemSelected = { destination -> destination == selectedDestination },
                            itemText = { destination -> destination.title },
                            itemTextStyle = { TextStyle() },
                            itemIcon = { destination -> destination.icon },
                            itemIconTint = { Color.White },
                            itemIconDescription = { destination -> destination.title },
                            onItemClick = { destination ->
                                clickedDestinationList += destination
                                selectedDestination = destination
                            }
                        )
                    }
                }
            }
        }
    }

    fun clickItem(destination: BarDestination) = apply {
        composeUiTest.onNodeWithContentDescription(destination.title).performClick()
        composeUiTest.waitForIdle()
    }

    fun clickText(text: String) = apply {
        composeUiTest.onNodeWithText(text).performClick()
        composeUiTest.waitForIdle()
    }

    fun selectDestination(destination: BarDestination?) = apply {
        selectedDestination = destination
        composeUiTest.waitForIdle()
    }

    fun assertTextDisplayed(text: String) = apply {
        composeUiTest.onNodeWithText(text).assertIsDisplayed()
    }

    fun assertItemCellsFollowBarAxis() = apply {
        BarDestination.entries.forEach { destination ->
            val cellBounds = composeUiTest.onNodeWithContentDescription(destination.title).getBoundsInRoot()
            val cellStart = if (isVertical) cellBounds.top else cellBounds.left
            val cellLength = if (isVertical) cellBounds.bottom - cellBounds.top else cellBounds.right - cellBounds.left
            assertEquals(CELL_LENGTH * destination.ordinal, cellStart, "cell start of ${destination.title}")
            assertEquals(CELL_LENGTH, cellLength, "cell length of ${destination.title}")
        }
    }

    fun assertNoIndicator() = apply {
        composeUiTest.waitForIdle()
        BarDestination.entries.forEach { cellDestination ->
            val probeColor = probeColor(cellIndex = cellDestination.ordinal, mainAxisOffset = PROBE_OFFSET, crossAxisOffset = PROBE_OFFSET)
            assertEquals(false, isIndicatorColor(probeColor), "indicator on ${cellDestination.title}: $probeColor")
        }
    }

    fun assertInsetLeftClearAround(destination: BarDestination) = apply {
        composeUiTest.waitForIdle()
        val insetProbeColor = probeColor(
            cellIndex = destination.ordinal,
            mainAxisOffset = INDICATOR_INSET / 2,
            crossAxisOffset = INDICATOR_INSET / 2
        )
        assertEquals(false, isIndicatorColor(insetProbeColor), "inset of ${destination.title}: $insetProbeColor")
    }

    fun assertIndicatorOn(destination: BarDestination, crossAxisOffset: Dp = PROBE_OFFSET, mainAxisOffset: Dp = PROBE_OFFSET) = apply {
        composeUiTest.waitForIdle()
        BarDestination.entries.forEach { cellDestination ->
            val probeColor = probeColor(cellIndex = cellDestination.ordinal, mainAxisOffset = mainAxisOffset, crossAxisOffset = crossAxisOffset)
            assertEquals(
                cellDestination == destination,
                isIndicatorColor(probeColor),
                "indicator on ${cellDestination.title}: $probeColor"
            )
        }
    }

    // A hovered or pressed item draws a translucent state layer over the indicator, so the probe
    // accepts the indicator color with that tint instead of the exact color.
    private fun isIndicatorColor(color: Color): Boolean =
        color.red > INDICATOR_CHANNEL_MIN && color.blue > INDICATOR_CHANNEL_MIN && color.green < BAR_CHANNEL_MAX

    private fun probeColor(cellIndex: Int, mainAxisOffset: Dp, crossAxisOffset: Dp): Color {
        val pixelMap = composeUiTest.onNodeWithTag(CONTAINER_TAG).captureToImage().toPixelMap()
        val cellStart = (BAR_LENGTH.value / BarDestination.entries.size * cellIndex).toInt()
        val mainAxisPixel = cellStart + mainAxisOffset.value.toInt()
        val crossAxisPixel = crossAxisOffset.value.toInt()
        return if (isVertical) pixelMap[crossAxisPixel, mainAxisPixel] else pixelMap[mainAxisPixel, crossAxisPixel]
    }

    companion object {
        val BAR_LENGTH = 300.dp
        val CELL_LENGTH = BAR_LENGTH / BarDestination.entries.size
        val BAR_THICKNESS = 72.dp
        val INDICATOR_INSET = 6.dp
        val PROBE_OFFSET = 8.dp
        val BAR_COLOR = Color.Black
        val INDICATOR_COLOR = Color.Magenta
        const val CUSTOM_CONTENT_TEXT = "Center"
        const val CUSTOM_CONTENT_SELECTED_TEXT = "Center selected"
        private const val INDICATOR_CHANNEL_MIN = 0.8f
        private const val BAR_CHANNEL_MAX = 0.2f
        private const val CONTAINER_TAG = "navigationMenuBarTestContainer"

        fun customContentText(isSelected: Boolean): String =
            if (isSelected) CUSTOM_CONTENT_SELECTED_TEXT else CUSTOM_CONTENT_TEXT
    }
}
