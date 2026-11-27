package core.presentation.component.mediaviewer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.unit.Density
import core.presentation.theme.AppTheme
import core.testing.SCREENSHOT_DENSITY
import core.testing.screenshotPath
import io.github.takahirom.roborazzi.captureRoboImage
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class MediaViewerHeaderScreenshotTest {

    @Test
    fun header_withActions_matchesBaseline() = runDesktopComposeUiTest(width = CANVAS_WIDTH_PX, height = CANVAS_HEIGHT_PX) {
        setContent {
            CompositionLocalProvider(LocalDensity provides Density(density = SCREENSHOT_DENSITY)) {
                AppTheme(darkTheme = false) {
                    Box(modifier = Modifier.testTag(HEADER_TAG).background(color = Color.Black)) {
                        MediaViewerHeader(
                            onCloseButtonClick = {},
                            closeContentDescription = CLOSE_DESCRIPTION,
                            actions = { Text(text = ACTION_LABEL, color = Color.White) },
                        )
                    }
                }
            }
        }
        waitForIdle()

        onNodeWithTag(HEADER_TAG).captureRoboImage(filePath = screenshotPath(name = "media_viewer_header_with_actions"))
    }

    private companion object {
        const val CANVAS_WIDTH_PX = 720
        const val CANVAS_HEIGHT_PX = 200
        const val HEADER_TAG = "media_viewer_header"
        const val CLOSE_DESCRIPTION = "Close full screen"
        const val ACTION_LABEL = "Share"
    }
}
