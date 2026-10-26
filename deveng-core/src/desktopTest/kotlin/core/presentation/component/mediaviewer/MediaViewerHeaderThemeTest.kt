package core.presentation.component.mediaviewer

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import core.presentation.theme.AppTheme
import core.presentation.theme.DefaultComponentTheme
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class MediaViewerHeaderThemeTest {

    @Test
    fun showHeader_withCustomLayoutTheme_usesThemePaddingsAndSizes() = runComposeUiTest {
        showHeader()

        val customTheme = CUSTOM_THEME.mediaViewerHeader
        val headerBounds = onNodeWithTag(HEADER_TAG).getUnclippedBoundsInRoot()
        val closeButtonBounds = onNodeWithContentDescription(CLOSE_DESCRIPTION).getUnclippedBoundsInRoot()
        val closeIconBounds = onNodeWithContentDescription(CLOSE_DESCRIPTION, useUnmergedTree = true).getUnclippedBoundsInRoot()
        val actionBounds = onNodeWithText(ACTION_LABEL).getUnclippedBoundsInRoot()
        assertDpEquals(
            customTheme.outerHorizontalPadding + customTheme.panelHorizontalPadding,
            closeButtonBounds.left - headerBounds.left
        )
        assertDpEquals(
            customTheme.outerVerticalPadding + customTheme.panelVerticalPadding,
            closeButtonBounds.top - headerBounds.top
        )
        assertDpEquals(customTheme.closeButtonSize, closeButtonBounds.width)
        assertDpEquals(customTheme.closeIconSize, closeIconBounds.width)
        assertDpEquals(
            headerBounds.right - customTheme.outerHorizontalPadding - customTheme.panelHorizontalPadding,
            actionBounds.right
        )
    }

    @Test
    fun showHeader_withPaddingParameters_prefersParametersOverTheme() = runComposeUiTest {
        showHeader(
            outerHorizontalPadding = PARAMETER_OUTER_PADDING,
            outerVerticalPadding = PARAMETER_OUTER_PADDING,
            panelHorizontalPadding = PARAMETER_PANEL_PADDING,
            panelVerticalPadding = PARAMETER_PANEL_PADDING
        )

        val headerBounds = onNodeWithTag(HEADER_TAG).getUnclippedBoundsInRoot()
        val closeButtonBounds = onNodeWithContentDescription(CLOSE_DESCRIPTION).getUnclippedBoundsInRoot()
        assertDpEquals(PARAMETER_OUTER_PADDING + PARAMETER_PANEL_PADDING, closeButtonBounds.left - headerBounds.left)
        assertDpEquals(PARAMETER_OUTER_PADDING + PARAMETER_PANEL_PADDING, closeButtonBounds.top - headerBounds.top)
    }

    @Test
    fun showHeader_withActionSpacing_separatesActionsByThemeValue() = runComposeUiTest {
        setContent {
            AppTheme(darkTheme = false, componentTheme = CUSTOM_THEME) {
                MediaViewerHeader(
                    onCloseButtonClick = {},
                    closeContentDescription = CLOSE_DESCRIPTION,
                    actions = {
                        Text(text = ACTION_LABEL)
                        Text(text = SECOND_ACTION_LABEL)
                    },
                )
            }
        }

        val firstActionBounds = onNodeWithText(ACTION_LABEL).getUnclippedBoundsInRoot()
        val secondActionBounds = onNodeWithText(SECOND_ACTION_LABEL).getUnclippedBoundsInRoot()
        assertDpEquals(CUSTOM_THEME.mediaViewerHeader.actionSpacing, secondActionBounds.left - firstActionBounds.right)
    }

    private fun ComposeUiTest.showHeader(
        outerHorizontalPadding: Dp? = null,
        outerVerticalPadding: Dp? = null,
        panelHorizontalPadding: Dp? = null,
        panelVerticalPadding: Dp? = null
    ) {
        setContent {
            AppTheme(darkTheme = false, componentTheme = CUSTOM_THEME) {
                Box(modifier = Modifier.testTag(HEADER_TAG)) {
                    MediaViewerHeader(
                        onCloseButtonClick = {},
                        closeContentDescription = CLOSE_DESCRIPTION,
                        outerHorizontalPadding = outerHorizontalPadding,
                        outerVerticalPadding = outerVerticalPadding,
                        panelHorizontalPadding = panelHorizontalPadding,
                        panelVerticalPadding = panelVerticalPadding,
                        actions = { Text(text = ACTION_LABEL) },
                    )
                }
            }
        }
        waitForIdle()
    }

    private fun assertDpEquals(expected: Dp, actual: Dp) {
        assertTrue(abs(expected.value - actual.value) <= DP_TOLERANCE, "expected $expected but was $actual")
    }

    private companion object {
        const val DP_TOLERANCE = 0.5f
        const val HEADER_TAG = "media_viewer_header"
        const val CLOSE_DESCRIPTION = "Close full screen"
        const val ACTION_LABEL = "Share"
        const val SECOND_ACTION_LABEL = "Save"
        val PARAMETER_OUTER_PADDING = 3.dp
        val PARAMETER_PANEL_PADDING = 5.dp
        val CUSTOM_THEME = DefaultComponentTheme.copy(
            mediaViewerHeader = DefaultComponentTheme.mediaViewerHeader.copy(
                outerHorizontalPadding = 14.dp,
                outerVerticalPadding = 11.dp,
                panelHorizontalPadding = 10.dp,
                panelVerticalPadding = 9.dp,
                panelCornerRadius = 4.dp,
                actionSpacing = 20.dp,
                closeButtonSize = 52.dp,
                closeIconSize = 30.dp
            )
        )
    }
}
