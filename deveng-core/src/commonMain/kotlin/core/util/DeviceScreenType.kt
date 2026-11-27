package core.util

import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.window.core.layout.WindowSizeClass
import androidx.window.core.layout.WindowSizeClass.Companion.HEIGHT_DP_EXPANDED_LOWER_BOUND
import androidx.window.core.layout.WindowSizeClass.Companion.HEIGHT_DP_MEDIUM_LOWER_BOUND
import androidx.window.core.layout.WindowSizeClass.Companion.WIDTH_DP_EXPANDED_LOWER_BOUND
import androidx.window.core.layout.WindowSizeClass.Companion.WIDTH_DP_MEDIUM_LOWER_BOUND

enum class DeviceScreenType {
    MOBILE_PORTRAIT,
    MOBILE_LANDSCAPE,
    TABLET_PORTRAIT,
    TABLET_LANDSCAPE,
    DESKTOP;

    companion object {
        fun fromWindowSizeClass(windowSizeClass: WindowSizeClass): DeviceScreenType {
            return with(windowSizeClass) {
                when {
                    minWidthDp < WIDTH_DP_MEDIUM_LOWER_BOUND &&
                            minHeightDp >= HEIGHT_DP_MEDIUM_LOWER_BOUND -> MOBILE_PORTRAIT

                    minWidthDp >= HEIGHT_DP_MEDIUM_LOWER_BOUND && minWidthDp < WIDTH_DP_MEDIUM_LOWER_BOUND &&
                            minHeightDp < HEIGHT_DP_MEDIUM_LOWER_BOUND -> MOBILE_LANDSCAPE

                    minWidthDp >= WIDTH_DP_MEDIUM_LOWER_BOUND && minWidthDp < WIDTH_DP_EXPANDED_LOWER_BOUND &&
                            minHeightDp >= HEIGHT_DP_EXPANDED_LOWER_BOUND -> TABLET_PORTRAIT

                    minWidthDp >= WIDTH_DP_MEDIUM_LOWER_BOUND && minWidthDp < WIDTH_DP_EXPANDED_LOWER_BOUND &&
                            minHeightDp >= HEIGHT_DP_MEDIUM_LOWER_BOUND && minHeightDp < HEIGHT_DP_EXPANDED_LOWER_BOUND -> TABLET_LANDSCAPE

                    minWidthDp >= WIDTH_DP_EXPANDED_LOWER_BOUND &&
                            minHeightDp >= HEIGHT_DP_EXPANDED_LOWER_BOUND -> DESKTOP

                    else -> DESKTOP
                }
            }
        }
    }
}

@Composable
fun currentDeviceScreenType(): DeviceScreenType {
    val windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass
    return DeviceScreenType.fromWindowSizeClass(windowSizeClass)
}
