package core.presentation.component.mediaviewer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import core.presentation.theme.LocalComponentTheme
import global.deveng.deveng_core.generated.resources.Res
import global.deveng.deveng_core.generated.resources.shared_content_description_close_full_screen
import org.jetbrains.compose.resources.stringResource

private val DefaultHeaderOuterHorizontalPadding = 8.dp
private val DefaultHeaderOuterVerticalPadding = 8.dp
private val DefaultHeaderPanelHorizontalPadding = 8.dp
private val DefaultHeaderPanelVerticalPadding = 6.dp
private val DefaultHeaderPanelCornerRadius = 12.dp
private val DefaultHeaderActionSpacing = 8.dp

/**
 * Top bar for a fullscreen [MediaViewer]: a back control on the left and an optional trailing
 * action row on the right, laid out on a dark rounded panel. Pass it to [MediaViewer]'s `topBar`.
 * Panel color and back icon tint come from [core.presentation.theme.MediaViewerHeaderTheme].
 *
 * @param onCloseButtonClick Invoked when the back control is tapped.
 * @param modifier Applied to the header row, e.g. for enter/exit animation modifiers.
 * @param closeContentDescription Accessibility description of the back control.
 * @param isPanelBackgroundVisible Whether the dark rounded panel is drawn behind the row.
 * @param outerHorizontalPadding Horizontal inset between the header and the viewer edges.
 * @param outerVerticalPadding Vertical inset between the header and the viewer edges.
 * @param panelHorizontalPadding Horizontal padding inside the panel.
 * @param panelVerticalPadding Vertical padding inside the panel.
 * @param panelCornerRadius Corner radius of the panel.
 * @param actionSpacing Gap between the back control and the actions, and between the actions.
 * @param actions Trailing controls, laid out end-aligned next to the back control.
 */
@Composable
fun MediaViewerHeader(
    onCloseButtonClick: () -> Unit,
    modifier: Modifier = Modifier,
    closeContentDescription: String = stringResource(Res.string.shared_content_description_close_full_screen),
    isPanelBackgroundVisible: Boolean = true,
    outerHorizontalPadding: Dp = DefaultHeaderOuterHorizontalPadding,
    outerVerticalPadding: Dp = DefaultHeaderOuterVerticalPadding,
    panelHorizontalPadding: Dp = DefaultHeaderPanelHorizontalPadding,
    panelVerticalPadding: Dp = DefaultHeaderPanelVerticalPadding,
    panelCornerRadius: Dp = DefaultHeaderPanelCornerRadius,
    actionSpacing: Dp = DefaultHeaderActionSpacing,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val mediaViewerHeaderTheme = LocalComponentTheme.current.mediaViewerHeader
    val panelModifier = if (isPanelBackgroundVisible) {
        Modifier
            .background(
                color = mediaViewerHeaderTheme.panelBackgroundColor,
                shape = RoundedCornerShape(panelCornerRadius),
            )
            .padding(
                horizontal = panelHorizontalPadding,
                vertical = panelVerticalPadding,
            )
    } else {
        Modifier
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = outerHorizontalPadding,
                vertical = outerVerticalPadding,
            )
            .then(panelModifier),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MediaViewerCloseButton(
            closeContentDescription = closeContentDescription,
            onCloseClick = onCloseButtonClick,
        )

        Row(
            modifier = Modifier
                .weight(weight = 1f)
                .padding(start = actionSpacing),
            horizontalArrangement = Arrangement.spacedBy(actionSpacing, Alignment.End),
            verticalAlignment = Alignment.CenterVertically,
            content = actions,
        )
    }
}
