package core.presentation.component.mediaviewer.internal

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Wraps [content] in a drag that dismisses it, shrinking and fading it as the drag goes on.
 *
 * @param enabled Whether the dismiss drag is detected at all. The pointer node stays in place
 *   either way, since removing it mid-gesture can cancel sibling detectors while fingers are down.
 * @param threshold Fraction of the container's extent along [orientation] the drag must cover to
 *   dismiss on release.
 * @param velocityThreshold Release velocity that dismisses regardless of how far the drag got.
 * @param onDismiss Called once the drag has asked for dismissal.
 * @param onProgressChanged Reports the drag's fraction of the container, 0 to 1, as it moves.
 * @param onDragging Reports whether a dismiss drag is currently under way.
 * @param orientation Axis the dismiss drag is detected on. Must differ from the pager's own
 *   paging axis, or the two gesture detectors fight over the same drag — vertical paging needs
 *   [Orientation.Horizontal] here, and horizontal paging (the default media viewer) needs
 *   [Orientation.Vertical].
 * @param content The content being dragged.
 */
@Composable
fun SwipeToDismissBox(
    enabled: Boolean,
    threshold: Float,
    velocityThreshold: Float,
    onDismiss: () -> Unit,
    onProgressChanged: (Float) -> Unit,
    onDragging: (Boolean) -> Unit,
    orientation: Orientation = Orientation.Vertical,
    content: @Composable () -> Unit,
) {
    val offset = remember { Animatable(0f) }
    var containerExtent by remember { mutableFloatStateOf(1f) }
    val scope = rememberCoroutineScope()

    val draggableState = rememberDraggableState { delta ->
        scope.launch {
            offset.snapTo(offset.value + delta)
            val progress = (abs(offset.value) / containerExtent).coerceIn(0f, 1f)
            onProgressChanged(progress)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged {
                containerExtent = when (orientation) {
                    Orientation.Vertical -> it.height
                    Orientation.Horizontal -> it.width
                }.toFloat().coerceAtLeast(1f)
            }
            // Keep pointer node stable; toggling modifier on/off mid-pinch can cancel
            // sibling gesture detectors while fingers are still down.
            .draggable(
                enabled = enabled,
                state = draggableState,
                orientation = orientation,
                onDragStarted = { onDragging(true) },
                onDragStopped = { velocity ->
                    val fraction = abs(offset.value) / containerExtent
                    if (fraction >= threshold || abs(velocity) >= velocityThreshold) {
                        onDragging(false)
                        onDismiss()
                    } else {
                        scope.launch {
                            offset.animateTo(0f, spring())
                            onProgressChanged(0f)
                            onDragging(false)
                        }
                    }
                },
            )
            .graphicsLayer {
                if (enabled) {
                    val progress = (abs(offset.value) / containerExtent).coerceIn(0f, 1f)
                    when (orientation) {
                        Orientation.Vertical -> translationY = offset.value
                        Orientation.Horizontal -> translationX = offset.value
                    }
                    val scale = 1f - (progress * 0.2f)
                    scaleX = scale
                    scaleY = scale
                }
            },
    ) {
        content()
    }
}
