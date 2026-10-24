package core.presentation.effect

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import core.presentation.motion.LocalReduceMotion
import kotlin.math.PI
import kotlin.math.sin

private const val SWIRL_LOOP_MILLIS = 600_000
private const val SWIRL_LOOP_SECONDS = 600f
private const val FALLBACK_LOOP_MILLIS = 12_000

/**
 * The three colors of [SwirlBackground]: [base] fills most of the picture, [accent] and [shade] mark the paint swirls.
 */
@Immutable
data class SwirlPalette(
    val base: Color,
    val accent: Color,
    val shade: Color
)

/**
 * A slowly moving, pixelated paint swirl in the three [palette] colors.
 * While [LocalReduceMotion] is on the picture stands still.
 *
 * @param spinAmount How strongly the swirl winds around the center (0 to 1).
 * @param contrast Contrast of the paint (1 is the default look).
 */
@Composable
fun SwirlBackground(
    palette: SwirlPalette,
    modifier: Modifier = Modifier,
    spinAmount: Float = 0f,
    contrast: Float = 1f
) {
    if (!isRuntimeShaderSupported()) {
        SwirlFallback(palette = palette, modifier = modifier)
        return
    }

    val program = remember { RuntimeShaderProgram(SWIRL_SHADER) }
    val time = rememberSwirlTime()
    Canvas(modifier = modifier.fillMaxSize()) {
        program.setFloats("resolution", size.width, size.height)
        program.setFloats("time", time.value)
        program.setFloats("spinTime", time.value)
        program.setFloats("spinAmount", spinAmount)
        program.setFloats("contrast", contrast)
        program.setFloats("colour1", palette.base.red, palette.base.green, palette.base.blue, palette.base.alpha)
        program.setFloats("colour2", palette.accent.red, palette.accent.green, palette.accent.blue, palette.accent.alpha)
        program.setFloats("colour3", palette.shade.red, palette.shade.green, palette.shade.blue, palette.shade.alpha)
        drawRect(brush = program.toBrush())
    }
}

/**
 * A short full-screen vortex and flash for changing between two stages.
 * [progress] goes 0 to 1; the effect peaks halfway and is invisible at both ends.
 */
@Composable
fun VortexFlash(
    progress: Float,
    palette: SwirlPalette,
    modifier: Modifier = Modifier
) {
    val strength = sin(progress.coerceIn(0f, 1f) * PI.toFloat())
    if (strength <= 0f) return

    if (!isRuntimeShaderSupported()) {
        Canvas(modifier = modifier.fillMaxSize()) {
            drawRect(color = palette.accent.copy(alpha = strength * 0.6f))
        }
        return
    }

    val program = remember { RuntimeShaderProgram(SWIRL_SHADER) }
    Canvas(modifier = modifier.fillMaxSize()) {
        program.setFloats("resolution", size.width, size.height)
        program.setFloats("time", progress * 6f)
        program.setFloats("spinTime", progress * 40f)
        program.setFloats("spinAmount", 1f)
        program.setFloats("contrast", 1f)
        program.setFloats("colour1", palette.base.red, palette.base.green, palette.base.blue, 1f)
        program.setFloats("colour2", palette.accent.red, palette.accent.green, palette.accent.blue, 1f)
        program.setFloats("colour3", palette.shade.red, palette.shade.green, palette.shade.blue, 1f)
        drawRect(brush = program.toBrush(), alpha = strength)
    }
}

@Composable
private fun rememberSwirlTime(): State<Float> {
    if (LocalReduceMotion.current) return remember { mutableStateOf(0f) }
    val transition = rememberInfiniteTransition(label = "swirl")
    return transition.animateFloat(
        initialValue = 0f,
        targetValue = SWIRL_LOOP_SECONDS,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = SWIRL_LOOP_MILLIS, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "swirlTime"
    )
}

@Composable
private fun SwirlFallback(palette: SwirlPalette, modifier: Modifier) {
    val phase = if (LocalReduceMotion.current) {
        remember { mutableStateOf(0.5f) }
    } else {
        rememberInfiniteTransition(label = "swirlFallback").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = FALLBACK_LOOP_MILLIS, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "swirlFallbackPhase"
        )
    }
    Canvas(modifier = modifier.fillMaxSize()) {
        drawRect(color = palette.base)
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(palette.accent.copy(alpha = 0.55f), Color.Transparent),
                center = Offset(x = size.width * (0.25f + 0.5f * phase.value), y = size.height * 0.35f),
                radius = size.maxDimension * 0.7f
            )
        )
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(palette.shade.copy(alpha = 0.5f), Color.Transparent),
                center = Offset(x = size.width * (0.75f - 0.5f * phase.value), y = size.height * 0.75f),
                radius = size.maxDimension * 0.7f
            )
        )
    }
}
