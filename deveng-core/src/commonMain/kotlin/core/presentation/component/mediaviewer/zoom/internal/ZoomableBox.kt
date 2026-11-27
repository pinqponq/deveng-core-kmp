package core.presentation.component.mediaviewer.zoom.internal

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import core.presentation.component.mediaviewer.zoom.ZoomableConfig
import core.presentation.component.mediaviewer.zoom.ZoomableState

@Composable
internal fun ZoomableBox(
    zoomableState: ZoomableState,
    config: ZoomableConfig,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val transformation = zoomableState.transformation
    Box(
        modifier = modifier
            .clipToBounds()
            .onSizeChanged { zoomableState.setLayoutSize(it) }
            .then(if (enabled) Modifier.zoomGestures(zoomableState, config) else Modifier)
            .graphicsLayer {
                scaleX = transformation.scale.scaleX
                scaleY = transformation.scale.scaleY
                translationX = transformation.offset.x
                translationY = transformation.offset.y
                rotationZ = transformation.rotationZ
            },
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}
