package core.presentation.component.mediaviewer.zoom.internal

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import core.presentation.component.mediaviewer.zoom.ZoomableConfig
import core.presentation.component.mediaviewer.zoom.ZoomableState
import kotlinx.coroutines.launch

internal fun Modifier.zoomGestures(
    state: ZoomableState,
    config: ZoomableConfig,
): Modifier = composed {
    val scope = rememberCoroutineScope()
    val latestConfig by rememberUpdatedState(config)
    this
        .then(
            if (config.enableDoubleTapZoom || config.onSingleTap != null) {
                Modifier.pointerInput(config.enableDoubleTapZoom, config.onSingleTap) {
                    detectTapGestures(
                        onTap = { latestConfig.onSingleTap?.invoke() },
                        onDoubleTap = if (config.enableDoubleTapZoom) {
                            { offset ->
                                scope.launch {
                                    if (state.isZoomed) state.resetZoom()
                                    else state.zoomTo(latestConfig.doubleTapZoom, offset)
                                }
                            }
                        } else {
                            null
                        },
                    )
                }
            } else Modifier,
        )
        .pointerInput(Unit) {
            awaitEachGesture {
                var wasMultiTouch = false
                var totalPanDistance = 0f
                awaitFirstDown(requireUnconsumed = false)
                do {
                    val event = awaitPointerEvent()
                    val pointers = event.changes.filter { it.pressed }
                    if (pointers.isNotEmpty()) {
                        val isMultiTouch = pointers.size > 1
                        if (isMultiTouch) wasMultiTouch = true
                        val zoom = event.calculateZoom()
                        val pan = event.calculatePan()
                        val centroid = event.calculateCentroid(useCurrent = true)
                        if (isMultiTouch || wasMultiTouch) {
                            if (zoom != 1f) state.onGestureZoom(zoom, centroid)
                            if (pan != Offset.Zero) state.onGesturePan(pan)
                            event.changes.forEach { it.consume() }
                        } else if (state.isZoomed && pan != Offset.Zero) {
                            state.onGesturePan(pan)
                            totalPanDistance += pan.getDistance()
                            if (totalPanDistance > 10f) {
                                event.changes.forEach { if (it.positionChanged()) it.consume() }
                            }
                        }
                    }
                } while (event.changes.any { it.pressed })
            }
        }
}
