package core.domain.camera.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import core.domain.camera.controller.CameraController

/**
 * Platform-specific overlay that handles pinch-to-zoom, tap-to-focus, and double-tap (switch camera).
 * On Android this uses the View system (ScaleGestureDetector) so gestures work immediately;
 * on other platforms it uses Compose pointerInput.
 *
 * @param controller Camera controller for zoom, focus, and lens switch.
 * @param modifier Modifier for the overlay (typically fillMaxSize).
 * @param onZoomChange Called when zoom level changes (e.g. to update UI state).
 * @param onDoubleTap Called on double-tap (e.g. switch camera and refresh zoom state).
 * @param onFocusPointTapped Called when user taps to focus with normalized coordinates (0..1). Use to show focus indicator.
 */
@Composable
expect fun CameraZoomGestureOverlay(
    controller: CameraController,
    modifier: Modifier,
    onZoomChange: (Float) -> Unit,
    onDoubleTap: () -> Unit,
    onFocusPointTapped: (normalizedX: Float, normalizedY: Float) -> Unit,
)
