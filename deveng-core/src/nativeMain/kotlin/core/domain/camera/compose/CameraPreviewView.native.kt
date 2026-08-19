package core.domain.camera.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitViewController
import core.domain.camera.controller.CameraController

/**
 * iOS camera preview. Gesture handling (tap-to-focus, double-tap switch, pinch zoom)
 * is done via native UIKit gesture recognizers on the controller's view so they fire
 * on the very first touch (Compose overlays lose the first touch to UIKit interop routing).
 *
 * The preview's video orientation is not driven from here: the controller re-resolves it on every
 * layout pass, which is when the interface has actually rotated. Reacting to device rotation
 * instead used to turn the image sideways inside a preview the host had pinned to portrait.
 */
@Composable
actual fun CameraPreviewView(controller: CameraController, modifier: Modifier) {
    key(controller) {
        DisposableEffect(controller) {
            controller.logPreviewDebug("COMPOSE_UIKitView_MOUNT")
            onDispose {
                controller.logPreviewDebug("COMPOSE_UIKitView_DISPOSE")
            }
        }

        Box(modifier = modifier) {
            UIKitViewController(
                factory = { controller },
                modifier = Modifier.fillMaxSize(),
                update = { }
            )
        }
    }
}
