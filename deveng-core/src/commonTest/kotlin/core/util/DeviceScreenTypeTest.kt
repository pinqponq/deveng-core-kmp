package core.util

import androidx.window.core.layout.WindowSizeClass
import kotlin.test.Test
import kotlin.test.assertEquals

class DeviceScreenTypeTest {

    @Test
    fun fromWindowSizeClass_withCompactWidthAndMediumHeight_returnsMobilePortrait() {
        assertDeviceScreenType(widthDp = 0, heightDp = 480, expected = DeviceScreenType.MOBILE_PORTRAIT)
    }

    @Test
    fun fromWindowSizeClass_withCompactWidthAndCompactHeight_returnsMobileLandscape() {
        assertDeviceScreenType(widthDp = 480, heightDp = 0, expected = DeviceScreenType.MOBILE_LANDSCAPE)
    }

    @Test
    fun fromWindowSizeClass_withMediumWidthAndExpandedHeight_returnsTabletPortrait() {
        assertDeviceScreenType(widthDp = 600, heightDp = 900, expected = DeviceScreenType.TABLET_PORTRAIT)
    }

    @Test
    fun fromWindowSizeClass_withMediumWidthAndMediumHeight_returnsTabletLandscape() {
        assertDeviceScreenType(widthDp = 600, heightDp = 480, expected = DeviceScreenType.TABLET_LANDSCAPE)
    }

    @Test
    fun fromWindowSizeClass_withExpandedWidthAndExpandedHeight_returnsDesktop() {
        assertDeviceScreenType(widthDp = 840, heightDp = 900, expected = DeviceScreenType.DESKTOP)
    }

    @Test
    fun fromWindowSizeClass_withMediumWidthAndCompactHeight_fallsBackToDesktop() {
        assertDeviceScreenType(widthDp = 600, heightDp = 0, expected = DeviceScreenType.DESKTOP)
    }

    @Test
    fun fromWindowSizeClass_withCompactWidthBelowLandscapeBoundAndCompactHeight_fallsBackToDesktop() {
        assertDeviceScreenType(widthDp = 0, heightDp = 0, expected = DeviceScreenType.DESKTOP)
    }

    private fun assertDeviceScreenType(widthDp: Int, heightDp: Int, expected: DeviceScreenType) {
        val windowSizeClass = WindowSizeClass(minWidthDp = widthDp, minHeightDp = heightDp)

        assertEquals(expected, DeviceScreenType.fromWindowSizeClass(windowSizeClass))
    }
}
