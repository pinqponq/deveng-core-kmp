package core.domain.camera.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import core.domain.camera.builder.CameraControllerBuilder
import core.domain.camera.controller.CameraController

/**
 * WASM/JS noop: camera preview is not supported; renders nothing.
 */
@Composable
actual fun expectCameraPreview(
    modifier: Modifier,
    cameraConfiguration: CameraControllerBuilder.() -> Unit,
    onCameraControllerReady: (CameraController) -> Unit,
) {
    // No-op: no camera on WASM
}
