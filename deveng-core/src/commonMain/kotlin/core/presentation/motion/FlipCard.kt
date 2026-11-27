package core.presentation.motion

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity

private const val BACK_ROTATION_DEGREES = 0f
private const val FRONT_ROTATION_DEGREES = 180f
private const val HALF_TURN_DEGREES = 90f

/**
 * A card that turns around its vertical axis between two faces. The back shows while [isFlipped]
 * is false, the front once it is true. Each face is composed only while it is visible. While
 * reduce motion is on, the card swaps faces without turning.
 *
 * @param front Content of the face shown after the flip.
 * @param back Content of the face shown before the flip.
 * @param isFlipped Whether the front face is up.
 * @param modifier Modifier for the card.
 * @param durationMillis Time of one full turn.
 * @param delayMillis Time to wait before the turn starts. Use a small different value per card
 * (up to about 80 ms) so several cards do not move in lockstep.
 * @param cameraDistance Camera distance in density-independent units multiplied by the screen
 * density; a larger value gives a flatter turn.
 */
@Composable
fun FlipCard(
    front: @Composable () -> Unit,
    back: @Composable () -> Unit,
    isFlipped: Boolean,
    modifier: Modifier = Modifier,
    durationMillis: Int = MotionDefaults.FLIP_DURATION_MILLIS,
    delayMillis: Int = 0,
    cameraDistance: Float = MotionDefaults.FLIP_CAMERA_DISTANCE
) {
    val density = LocalDensity.current.density
    val rotationDegrees by animateFloatAsState(
        targetValue = if (isFlipped) FRONT_ROTATION_DEGREES else BACK_ROTATION_DEGREES,
        animationSpec = if (LocalReduceMotion.current) {
            snap()
        } else {
            tween(durationMillis = durationMillis, delayMillis = delayMillis, easing = FastOutSlowInEasing)
        },
        label = "flipCardRotation"
    )
    val isFrontFaceShown by remember { derivedStateOf { isFrontFaceVisible(rotationDegrees) } }

    Box(
        modifier = modifier.graphicsLayer {
            rotationY = rotationDegrees
            this.cameraDistance = cameraDistance * density
        }
    ) {
        if (isFrontFaceShown) {
            Box(modifier = Modifier.graphicsLayer { rotationY = FRONT_ROTATION_DEGREES }) {
                front()
            }
        } else {
            back()
        }
    }
}

internal fun isFrontFaceVisible(rotationDegrees: Float): Boolean = rotationDegrees > HALF_TURN_DEGREES
