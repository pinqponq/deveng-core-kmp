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
import core.presentation.theme.LocalComponentTheme
import global.deveng.deveng_core.generated.resources.Res
import global.deveng.deveng_core.generated.resources.shared_content_description_close_full_screen
import org.jetbrains.compose.resources.stringResource

/**
 * Top bar for a fullscreen [MediaViewer]: a back control on the left and an optional trailing
 * action row on the right, laid out on a dark rounded panel. Pass it to [MediaViewer]'s `topBar`.
 * Panel color, back icon tint and size, and the default paddings come from
 * [core.presentation.theme.MediaViewerHeaderTheme].
 *
 * @param onCloseButtonClick Invoked when the back control is tapped.
 * @param modifier Applied to the header row, e.g. for enter/exit animation modifiers.
 * @param closeContentDescription Accessibility description of the back control.
 * @param isPanelBackgroundVisible Whether the dark rounded panel is drawn behind the row.
 * @param outerHorizontalPadding Horizontal inset between the header and the viewer edges. If null, uses the theme value.
 * @param outerVerticalPadding Vertical inset between the header and the viewer edges. If null, uses the theme value.
 * @param panelHorizontalPadding Horizontal padding inside the panel. If null, uses the theme value.
 * @param panelVerticalPadding Vertical padding inside the panel. If null, uses the theme value.
 * @param panelCornerRadius Corner radius of the panel. If null, uses the theme value.
 * @param actionSpacing Gap between the back control and the actions, and between the actions. If null, uses the
 * theme value.
 * @param actions Trailing controls, laid out end-aligned next to the back control.
 */
@Composable
fun MediaViewerHeader(
    onCloseButtonClick: () -> Unit,
    modifier: Modifier = Modifier,
    closeContentDescription: String = stringResource(Res.string.shared_content_description_close_full_screen),
    isPanelBackgroundVisible: Boolean = true,
    outerHorizontalPadding: Dp? = null,
    outerVerticalPadding: Dp? = null,
    panelHorizontalPadding: Dp? = null,
    panelVerticalPadding: Dp? = null,
    panelCornerRadius: Dp? = null,
    actionSpacing: Dp? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val mediaViewerHeaderTheme = LocalComponentTheme.current.mediaViewerHeader
    val finalOuterHorizontalPadding = outerHorizontalPadding ?: mediaViewerHeaderTheme.outerHorizontalPadding
    val finalOuterVerticalPadding = outerVerticalPadding ?: mediaViewerHeaderTheme.outerVerticalPadding
    val finalPanelHorizontalPadding = panelHorizontalPadding ?: mediaViewerHeaderTheme.panelHorizontalPadding
    val finalPanelVerticalPadding = panelVerticalPadding ?: mediaViewerHeaderTheme.panelVerticalPadding
    val finalPanelCornerRadius = panelCornerRadius ?: mediaViewerHeaderTheme.panelCornerRadius
    val finalActionSpacing = actionSpacing ?: mediaViewerHeaderTheme.actionSpacing
    val panelModifier = if (isPanelBackgroundVisible) {
        Modifier
            .background(
                color = mediaViewerHeaderTheme.panelBackgroundColor,
                shape = RoundedCornerShape(finalPanelCornerRadius),
            )
            .padding(
                horizontal = finalPanelHorizontalPadding,
                vertical = finalPanelVerticalPadding,
            )
    } else {
        Modifier
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = finalOuterHorizontalPadding,
                vertical = finalOuterVerticalPadding,
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
                .padding(start = finalActionSpacing),
            horizontalArrangement = Arrangement.spacedBy(finalActionSpacing, Alignment.End),
            verticalAlignment = Alignment.CenterVertically,
            content = actions,
        )
    }
}
