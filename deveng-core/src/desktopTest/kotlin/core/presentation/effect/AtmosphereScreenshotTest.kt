package core.presentation.effect

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.runDesktopComposeUiTest
import core.presentation.theme.AppTheme
import core.testing.screenshotPath
import io.github.takahirom.roborazzi.captureRoboImage
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class AtmosphereScreenshotTest {

    private val palette = SwirlPalette(base = Color(0xFF2A1B4D), accent = Color(0xFFFFC83D), shade = Color(0xFF7C3AED))

    @Test
    fun swirlBackground_reduceMotion_matchesBaseline() = capture(name = "swirl_background") {
        SwirlBackground(palette = palette)
    }

    @Test
    fun crtScreen_overSwirl_matchesBaseline() = capture(name = "crt_screen_over_swirl") {
        Box(modifier = Modifier.fillMaxSize().crtScreen(), contentAlignment = Alignment.Center) {
            SwirlBackground(palette = palette)
            Text(text = "Tarot", color = Color.White)
        }
    }

    @Test
    fun vortexFlash_atPeak_matchesBaseline() = capture(name = "vortex_flash_peak") {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            VortexFlash(progress = 0.5f, palette = palette)
        }
    }

    private fun capture(name: String, content: @Composable () -> Unit) =
        runDesktopComposeUiTest(width = CANVAS_WIDTH_PX, height = CANVAS_HEIGHT_PX) {
            setContent { AppTheme(darkTheme = false, reduceMotion = true) { content() } }
            waitForIdle()
            onRoot().captureRoboImage(filePath = screenshotPath(name = name))
        }

    private companion object {
        const val CANVAS_WIDTH_PX = 360
        const val CANVAS_HEIGHT_PX = 640
    }
}
