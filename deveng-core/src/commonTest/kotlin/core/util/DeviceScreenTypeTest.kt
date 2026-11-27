package core.util

import androidx.window.core.layout.WindowSizeClass
import kotlin.test.Test
import kotlin.test.assertEquals

class DeviceScreenTypeTest {

    @Test
    fun fromWindowSizeClass_withCompactWidthAndMediumHeight_returnsMobilePortrait() {
        assertDeviceScreenType(widthDp = ZERO_DP, heightDp = MEDIUM_HEIGHT_DP, expected = DeviceScreenType.MOBILE_PORTRAIT)
        assertDeviceScreenType(widthDp = LAST_COMPACT_WIDTH_DP, heightDp = MEDIUM_HEIGHT_DP, expected = DeviceScreenType.MOBILE_PORTRAIT)
    }

    @Test
    fun fromWindowSizeClass_withLandscapePhoneWidthAndCompactHeight_returnsMobileLandscape() {
        assertDeviceScreenType(widthDp = MEDIUM_HEIGHT_DP, heightDp = ZERO_DP, expected = DeviceScreenType.MOBILE_LANDSCAPE)
        assertDeviceScreenType(widthDp = LAST_COMPACT_WIDTH_DP, heightDp = LAST_COMPACT_HEIGHT_DP, expected = DeviceScreenType.MOBILE_LANDSCAPE)
    }

    @Test
    fun fromWindowSizeClass_withMediumWidthAndExpandedHeight_returnsTabletPortrait() {
        assertDeviceScreenType(widthDp = MEDIUM_WIDTH_DP, heightDp = EXPANDED_HEIGHT_DP, expected = DeviceScreenType.TABLET_PORTRAIT)
        assertDeviceScreenType(widthDp = LAST_MEDIUM_WIDTH_DP, heightDp = EXPANDED_HEIGHT_DP, expected = DeviceScreenType.TABLET_PORTRAIT)
    }

    @Test
    fun fromWindowSizeClass_withMediumWidthAndMediumHeight_returnsTabletLandscape() {
        assertDeviceScreenType(widthDp = MEDIUM_WIDTH_DP, heightDp = MEDIUM_HEIGHT_DP, expected = DeviceScreenType.TABLET_LANDSCAPE)
        assertDeviceScreenType(widthDp = LAST_MEDIUM_WIDTH_DP, heightDp = LAST_MEDIUM_HEIGHT_DP, expected = DeviceScreenType.TABLET_LANDSCAPE)
    }

    @Test
    fun fromWindowSizeClass_withExpandedWidthAndExpandedHeight_returnsDesktop() {
        assertDeviceScreenType(widthDp = EXPANDED_WIDTH_DP, heightDp = EXPANDED_HEIGHT_DP, expected = DeviceScreenType.DESKTOP)
    }

    @Test
    fun fromWindowSizeClass_withSizeNoCategoryMatches_fallsBackToDesktop() {
        assertDeviceScreenType(widthDp = MEDIUM_WIDTH_DP, heightDp = ZERO_DP, expected = DeviceScreenType.DESKTOP)
        assertDeviceScreenType(widthDp = LAST_LANDSCAPE_GAP_WIDTH_DP, heightDp = ZERO_DP, expected = DeviceScreenType.DESKTOP)
        assertDeviceScreenType(widthDp = EXPANDED_WIDTH_DP, heightDp = LAST_MEDIUM_HEIGHT_DP, expected = DeviceScreenType.DESKTOP)
        assertDeviceScreenType(widthDp = MEDIUM_WIDTH_DP, heightDp = LAST_COMPACT_HEIGHT_DP, expected = DeviceScreenType.DESKTOP)
    }

    private fun assertDeviceScreenType(widthDp: Int, heightDp: Int, expected: DeviceScreenType) {
        val windowSizeClass = WindowSizeClass(minWidthDp = widthDp, minHeightDp = heightDp)

        assertEquals(expected, DeviceScreenType.fromWindowSizeClass(windowSizeClass), "${widthDp}x$heightDp")
    }

    private companion object {
        const val ZERO_DP = 0
        const val MEDIUM_HEIGHT_DP = 480
        const val LAST_COMPACT_HEIGHT_DP = MEDIUM_HEIGHT_DP - 1
        const val EXPANDED_HEIGHT_DP = 900
        const val LAST_MEDIUM_HEIGHT_DP = EXPANDED_HEIGHT_DP - 1
        const val MEDIUM_WIDTH_DP = 600
        const val LAST_COMPACT_WIDTH_DP = MEDIUM_WIDTH_DP - 1
        const val LAST_LANDSCAPE_GAP_WIDTH_DP = MEDIUM_HEIGHT_DP - 1
        const val EXPANDED_WIDTH_DP = 840
        const val LAST_MEDIUM_WIDTH_DP = EXPANDED_WIDTH_DP - 1
    }
}
