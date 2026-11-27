package core.domain.camera.compose

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

private const val CIRCLE_RADIUS_DP = 38f

/**
 * Outer diameter of the tap-to-focus ring (2 × [CIRCLE_RADIUS_DP]).
 * Use for aligning controls to the reticle (e.g. vertical exposure track length).
 */
val FocusIndicatorRingDiameter: Dp
    get() = (CIRCLE_RADIUS_DP * 2f).dp

private const val STROKE_WIDTH_DP = 1f
private const val STAY_DURATION_MS = 3000L

/**
 * Tap-to-focus indicator: single circle (no fill). On tap it minimizes a little,
 * stays on screen for 3 seconds, then fades out. Draw-only overlay.
 * @param keepVisible When true, the stay countdown is paused (e.g. while user adjusts brightness).
 */
@Composable
fun FocusIndicator(
    tapPosition: Offset?,
    onAnimationComplete: () -> Unit,
    modifier: Modifier = Modifier,
    keepVisible: Boolean = false,
) {
    val density = LocalDensity.current
    var size by remember { mutableStateOf(IntSize.Zero) }

    val scale = remember { Animatable(1f) }
    val alpha = remember { Animatable(1f) }

    LaunchedEffect(tapPosition, keepVisible) {
        if (tapPosition == null) {
            return@LaunchedEffect
        }
        scale.snapTo(1f)
        alpha.snapTo(1f)

        // Minimize a little (scale 1 → 0.9)
        scale.animateTo(0.9f, animationSpec = tween(150))

        // Stay on screen for 3 seconds; don't count down while keepVisible is true
        var remaining = STAY_DURATION_MS
        while (remaining > 0) {
            delay(100)
            if (!keepVisible) remaining -= 100
        }

        // Fade out
        alpha.animateTo(0f, animationSpec = tween(200))

        onAnimationComplete()
    }

    val scaleVal = scale.value
    val alphaVal = alpha.value

    Canvas(
        modifier = modifier.onSizeChanged {
            size = it
        },
    ) {
        val pos = tapPosition ?: return@Canvas
        val s = size
        if (s.width <= 0 || s.height <= 0) {
            return@Canvas
        }

        val strokeWidthPx = with(density) { STROKE_WIDTH_DP.dp.toPx() }
        val radiusPx = with(density) { CIRCLE_RADIUS_DP.dp.toPx() }

        val centerX = pos.x.coerceIn(0f, s.width.toFloat())
        val centerY = pos.y.coerceIn(0f, s.height.toFloat())
        val center = Offset(centerX, centerY)

        if (alphaVal > 0f && scaleVal > 0f) {
            drawCircle(
                color = Color.White.copy(alpha = alphaVal),
                radius = radiusPx * scaleVal,
                center = center,
                style = Stroke(width = strokeWidthPx),
            )
        }
    }
}
