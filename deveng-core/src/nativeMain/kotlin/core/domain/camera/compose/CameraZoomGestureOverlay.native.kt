package core.domain.camera.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import core.domain.camera.controller.CameraController

/**
 * iOS no-op: gesture handling (tap, double-tap, pinch zoom) is co-located inside
 * [CameraPreviewView] so the Compose gesture Box is a direct sibling of the
 * UIKitViewController — the only layout where touches reliably reach Compose on iOS.
 */
@Composable
actual fun CameraZoomGestureOverlay(
    controller: CameraController,
    modifier: Modifier,
    onZoomChange: (Float) -> Unit,
    onDoubleTap: () -> Unit,
    onFocusPointTapped: (normalizedX: Float, normalizedY: Float) -> Unit,
) {
    Box(modifier = modifier.fillMaxSize())
}
