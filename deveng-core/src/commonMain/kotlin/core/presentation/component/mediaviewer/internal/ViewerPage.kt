package core.presentation.component.mediaviewer.internal

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import core.presentation.component.mediaviewer.MediaViewerState
import core.presentation.component.mediaviewer.zoom.ZoomableConfig
import core.presentation.component.mediaviewer.zoom.internal.ZoomableBox
import core.presentation.component.mediaviewer.zoom.rememberZoomableState
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
fun ViewerPage(
    page: Int,
    imageModel: Any,
    isCurrentPage: Boolean,
    viewerState: MediaViewerState,
    zoomableConfig: ZoomableConfig,
    zoomEnabled: Boolean,
    onZoomChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    sharedModifier: Modifier = Modifier,
    content: (@Composable (page: Int, item: Any) -> Unit)?,
) {
    if (!zoomEnabled) {
        Box(modifier = modifier.then(sharedModifier).fillMaxSize(), contentAlignment = Alignment.Center) {
            if (content != null) {
                content(page, imageModel)
            } else {
                val context = LocalPlatformContext.current
                AsyncImage(
                    model = ImageRequest.Builder(context).data(imageModel).build(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        return
    }

    val zoomableState = rememberZoomableState(config = zoomableConfig, resetKey = imageModel)

    LaunchedEffect(isCurrentPage, zoomableState) {
        if (isCurrentPage) viewerState.currentZoomableState = zoomableState
    }

    DisposableEffect(zoomableState) {
        onDispose {
            if (viewerState.currentZoomableState === zoomableState) {
                viewerState.currentZoomableState = null
            }
        }
    }

    val currentOnZoomChanged by rememberUpdatedState(onZoomChanged)
    LaunchedEffect(zoomableState) {
        snapshotFlow { zoomableState.isZoomed }
            .distinctUntilChanged()
            .collect { currentOnZoomChanged(it) }
    }

    Box(modifier = modifier.then(sharedModifier).fillMaxSize(), contentAlignment = Alignment.Center) {
        ZoomableBox(
            zoomableState = zoomableState,
            config = zoomableConfig,
            modifier = Modifier.fillMaxSize(),
        ) {
            if (content != null) {
                content(page, imageModel)
            } else {
                val context = LocalPlatformContext.current
                AsyncImage(
                    model = ImageRequest.Builder(context).data(imageModel).build(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}
