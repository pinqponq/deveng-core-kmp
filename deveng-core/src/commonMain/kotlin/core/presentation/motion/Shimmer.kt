package core.presentation.motion

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import core.presentation.theme.LocalComponentTheme

/**
 * Fills the element with a loading placeholder whose highlight sweeps from start to end.
 * While [LocalReduceMotion] is on the fill is the static base color.
 *
 * @param baseColor Placeholder color. If null, uses theme default.
 * @param highlightColor Color of the sweeping highlight. If null, uses theme default.
 */
fun Modifier.shimmer(
    baseColor: Color? = null,
    highlightColor: Color? = null
): Modifier = composed {
    val shimmerTheme = LocalComponentTheme.current.shimmer
    val finalBaseColor = baseColor ?: shimmerTheme.baseColor
    val finalHighlightColor = highlightColor ?: shimmerTheme.highlightColor

    if (LocalReduceMotion.current) return@composed background(color = finalBaseColor)

    val shimmerTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerProgress by shimmerTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = MotionTokens.SHIMMER_DURATION_MILLIS,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerProgress"
    )

    drawBehind {
        val highlightStartX = shimmerHighlightStartX(progress = shimmerProgress, width = size.width)
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(finalBaseColor, finalHighlightColor, finalBaseColor),
                start = Offset(x = highlightStartX, y = 0f),
                end = Offset(x = highlightStartX + size.width, y = size.height)
            )
        )
    }
}

private fun shimmerHighlightStartX(progress: Float, width: Float): Float =
    width * (2 * progress - 1)
