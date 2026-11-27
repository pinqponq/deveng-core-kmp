package core.presentation.component.navigationmenu

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import core.util.DeviceScreenType
import core.util.currentDeviceScreenType

/**
 * Where a compact navigation bar sits in the window, with the [MenuMode] and [MenuAlignment] that render it.
 */
enum class NavigationBarPlacement(
    val menuMode: MenuMode,
    val menuAlignment: MenuAlignment
) {
    Bottom(menuMode = MenuMode.BottomBar, menuAlignment = MenuAlignment.Start),
    Start(menuMode = MenuMode.SideBar, menuAlignment = MenuAlignment.Start),
    End(menuMode = MenuMode.SideBar, menuAlignment = MenuAlignment.End);

    /**
     * Padding that keeps screen content clear of a bar of [barThickness] in this placement.
     */
    fun contentPadding(barThickness: Dp): PaddingValues = when (this) {
        Bottom -> PaddingValues(bottom = barThickness)
        Start -> PaddingValues(start = barThickness)
        End -> PaddingValues(end = barThickness)
    }

    companion object {
        /**
         * The bar stays at the bottom only on a phone in portrait; every wider or shorter window gets a side bar.
         *
         * @param deviceScreenType Window size category of the current window.
         * @param sideBarAlignment Side the bar moves to when it does not sit at the bottom.
         */
        fun fromDeviceScreenType(
            deviceScreenType: DeviceScreenType,
            sideBarAlignment: MenuAlignment = MenuAlignment.Start
        ): NavigationBarPlacement {
            if (deviceScreenType == DeviceScreenType.MOBILE_PORTRAIT) return Bottom

            return when (sideBarAlignment) {
                MenuAlignment.Start -> Start
                MenuAlignment.End -> End
            }
        }
    }
}

/**
 * Bar placement for the current window size. See [NavigationBarPlacement.fromDeviceScreenType].
 *
 * @param sideBarAlignment Side the bar moves to when it does not sit at the bottom.
 */
@Composable
fun currentNavigationBarPlacement(
    sideBarAlignment: MenuAlignment = MenuAlignment.Start
): NavigationBarPlacement = NavigationBarPlacement.fromDeviceScreenType(
    deviceScreenType = currentDeviceScreenType(),
    sideBarAlignment = sideBarAlignment
)
