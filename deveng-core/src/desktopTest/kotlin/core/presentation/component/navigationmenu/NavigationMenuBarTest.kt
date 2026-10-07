package core.presentation.component.navigationmenu

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class NavigationMenuBarTest {

    @Test
    fun bottomBar_withEachSelectedItem_drawsIndicatorOnThatItem() = runComposeUiTest {
        val robot = NavigationMenuBarRobot(composeUiTest = this)
            .setContent(placement = NavigationBarPlacement.Bottom)

        BarDestination.entries.reversed().forEach { destination ->
            robot
                .selectDestination(destination = destination)
                .assertIndicatorOn(destination = destination)
        }
    }

    @Test
    fun sideBarAtStart_withEachSelectedItem_drawsIndicatorOnThatItem() = runComposeUiTest {
        val robot = NavigationMenuBarRobot(composeUiTest = this)
            .setContent(placement = NavigationBarPlacement.Start)

        BarDestination.entries.reversed().forEach { destination ->
            robot
                .selectDestination(destination = destination)
                .assertIndicatorOn(destination = destination)
        }
    }

    @Test
    fun sideBarAtEnd_withEachSelectedItem_drawsIndicatorOnThatItem() = runComposeUiTest {
        val robot = NavigationMenuBarRobot(composeUiTest = this)
            .setContent(placement = NavigationBarPlacement.End)

        BarDestination.entries.reversed().forEach { destination ->
            robot
                .selectDestination(destination = destination)
                .assertIndicatorOn(destination = destination)
        }
    }

    @Test
    fun clickItem_inBottomBar_sendsItemAndMovesIndicator() = runComposeUiTest {
        val robot = NavigationMenuBarRobot(composeUiTest = this)
            .setContent(placement = NavigationBarPlacement.Bottom)
            .clickItem(destination = BarDestination.Profile)
            .assertIndicatorOn(destination = BarDestination.Profile)

        assertEquals(listOf(BarDestination.Profile), robot.clickedDestinationList)
    }

    @Test
    fun clickItem_inSideBar_sendsItemAndMovesIndicator() = runComposeUiTest {
        val robot = NavigationMenuBarRobot(composeUiTest = this)
            .setContent(placement = NavigationBarPlacement.Start)
            .clickItem(destination = BarDestination.Calendar)
            .assertIndicatorOn(destination = BarDestination.Calendar)

        assertEquals(listOf(BarDestination.Calendar), robot.clickedDestinationList)
    }

    @Test
    fun clearSelection_afterSelectingAnItem_keepsIndicatorOnLastSelectedItem() = runComposeUiTest {
        NavigationMenuBarRobot(composeUiTest = this)
            .setContent(placement = NavigationBarPlacement.Bottom)
            .clickItem(destination = BarDestination.Calendar)
            .selectDestination(destination = null)
            .assertIndicatorOn(destination = BarDestination.Calendar)
    }

    @Test
    fun setContent_withoutSelectedItem_drawsIndicatorOnFirstItem() = runComposeUiTest {
        NavigationMenuBarRobot(composeUiTest = this)
            .setContent(placement = NavigationBarPlacement.Bottom, initiallySelectedDestination = null)
            .assertIndicatorOn(destination = BarDestination.Search)
    }

    @Test
    fun indicatorSize_whenSet_drawsFixedSizeIndicatorCenteredInItem() = runComposeUiTest {
        NavigationMenuBarRobot(composeUiTest = this)
            .setContent(
                placement = NavigationBarPlacement.Bottom,
                initiallySelectedDestination = BarDestination.Calendar,
                indicatorSize = FIXED_INDICATOR_SIZE
            )
            .assertIndicatorOn(
                destination = BarDestination.Calendar,
                mainAxisOffset = FIXED_INDICATOR_PROBE_MAIN_AXIS_OFFSET,
                crossAxisOffset = NavigationMenuBarRobot.BAR_THICKNESS / 2
            )
            .assertNoIndicator()
    }

    @Test
    fun barItemContent_withCustomItem_rendersItWithItsSelectionState() = runComposeUiTest {
        NavigationMenuBarRobot(composeUiTest = this)
            .setContent(
                placement = NavigationBarPlacement.Bottom,
                customContentDestination = BarDestination.Calendar
            )
            .assertTextDisplayed(text = NavigationMenuBarRobot.CUSTOM_CONTENT_TEXT)
            .selectDestination(destination = BarDestination.Calendar)
            .assertTextDisplayed(text = NavigationMenuBarRobot.CUSTOM_CONTENT_SELECTED_TEXT)
    }

    @Test
    fun clickItem_withCustomItemContent_sendsCustomAndDefaultItems() = runComposeUiTest {
        val robot = NavigationMenuBarRobot(composeUiTest = this)
            .setContent(
                placement = NavigationBarPlacement.Bottom,
                customContentDestination = BarDestination.Calendar
            )
            .clickText(text = NavigationMenuBarRobot.CUSTOM_CONTENT_TEXT)
            .clickItem(destination = BarDestination.Profile)

        assertEquals(listOf(BarDestination.Calendar, BarDestination.Profile), robot.clickedDestinationList)
    }

    @Test
    fun setContent_withoutIndicator_drawsNoIndicator() = runComposeUiTest {
        NavigationMenuBarRobot(composeUiTest = this)
            .setContent(placement = NavigationBarPlacement.Bottom, isIndicatorVisible = false)
            .assertNoIndicator()
    }

    @Test
    fun indicatorInset_withSelectedItem_leavesItsEdgeClear() = runComposeUiTest {
        NavigationMenuBarRobot(composeUiTest = this)
            .setContent(placement = NavigationBarPlacement.Bottom, initiallySelectedDestination = BarDestination.Profile)
            .assertIndicatorOn(destination = BarDestination.Profile)
            .assertInsetLeftClearAround(destination = BarDestination.Profile)
    }

    @Test
    fun bottomBar_withThreeItems_placesThemInEqualCellsAlongTheRow() = runComposeUiTest {
        NavigationMenuBarRobot(composeUiTest = this)
            .setContent(placement = NavigationBarPlacement.Bottom)
            .assertItemCellsFollowBarAxis()
    }

    @Test
    fun sideBar_withThreeItems_placesThemInEqualCellsAlongTheColumn() = runComposeUiTest {
        NavigationMenuBarRobot(composeUiTest = this)
            .setContent(placement = NavigationBarPlacement.End)
            .assertItemCellsFollowBarAxis()
    }

    private companion object {
        val FIXED_INDICATOR_SIZE = DpSize(width = 40.dp, height = 10.dp)
        val FIXED_INDICATOR_PROBE_MAIN_AXIS_OFFSET = 32.dp
    }

    @Test
    fun clickItem_byDefault_showsRipple() = runComposeUiTest {
        NavigationMenuBarRobot(composeUiTest = this)
            .setContent(placement = NavigationBarPlacement.Bottom)
            .clickItem(destination = BarDestination.Profile)
            .assertItemRippleCount(expectedCount = 1)
    }

    @Test
    fun clickItem_withRippleDisabled_showsNoRipple() = runComposeUiTest {
        NavigationMenuBarRobot(composeUiTest = this)
            .setContent(placement = NavigationBarPlacement.Bottom, isItemRippleEnabled = false)
            .clickItem(destination = BarDestination.Profile)
            .assertItemRippleCount(expectedCount = 0)
            .assertIndicatorOn(destination = BarDestination.Profile)
    }
}
