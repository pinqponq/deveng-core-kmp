package core.presentation.component.shimmer

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import core.presentation.motion.LocalReduceMotion
import core.presentation.motion.MotionTokens
import core.presentation.theme.AppTheme
import core.presentation.theme.ComponentTheme
import core.presentation.theme.ShimmerTheme
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class ShimmerBoxTest {

    @Test
    fun shimmerBox_halfwayThroughSweep_showsHighlightInCenterAndBaseAtStart() = runDesktopComposeUiTest {
        setShimmerBox(isReduceMotionEnabled = false, baseColor = BASE_COLOR, highlightColor = HIGHLIGHT_COLOR)

        advanceShimmerTo(fraction = HALFWAY)

        assertColorClose(expected = HIGHLIGHT_COLOR, actual = pixelAt(xFraction = CENTER, yFraction = CENTER), tolerance = EDGE_TOLERANCE)
        assertColorClose(expected = BASE_COLOR, actual = pixelAt(xFraction = START, yFraction = CENTER), tolerance = EDGE_TOLERANCE)
    }

    @Test
    fun shimmerBox_atSweepStart_showsOnlyBaseColor() = runDesktopComposeUiTest {
        setShimmerBox(isReduceMotionEnabled = false, baseColor = BASE_COLOR, highlightColor = HIGHLIGHT_COLOR)

        advanceShimmerTo(fraction = 0f)

        assertColorClose(expected = BASE_COLOR, actual = pixelAt(xFraction = CENTER, yFraction = CENTER), tolerance = EDGE_TOLERANCE)
    }

    @Test
    fun shimmerBox_withReduceMotion_showsStaticBaseColorEverywhere() = runDesktopComposeUiTest {
        setShimmerBox(isReduceMotionEnabled = true, baseColor = BASE_COLOR, highlightColor = HIGHLIGHT_COLOR)

        advanceShimmerTo(fraction = HALFWAY)

        assertColorClose(expected = BASE_COLOR, actual = pixelAt(xFraction = START, yFraction = CENTER))
        assertColorClose(expected = BASE_COLOR, actual = pixelAt(xFraction = CENTER, yFraction = CENTER))
        assertColorClose(expected = BASE_COLOR, actual = pixelAt(xFraction = END, yFraction = CENTER))
    }

    @Test
    fun shimmerBox_withoutColors_usesThemeColorsAndCornerRadius() = runDesktopComposeUiTest {
        val shimmerTheme = ShimmerTheme(baseColor = BASE_COLOR, highlightColor = HIGHLIGHT_COLOR, cornerRadius = LARGE_CORNER_RADIUS)
        setContent {
            CompositionLocalProvider(LocalDensity provides Density(density = 1f)) {
                AppTheme(
                    darkTheme = false,
                    componentTheme = ComponentTheme(shimmer = shimmerTheme),
                    reduceMotion = true
                ) {
                    ShimmerBox(modifier = Modifier.size(width = BOX_WIDTH_DP.dp, height = BOX_HEIGHT_DP.dp))
                }
            }
        }

        assertColorClose(expected = BASE_COLOR, actual = pixelAt(xFraction = CENTER, yFraction = CENTER))
        assertEquals(0f, pixelAt(xFraction = 0f, yFraction = 0f).alpha, absoluteTolerance = ALPHA_TOLERANCE)
    }

    private fun ComposeUiTest.setShimmerBox(isReduceMotionEnabled: Boolean, baseColor: Color, highlightColor: Color) {
        mainClock.autoAdvance = false
        setContent {
            CompositionLocalProvider(
                LocalDensity provides Density(density = 1f),
                LocalReduceMotion provides isReduceMotionEnabled
            ) {
                ShimmerBox(
                    modifier = Modifier.size(width = BOX_WIDTH_DP.dp, height = BOX_HEIGHT_DP.dp),
                    cornerRadius = 0.dp,
                    baseColor = baseColor,
                    highlightColor = highlightColor
                )
            }
        }

        mainClock.advanceTimeByFrame()
    }

    private fun ComposeUiTest.advanceShimmerTo(fraction: Float) {
        mainClock.advanceTimeBy((MotionTokens.SHIMMER_DURATION_MILLIS * fraction).toLong())
    }

    private fun ComposeUiTest.pixelAt(xFraction: Float, yFraction: Float): Color {
        val pixelMap = onRoot().captureToImage().toPixelMap()
        val x = ((pixelMap.width - 1) * xFraction).toInt()
        val y = ((pixelMap.height - 1) * yFraction).toInt()
        return pixelMap[x, y]
    }

    private fun assertColorClose(expected: Color, actual: Color, tolerance: Float = COLOR_TOLERANCE) {
        val isClose = abs(expected.red - actual.red) <= tolerance &&
            abs(expected.green - actual.green) <= tolerance &&
            abs(expected.blue - actual.blue) <= tolerance
        assertTrue(isClose, "expected $expected but was $actual")
    }

    private companion object {
        val BASE_COLOR = Color.Black
        val HIGHLIGHT_COLOR = Color.White
        val LARGE_CORNER_RADIUS: Dp = 20.dp
        const val BOX_WIDTH_DP = 400
        const val BOX_HEIGHT_DP = 40
        const val HALFWAY = 0.5f
        const val START = 0f
        const val CENTER = 0.5f
        const val END = 1f
        const val COLOR_TOLERANCE = 0.08f
        const val EDGE_TOLERANCE = 0.2f
        const val ALPHA_TOLERANCE = 0.01f
    }
}
