package core.presentation.component.mediaviewer

import androidx.compose.material3.Text
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import core.presentation.theme.AppTheme
import kotlin.test.Test
import kotlin.test.assertEquals

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
    fun header_withActions_showsActionsNextToCloseButton() = runComposeUiTest {
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

    private companion object {
        const val CLOSE_DESCRIPTION = "Close full screen"
        const val ACTION_LABEL = "Share"
    }
}
