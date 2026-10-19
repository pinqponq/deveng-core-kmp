package core.util.markdown

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
class MarkdownContentParserScreenshotTest {

    @Test
    fun headingsListsBoldAndItalic_matchBaseline() =
        runDesktopComposeUiTest(width = CANVAS_WIDTH_PX, height = CANVAS_HEIGHT_PX) {
            setContent {
                CompositionLocalProvider(LocalDensity provides Density(density = SCREENSHOT_DENSITY)) {
                    AppTheme(darkTheme = false) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(BACKGROUND_COLOR)
                                .padding(16.dp)
                        ) {
                            MarkdownContentParser(
                                markdownContent = HEADINGS_LISTS_BOLD_AND_ITALIC,
                                textColor = TEXT_COLOR
                            )
                        }
                    }
                }
            }

            waitForIdle()

            onRoot().captureRoboImage(filePath = screenshotPath(name = "markdown_content_parser_headings_lists_bold_italic"))
        }

    private companion object {
        const val CANVAS_WIDTH_PX = 900
        const val CANVAS_HEIGHT_PX = 700
        val BACKGROUND_COLOR = Color(0xFF1E1E1E)
        val TEXT_COLOR = Color(0xFFE7E7E7)
        val HEADINGS_LISTS_BOLD_AND_ITALIC = """
            # Terms
            ## Account
            - You accept **safe usage** rules.
            - You can cancel _anytime_.

            ### Notes
            Keep this text short for demo.
        """.trimIndent()
    }
}
