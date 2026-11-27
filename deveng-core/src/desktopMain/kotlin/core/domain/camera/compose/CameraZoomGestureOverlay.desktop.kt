package core.domain.camera.compose

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import core.domain.camera.controller.CameraController
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

@Composable
actual fun CameraZoomGestureOverlay(
    controller: CameraController,
    modifier: Modifier,
    onZoomChange: (Float) -> Unit,
    onDoubleTap: () -> Unit,
    onFocusPointTapped: (normalizedX: Float, normalizedY: Float) -> Unit,
) {
    var size by remember { mutableStateOf(IntSize.Zero) }
    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { size = it }
            .pointerInput(controller, size) {
                coroutineScope {
                    launch {
                        detectTransformGestures { _, _, gestureZoom, _ ->
                            val current = controller.getZoom()
                            val max = controller.getMaxZoom().coerceAtLeast(1f)
                            val newZoom = (current * gestureZoom).coerceIn(1f, max)
                            controller.setZoom(newZoom)
                            onZoomChange(controller.getZoom())
                        }
                    }
                    launch {
                        detectTapGestures(
                            onTap = { offset: Offset ->
                                val s = size
                                if (s.width > 0 && s.height > 0) {
                                    val nx = (offset.x / s.width).coerceIn(0f, 1f)
                                    val ny = (offset.y / s.height).coerceIn(0f, 1f)
                                    if (controller.shouldSuppressTapToFocus?.invoke(nx, ny) == true) {
                                        return@detectTapGestures
                                    }
                                    controller.setFocusPoint(nx, ny)
                                    onFocusPointTapped(nx, ny)
                                }
                            },
                            onDoubleTap = { onDoubleTap() },
                        )
                    }
                }
            },
    )
}
