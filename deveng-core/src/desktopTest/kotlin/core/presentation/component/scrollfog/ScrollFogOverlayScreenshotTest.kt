package core.presentation.component.scrollfog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import core.presentation.theme.AppTheme
import core.testing.SCREENSHOT_DENSITY
import core.testing.screenshotPath
import io.github.takahirom.roborazzi.captureRoboImage
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class ScrollFogOverlayScreenshotTest {

    @Test
    fun render_whenVisibleWithCustomColor_matchesBaseline() = captureFog(
        name = "scroll_fog_visible_custom_color",
        isVisible = true,
        fogColor = FOG_COLOR,
    )

    @Test
    fun render_whenHidden_matchesBaseline() = captureFog(
        name = "scroll_fog_hidden",
        isVisible = false,
        fogColor = FOG_COLOR,
    )

    private fun captureFog(name: String, isVisible: Boolean, fogColor: Color) =
        runDesktopComposeUiTest(width = CANVAS_PX, height = CANVAS_PX) {
            mainClock.autoAdvance = true
            setContent {
                CompositionLocalProvider(LocalDensity provides Density(density = SCREENSHOT_DENSITY)) {
                    AppTheme(darkTheme = false) {
                        Box(modifier = Modifier.size(CANVAS_DP).background(CONTENT_COLOR)) {
                            ScrollFogOverlay(isVisible = isVisible, color = fogColor)
                        }
                    }
                }
            }
            waitForIdle()
            mainClock.advanceTimeBy(FOG_SETTLE_MILLIS)

            onRoot().captureRoboImage(filePath = screenshotPath(name = name))
        }

    private companion object {
        const val CANVAS_PX = 400
        const val FOG_SETTLE_MILLIS = 500L
        val CANVAS_DP = 160.dp
        val CONTENT_COLOR = Color(0xFF3D5AFE)
        val FOG_COLOR = Color(0xFFFFF3E0)
    }
}
