package core.presentation.component.mediaviewer

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import core.presentation.component.CustomIconButton
import core.presentation.theme.LocalComponentTheme
import global.deveng.deveng_core.generated.resources.Res
import global.deveng.deveng_core.generated.resources.shared_ic_arrow_left

@Composable
internal fun MediaViewerCloseButton(
    closeContentDescription: String,
    onCloseClick: () -> Unit,
) {
    val mediaViewerHeaderTheme = LocalComponentTheme.current.mediaViewerHeader

    CustomIconButton(
        icon = Res.drawable.shared_ic_arrow_left,
        iconDescription = closeContentDescription,
        iconTint = mediaViewerHeaderTheme.closeIconTint,
        iconModifier = Modifier.size(mediaViewerHeaderTheme.closeIconSize),
        buttonSize = mediaViewerHeaderTheme.closeButtonSize,
        backgroundColor = Color.Transparent,
        shadowElevation = 0.dp,
        onClick = onCloseClick,
    )
}
