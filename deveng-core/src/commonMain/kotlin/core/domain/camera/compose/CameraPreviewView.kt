package core.domain.camera.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import core.domain.camera.controller.CameraController

/**
 * Stateless camera preview composable for the new Compose-first API.
 * This is a pure rendering component that displays the camera feed from a [CameraController].
 *
 * **Default behavior:** Double-tap on the preview switches between front and back camera
 * (Android and iOS); on other platforms the gesture is a no-op.
 *
 * **Usage with new API:**
 * ```kotlin
 * val cameraState by rememberCameraKState()
 *
 * when (val state = cameraState) {
 *     is CameraKState.Ready -> {
 *         CameraPreviewView(
 *             controller = state.controller,
 *             modifier = Modifier.fillMaxSize()
 *         )
 *     }
 *     else -> {}
 * }
 * ```
 *
 * @param controller The initialized camera controller.
 * @param modifier Modifier to be applied to the preview.
 */
@Composable
expect fun CameraPreviewView(controller: CameraController, modifier: Modifier = Modifier)
