package core.presentation.component.shimmer

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import core.presentation.motion.LocalReduceMotion
import core.presentation.motion.MotionTokens
import core.presentation.theme.AppTheme
import core.testing.SCREENSHOT_DENSITY
import core.testing.screenshotPath
import io.github.takahirom.roborazzi.captureRoboImage
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class ShimmerBoxScreenshotTest {

    @Test
    fun shimmerBox_halfwayThroughSweep_matchesBaseline() = captureShimmerBox(
        name = "shimmer_box_halfway",
        isReduceMotionEnabled = false
    )

    @Test
    fun shimmerBox_withReduceMotion_showsStaticPlaceholder() = captureShimmerBox(
        name = "shimmer_box_reduce_motion",
        isReduceMotionEnabled = true
    )

    private fun captureShimmerBox(name: String, isReduceMotionEnabled: Boolean) = runDesktopComposeUiTest(
        width = CANVAS_WIDTH_PX,
        height = CANVAS_HEIGHT_PX
    ) {
        mainClock.autoAdvance = false
        setContent {
            CompositionLocalProvider(LocalDensity provides Density(density = SCREENSHOT_DENSITY)) {
                AppTheme(darkTheme = false, reduceMotion = isReduceMotionEnabled) {
                    ShimmerBox(modifier = Modifier.size(width = BOX_WIDTH_DP.dp, height = BOX_HEIGHT_DP.dp))
                }
            }
        }

        mainClock.advanceTimeByFrame()
        mainClock.advanceTimeBy(MotionTokens.SHIMMER_DURATION_MILLIS / 2L)

        onRoot().captureRoboImage(filePath = screenshotPath(name))
    }

    private companion object {
        const val CANVAS_WIDTH_PX = 440
        const val CANVAS_HEIGHT_PX = 80
        const val BOX_WIDTH_DP = 200
        const val BOX_HEIGHT_DP = 24
    }
}
