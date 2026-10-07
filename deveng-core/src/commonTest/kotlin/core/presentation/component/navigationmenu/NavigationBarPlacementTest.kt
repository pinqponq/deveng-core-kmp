package core.presentation.component.navigationmenu

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import core.util.DeviceScreenType
import kotlin.test.Test
import kotlin.test.assertEquals

class NavigationBarPlacementTest {

    @Test
    fun fromDeviceScreenType_withMobilePortrait_returnsBottom() {
        val placement = NavigationBarPlacement.fromDeviceScreenType(
            deviceScreenType = DeviceScreenType.MOBILE_PORTRAIT,
            sideBarAlignment = MenuAlignment.End
        )

        assertEquals(NavigationBarPlacement.Bottom, placement)
    }

    @Test
    fun fromDeviceScreenType_withEveryWiderOrShorterWindow_returnsStartByDefault() {
        SIDE_BAR_SCREEN_TYPES.forEach { deviceScreenType ->
            assertEquals(
                NavigationBarPlacement.Start,
                NavigationBarPlacement.fromDeviceScreenType(deviceScreenType = deviceScreenType)
            )
        }
    }

    @Test
    fun fromDeviceScreenType_withEndAlignment_returnsEnd() {
        SIDE_BAR_SCREEN_TYPES.forEach { deviceScreenType ->
            assertEquals(
                NavigationBarPlacement.End,
                NavigationBarPlacement.fromDeviceScreenType(
                    deviceScreenType = deviceScreenType,
                    sideBarAlignment = MenuAlignment.End
                )
            )
        }
    }

    @Test
    fun placement_mapsToBarMenuModeAndAlignment() {
        assertEquals(MenuMode.BottomBar, NavigationBarPlacement.Bottom.menuMode)
        assertEquals(MenuMode.SideBar, NavigationBarPlacement.Start.menuMode)
        assertEquals(MenuAlignment.Start, NavigationBarPlacement.Start.menuAlignment)
        assertEquals(MenuMode.SideBar, NavigationBarPlacement.End.menuMode)
        assertEquals(MenuAlignment.End, NavigationBarPlacement.End.menuAlignment)
    }

    @Test
    fun contentPadding_withBottom_padsOnlyTheBottom() {
        assertPadding(
            expected = PaddingValues(bottom = BAR_THICKNESS),
            actual = NavigationBarPlacement.Bottom.contentPadding(barThickness = BAR_THICKNESS)
        )
    }

    @Test
    fun contentPadding_withStart_padsOnlyTheStart() {
        assertPadding(
            expected = PaddingValues(start = BAR_THICKNESS),
            actual = NavigationBarPlacement.Start.contentPadding(barThickness = BAR_THICKNESS)
        )
    }

    @Test
    fun contentPadding_withEnd_padsOnlyTheEnd() {
        assertPadding(
            expected = PaddingValues(end = BAR_THICKNESS),
            actual = NavigationBarPlacement.End.contentPadding(barThickness = BAR_THICKNESS)
        )
    }

    private fun assertPadding(expected: PaddingValues, actual: PaddingValues) {
        assertEquals(expected.calculateTopPadding(), actual.calculateTopPadding())
        assertEquals(expected.calculateBottomPadding(), actual.calculateBottomPadding())
        assertEquals(
            expected.calculateLeftPadding(LayoutDirection.Ltr),
            actual.calculateLeftPadding(LayoutDirection.Ltr)
        )
        assertEquals(
            expected.calculateRightPadding(LayoutDirection.Ltr),
            actual.calculateRightPadding(LayoutDirection.Ltr)
        )
    }

    private companion object {
        val BAR_THICKNESS = 72.dp
        val SIDE_BAR_SCREEN_TYPES = listOf(
            DeviceScreenType.MOBILE_LANDSCAPE,
            DeviceScreenType.TABLET_PORTRAIT,
            DeviceScreenType.TABLET_LANDSCAPE,
            DeviceScreenType.DESKTOP
        )
    }
}
