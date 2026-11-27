package core.domain.camera.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import core.domain.camera.controller.CameraController

@Composable
actual fun CameraZoomGestureOverlay(
    controller: CameraController,
    modifier: Modifier,
    onZoomChange: (Float) -> Unit,
    onDoubleTap: () -> Unit,
    onFocusPointTapped: (normalizedX: Float, normalizedY: Float) -> Unit,
) {
    val context = LocalContext.current
    val onZoomChangeState = rememberUpdatedState(onZoomChange)
    val onDoubleTapState = rememberUpdatedState(onDoubleTap)
    val onFocusPointTappedState = rememberUpdatedState(onFocusPointTapped)

    AndroidView(
        factory = {
            CameraZoomGestureView(
                context = it,
                controller = controller,
                onZoomChange = { onZoomChangeState.value(it) },
                onDoubleTap = { onDoubleTapState.value() },
                onFocusPointTapped = { x, y -> onFocusPointTappedState.value(x, y) },
            )
        },
        update = { view ->
            view.update(
                newController = controller,
                newOnZoomChange = { onZoomChangeState.value(it) },
                newOnDoubleTap = { onDoubleTapState.value() },
                newOnFocusPointTapped = { x, y -> onFocusPointTappedState.value(x, y) },
            )
        },
        modifier = modifier,
    )
}
