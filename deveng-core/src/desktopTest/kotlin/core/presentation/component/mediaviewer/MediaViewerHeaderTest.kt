package core.presentation.component.mediaviewer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import core.presentation.theme.AppTheme
import core.presentation.theme.DefaultComponentTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class MediaViewerHeaderTest {

    @Test
    fun clickCloseButton_whenShown_sendsCloseClickOnce() = runComposeUiTest {
        var closeClickCount = 0
        setContent {
            AppTheme {
                MediaViewerHeader(
                    onCloseButtonClick = { closeClickCount++ },
                    closeContentDescription = CLOSE_DESCRIPTION,
                )
            }
        }

        onNodeWithContentDescription(CLOSE_DESCRIPTION).performClick()

        assertEquals(1, closeClickCount)
    }

    @Test
    fun showHeader_withActions_showsActionsNextToCloseButton() = runComposeUiTest {
        setContent {
            AppTheme {
                MediaViewerHeader(
                    onCloseButtonClick = {},
                    closeContentDescription = CLOSE_DESCRIPTION,
                    actions = { Text(text = ACTION_LABEL) },
                )
            }
        }

        onNodeWithContentDescription(CLOSE_DESCRIPTION).assertIsDisplayed()
        onNodeWithText(ACTION_LABEL).assertIsDisplayed()
    }

    @Test
    fun showHeader_withPanelBackgroundVisible_drawsPanelColor() = runComposeUiTest {
        showHeaderOnBackdrop(isPanelBackgroundVisible = true)

        assertTrue(capturedColors().contains(PANEL_COLOR))
    }

    @Test
    fun showHeader_withPanelBackgroundHidden_drawsNoPanelColor() = runComposeUiTest {
        showHeaderOnBackdrop(isPanelBackgroundVisible = false)

        assertFalse(capturedColors().contains(PANEL_COLOR))
    }

    private fun ComposeUiTest.showHeaderOnBackdrop(isPanelBackgroundVisible: Boolean) {
        val componentTheme = DefaultComponentTheme.copy(
            mediaViewerHeader = DefaultComponentTheme.mediaViewerHeader.copy(panelBackgroundColor = PANEL_COLOR)
        )
        setContent {
            AppTheme(darkTheme = false, componentTheme = componentTheme) {
                Box(modifier = Modifier.background(BACKDROP_COLOR)) {
                    MediaViewerHeader(
                        onCloseButtonClick = {},
                        isPanelBackgroundVisible = isPanelBackgroundVisible,
                        closeContentDescription = CLOSE_DESCRIPTION,
                    )
                }
            }
        }
    }

    private fun ComposeUiTest.capturedColors(): Set<Color> {
        val pixels = onRoot().captureToImage().toPixelMap()
        return buildSet { for (x in 0 until pixels.width) for (y in 0 until pixels.height) add(pixels[x, y]) }
    }

    private companion object {
        val PANEL_COLOR = Color(0xFFE53935)
        val BACKDROP_COLOR = Color(0xFF1E88E5)
        const val CLOSE_DESCRIPTION = "Close full screen"
        const val ACTION_LABEL = "Share"
    }
}
