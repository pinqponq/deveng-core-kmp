package core.presentation.component.navigationmenu

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.unit.Density
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class NavigationBarPlacementWindowTest {

    @Test
    fun currentNavigationBarPlacement_withPhonePortraitWindow_returnsBottom() = assertWindowPlacement(
        widthPx = PHONE_WIDTH_PX,
        heightPx = PHONE_HEIGHT_PX,
        sideBarAlignment = MenuAlignment.End,
        expected = NavigationBarPlacement.Bottom
    )

    @Test
    fun currentNavigationBarPlacement_withWideWindow_returnsSideBarAtRequestedSide() = assertWindowPlacement(
        widthPx = WIDE_WIDTH_PX,
        heightPx = PHONE_HEIGHT_PX,
        sideBarAlignment = MenuAlignment.End,
        expected = NavigationBarPlacement.End
    )

    private fun assertWindowPlacement(
        widthPx: Int,
        heightPx: Int,
        sideBarAlignment: MenuAlignment,
        expected: NavigationBarPlacement
    ) = runDesktopComposeUiTest(width = widthPx, height = heightPx) {
        var placement: NavigationBarPlacement? = null
        setContent {
            CompositionLocalProvider(LocalDensity provides Density(density = 1f)) {
                placement = currentNavigationBarPlacement(sideBarAlignment = sideBarAlignment)
            }
        }

        waitForIdle()

        assertEquals(expected, placement)
    }

    private companion object {
        const val PHONE_WIDTH_PX = 400
        const val PHONE_HEIGHT_PX = 900
        const val WIDE_WIDTH_PX = 1200
    }
}
