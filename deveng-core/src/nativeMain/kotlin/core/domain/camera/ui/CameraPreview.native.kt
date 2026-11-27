package core.domain.camera.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitViewController
import core.domain.camera.builder.CameraControllerBuilder
import core.domain.camera.builder.createIOSCameraControllerBuilder
import core.domain.camera.controller.CameraController

/**
 * iOS-specific implementation of [CameraPreview].
 *
 * The preview's video orientation is not driven from here: the controller re-resolves it on every
 * layout pass, which is when the interface has actually rotated. Reacting to device rotation
 * instead used to turn the image sideways inside a preview the host had pinned to portrait.
 *
 * @param modifier Modifier to be applied to the camera preview.
 * @param cameraConfiguration Lambda to configure the [CameraControllerBuilder].
 * @param onCameraControllerReady Callback invoked with the initialized [CameraController].
 */
@Composable
actual fun expectCameraPreview(
    modifier: Modifier,
    cameraConfiguration: CameraControllerBuilder.() -> Unit,
    onCameraControllerReady: (CameraController) -> Unit,
) {
    val cameraController =
        remember {
            createIOSCameraControllerBuilder()
                .apply(cameraConfiguration)
                .build()
        }

    LaunchedEffect(cameraController) {
        onCameraControllerReady(cameraController)
    }

    // Key on controller to force recreation when it changes
    key(cameraController) {
        UIKitViewController(
            factory = { cameraController },
            modifier = modifier,
            update = { viewController ->
                // Modifier is applied by the UIKitViewController wrapper
                // Background color and other styling can be set via the modifier parameter
            },
        )
    }
}
