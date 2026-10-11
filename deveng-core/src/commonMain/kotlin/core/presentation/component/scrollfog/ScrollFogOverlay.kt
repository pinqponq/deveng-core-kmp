package core.presentation.component.scrollfog

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private const val FOG_FADE_DURATION_MILLIS = 250
private val FogAlphaStops = listOf(1f, 0.6f, 0.4f, 0.2f, 0f)

/**
 * Fog that fades content out under the top edge of a scrolling area.
 *
 * @param isVisible whether the fog is shown; changes fade in and out.
 * @param modifier modifier applied to the fog box.
 * @param height height of the fog.
 * @param color fog color, usually the color of the surface behind the scrolling content.
 */
@Composable
fun BoxScope.ScrollFogOverlay(
    isVisible: Boolean,
    modifier: Modifier = Modifier,
    height: Dp = 72.dp,
    color: Color = MaterialTheme.colorScheme.background,
) {
    val fogAlpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(durationMillis = FOG_FADE_DURATION_MILLIS),
        label = "scroll_fog_alpha",
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .align(Alignment.TopCenter)
            .alpha(fogAlpha)
            .background(
                brush = Brush.verticalGradient(
                    colors = FogAlphaStops.map { stopAlpha -> color.copy(alpha = stopAlpha) },
                ),
            ),
    )
}
