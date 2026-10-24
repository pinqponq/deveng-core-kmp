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
private const val VORTEX_TIME_FACTOR = 6f
private const val VORTEX_SPIN_FACTOR = 40f
private const val VORTEX_FALLBACK_ALPHA = 0.6f

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
    if (!LocalRuntimeShaderEnabled.current) {
        SwirlFallback(palette = palette, modifier = modifier)
        return
    }

    val program = remember { RuntimeShaderProgram(SWIRL_SHADER) }
    val time = rememberSwirlTime()
    Canvas(modifier = modifier.fillMaxSize()) {
        program.apply(
            uniforms = swirlUniforms(
                palette = palette,
                width = size.width,
                height = size.height,
                time = time.value,
                spinTime = time.value,
                spinAmount = spinAmount,
                contrast = contrast
            )
        )
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
    val strength = vortexStrength(progress = progress)
    if (strength <= 0f) return

    if (!LocalRuntimeShaderEnabled.current) {
        Canvas(modifier = modifier.fillMaxSize()) {
            drawRect(color = palette.accent.copy(alpha = strength * VORTEX_FALLBACK_ALPHA))
        }
        return
    }

    val program = remember { RuntimeShaderProgram(SWIRL_SHADER) }
    Canvas(modifier = modifier.fillMaxSize()) {
        program.apply(
            uniforms = swirlUniforms(
                palette = palette,
                width = size.width,
                height = size.height,
                time = progress * VORTEX_TIME_FACTOR,
                spinTime = progress * VORTEX_SPIN_FACTOR,
                spinAmount = 1f,
                contrast = 1f
            )
        )
        drawRect(brush = program.toBrush(), alpha = strength)
    }
}

internal fun vortexStrength(progress: Float): Float = sin(progress.coerceIn(0f, 1f) * PI.toFloat())

internal fun swirlUniforms(
    palette: SwirlPalette,
    width: Float,
    height: Float,
    time: Float,
    spinTime: Float,
    spinAmount: Float,
    contrast: Float
): List<ShaderUniform> = listOf(
    ShaderUniform(name = "resolution", values = floatArrayOf(width, height)),
    ShaderUniform(name = "time", values = floatArrayOf(time)),
    ShaderUniform(name = "spinTime", values = floatArrayOf(spinTime)),
    ShaderUniform(name = "spinAmount", values = floatArrayOf(spinAmount)),
    ShaderUniform(name = "contrast", values = floatArrayOf(contrast)),
    ShaderUniform(name = "colour1", values = palette.base.toFloats()),
    ShaderUniform(name = "colour2", values = palette.accent.toFloats()),
    ShaderUniform(name = "colour3", values = palette.shade.toFloats())
)

private fun Color.toFloats(): FloatArray = floatArrayOf(red, green, blue, alpha)

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
