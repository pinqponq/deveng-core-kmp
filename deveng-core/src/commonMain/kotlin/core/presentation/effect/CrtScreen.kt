package core.presentation.effect

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import core.presentation.motion.LocalReduceMotion

private const val NOISE_LOOP_MILLIS = 60_000
private const val NOISE_TIME_OFFSET = 400f
private const val NOISE_TIME_LOOP = 1000f
private const val FALLBACK_LINE_STEP_PX = 4f
private const val FALLBACK_LINE_ALPHA = 0.07f
private const val FALLBACK_VIGNETTE_ALPHA = 0.35f

/**
 * Look of [crtScreen]. The defaults are Balatro's at its default CRT setting.
 *
 * @param intensity Strength of scanlines, color fringing and darkening (0 turns the look off).
 * @param noiseAmount Strength of the flickering grain.
 * @param bloomAmount Strength of the soft glow around bright areas.
 * @param scanlinesPerHeight Scanline period count per pixel of height.
 * @param vignette Darkening towards the corners.
 */
@Immutable
data class CrtConfig(
    val intensity: Float = 0.0336f,
    val noiseAmount: Float = 0.00021f,
    val bloomAmount: Float = 1f,
    val scanlinesPerHeight: Float = 0.75f,
    val vignette: Float = 0.25f
)

/**
 * Gives the content an old TV look: barrel curvature, scanlines, color fringing, grain, bloom and a vignette.
 * The content must paint an opaque background. While [LocalReduceMotion] is on the grain stays still.
 * Where runtime shaders are unavailable (Android below 13) it draws plain scanlines and a vignette instead.
 */
@Composable
fun Modifier.crtScreen(config: CrtConfig = CrtConfig()): Modifier {
    if (!isRuntimeShaderSupported()) return this.crtFallback()

    val program = remember { RuntimeShaderProgram(CRT_SHADER) }
    val noiseTime = rememberNoiseTime()
    return this.graphicsLayer {
        program.setFloats("resolution", size.width, size.height)
        program.setFloats("time", noiseTime.value)
        program.setFloats("intensity", config.intensity)
        program.setFloats("noiseAmount", config.noiseAmount)
        program.setFloats("bloomAmount", config.bloomAmount)
        program.setFloats("scanlineCount", size.height * config.scanlinesPerHeight)
        program.setFloats("vignette", config.vignette)
        renderEffect = program.toRenderEffect(contentName = "content")
    }
}

@Composable
private fun rememberNoiseTime(): State<Float> {
    if (LocalReduceMotion.current) return remember { mutableStateOf(NOISE_TIME_OFFSET) }
    val transition = rememberInfiniteTransition(label = "crtNoise")
    return transition.animateFloat(
        initialValue = NOISE_TIME_OFFSET,
        targetValue = NOISE_TIME_OFFSET + NOISE_TIME_LOOP,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = NOISE_LOOP_MILLIS, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "crtNoiseTime"
    )
}

private fun Modifier.crtFallback(): Modifier = drawWithContent {
    drawContent()
    var y = 0f
    while (y < size.height) {
        drawLine(
            color = Color.Black.copy(alpha = FALLBACK_LINE_ALPHA),
            start = Offset(x = 0f, y = y),
            end = Offset(x = size.width, y = y),
            strokeWidth = 1f
        )
        y += FALLBACK_LINE_STEP_PX
    }
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(Color.Transparent, Color.Black.copy(alpha = FALLBACK_VIGNETTE_ALPHA)),
            center = center,
            radius = size.maxDimension * 0.75f
        )
    )
}
