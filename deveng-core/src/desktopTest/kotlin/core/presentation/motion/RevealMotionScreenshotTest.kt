package core.presentation.motion

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
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
class RevealMotionScreenshotTest {

    @Test
    fun flipCard_backFaceUp_matchesBaseline() = captureFlipCard(isFlipped = false, name = "flip_card_back_face")

    @Test
    fun flipCard_frontFaceUp_matchesBaseline() = captureFlipCard(isFlipped = true, name = "flip_card_front_face")

    @Test
    fun countUpText_afterCount_matchesBaseline() = runDesktopComposeUiTest(width = CANVAS_WIDTH_PX, height = CANVAS_HEIGHT_PX) {
        setContent {
            CompositionLocalProvider(LocalDensity provides Density(density = SCREENSHOT_DENSITY)) {
                AppTheme(darkTheme = false) {
                    Box(modifier = Modifier.background(Color.White).padding(16.dp)) {
                        CountUpText(value = COUNT_TARGET, formatter = { shownValue -> "$shownValue km" })
                    }
                }
            }
        }
        mainClock.advanceTimeBy(COUNT_SETTLE_MILLIS)

        onRoot().captureRoboImage(filePath = screenshotPath(name = "count_up_text_after_count"))
    }

    private fun captureFlipCard(isFlipped: Boolean, name: String) =
        runDesktopComposeUiTest(width = CANVAS_WIDTH_PX, height = CANVAS_HEIGHT_PX) {
            setContent {
                CompositionLocalProvider(LocalDensity provides Density(density = SCREENSHOT_DENSITY)) {
                    AppTheme(darkTheme = false, reduceMotion = true) {
                        Row(
                            modifier = Modifier.background(Color.White).padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            FlipCard(
                                modifier = Modifier.size(width = 120.dp, height = 168.dp),
                                isFlipped = isFlipped,
                                front = { Face(text = "Front", color = Color(0xFF7C3AED)) },
                                back = { Face(text = "Back", color = Color(0xFF1F1F1F)) }
                            )
                        }
                    }
                }
            }
            waitForIdle()

            onRoot().captureRoboImage(filePath = screenshotPath(name = name))
        }

    @Composable
    private fun Face(text: String, color: Color) {
        Box(
            modifier = Modifier.size(width = 120.dp, height = 168.dp).background(color),
            contentAlignment = Alignment.Center
        ) {
            Text(text = text, color = Color.White)
        }
    }

    private companion object {
        const val CANVAS_WIDTH_PX = 600
        const val CANVAS_HEIGHT_PX = 450
        const val COUNT_TARGET = 640
        const val COUNT_SETTLE_MILLIS = 3000L
    }
}
