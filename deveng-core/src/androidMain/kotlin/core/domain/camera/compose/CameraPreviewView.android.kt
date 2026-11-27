package core.domain.camera.compose

import androidx.camera.view.PreviewView
import android.graphics.Bitmap
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.Image
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import core.domain.camera.controller.CameraController
import kotlinx.coroutines.delay

/**
 * Android implementation of stateless camera preview.
 * Displays the camera feed using CameraX's PreviewView.
 * Double-tap on the preview switches between front and back camera.
 */
@Composable
actual fun CameraPreviewView(controller: CameraController, modifier: Modifier) {
    val context = LocalContext.current
    var transitionBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var showTransitionOverlay by remember { mutableStateOf(false) }
    val overlayAlpha by animateFloatAsState(
        targetValue = if (showTransitionOverlay && transitionBitmap != null) 1f else 0f,
        animationSpec = tween(durationMillis = 180),
        label = "cameraDeviceSwitchOverlayAlpha",
    )
    val overlayScale by animateFloatAsState(
        targetValue = if (showTransitionOverlay && transitionBitmap != null) 1.035f else 1f,
        animationSpec = tween(durationMillis = 240),
        label = "cameraDeviceSwitchOverlayScale",
    )
    val previewView = remember {
        PreviewView(context).apply {
            // Prefer full FoV and pin preview content to bottom on tall screens.
            // This keeps bottom camera controls visually on top of the preview.
            scaleType = PreviewView.ScaleType.FIT_END
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }
    LaunchedEffect(showTransitionOverlay, transitionBitmap) {
        if (!showTransitionOverlay && transitionBitmap != null) {
            delay(280)
            if (!showTransitionOverlay) {
                transitionBitmap?.recycle()
                transitionBitmap = null
            }
        }
    }
    DisposableEffect(controller, previewView) {
        controller.onDeviceTypeSwitchTransition = { switching ->
            if (switching) {
                transitionBitmap?.recycle()
                transitionBitmap = previewView.bitmap?.copy(Bitmap.Config.ARGB_8888, false)
                showTransitionOverlay = transitionBitmap != null
            } else {
                showTransitionOverlay = false
            }
        }
        controller.bindCamera(previewView) {
            // Camera is already bound and started by the state holder
        }
        onDispose {
            controller.onDeviceTypeSwitchTransition = null
            transitionBitmap?.recycle()
            transitionBitmap = null
            // Cleanup is handled by the state holder
        }
    }

    Box(modifier = modifier) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize(),
        )
        val overlayBitmap = transitionBitmap
        if (overlayBitmap != null && overlayAlpha > 0.01f) {
            Image(
                bitmap = overlayBitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = overlayAlpha
                        scaleX = overlayScale
                        scaleY = overlayScale
                    },
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
