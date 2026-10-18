package core.presentation.motion

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer

private const val REST_SCALE = 1f

/**
 * Scales the element down slightly while it is pressed. Pass the same [interactionSource] to the
 * element's `clickable`. Stays at full size while [LocalReduceMotion] is on.
 *
 * @param interactionSource Interaction source of the clickable this modifier gives feedback for.
 * @param pressedScale Scale while pressed. Defaults to [MotionTokens.PRESSED_SCALE].
 */
fun Modifier.pressScale(
    interactionSource: InteractionSource,
    pressedScale: Float = MotionTokens.PRESSED_SCALE
): Modifier = composed {
    val isPressed by interactionSource.collectIsPressedAsState()
    val targetScale = pressScaleTarget(
        isPressed = isPressed,
        isReduceMotionEnabled = LocalReduceMotion.current,
        pressedScale = pressedScale
    )
    val scale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = MotionTokens.pressSpring(),
        label = "pressScale"
    )

    graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

private fun pressScaleTarget(
    isPressed: Boolean,
    isReduceMotionEnabled: Boolean,
    pressedScale: Float
): Float {
    if (isReduceMotionEnabled || !isPressed) return REST_SCALE
    return pressedScale
}
