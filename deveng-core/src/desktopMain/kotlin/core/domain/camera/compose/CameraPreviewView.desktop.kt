package core.domain.camera.compose

import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import core.domain.camera.controller.CameraController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.launch

/**
 * Desktop implementation of stateless camera preview.
 * Displays frames from JavaCV frame grabber.
 * Double-tap on the preview switches camera (no-op on desktop).
 */
@Composable
actual fun CameraPreviewView(controller: CameraController, modifier: Modifier) {
    BoxWithConstraints(modifier = modifier) {
        val scope = rememberCoroutineScope()
        val frameChannel = controller.getFrameChannel()
        var currentFrame by remember { mutableStateOf<ImageBitmap?>(null) }

        LaunchedEffect(controller) {
            scope.launch(Dispatchers.Default) {
                frameChannel.consumeAsFlow().collect { frame ->
                    currentFrame = frame.toComposeImageBitmap()
                }
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            currentFrame?.let { frame ->
                Image(
                    bitmap = frame,
                    contentDescription = "Camera Preview",
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(controller) {
                        detectTapGestures(
                            onDoubleTap = { controller.toggleCameraLens() },
                        )
                    },
            )
        }
    }
}
