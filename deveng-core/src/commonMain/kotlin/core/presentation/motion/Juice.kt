package core.presentation.motion

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

private const val REST_SCALE = 1f
private const val REST_ROTATION_DEGREES = 0f

/**
 * Drives a short grow-and-settle bounce ("pop") of an element. Call [pop] for a step, a selection
 * or a trigger, and apply the state with [juice]. A new [pop] cuts the running one short and
 * starts again from the current size. [pop] does nothing while reduce motion is on.
 */
class JuiceState internal constructor(
    private val coroutineScope: CoroutineScope,
    private val isReduceMotionEnabled: State<Boolean>,
    private val peakScale: Float,
    private val peakRotationDegrees: Float,
    private val riseMillis: Int,
    private val settleMillis: Int
) {
    internal val scale = Animatable(REST_SCALE)
    internal val rotationDegrees = Animatable(REST_ROTATION_DEGREES)
    private var popJob: Job? = null

    /** Plays one pop. */
    fun pop() {
        if (isReduceMotionEnabled.value) return
        popJob?.cancel()
        popJob = coroutineScope.launch { bounce(this) }
    }

    private suspend fun bounce(bounceScope: CoroutineScope) {
        val rotationJob = bounceScope.launch {
            rotationDegrees.animateTo(peakRotationDegrees, tween(riseMillis))
            rotationDegrees.animateTo(REST_ROTATION_DEGREES, tween(settleMillis))
        }
        scale.animateTo(peakScale, tween(riseMillis))
        scale.animateTo(REST_SCALE, tween(settleMillis))
        rotationJob.join()
    }
}

/**
 * Remembers a [JuiceState].
 *
 * @param peakScale Scale at the top of the pop.
 * @param peakRotationDegrees Rotation at the top of the pop. The default is no rotation.
 * @param riseMillis Time from rest to the top of the pop.
 * @param settleMillis Time from the top of the pop back to rest.
 */
@Composable
fun rememberJuiceState(
    peakScale: Float = MotionDefaults.POP_PEAK_SCALE,
    peakRotationDegrees: Float = REST_ROTATION_DEGREES,
    riseMillis: Int = MotionDefaults.POP_RISE_MILLIS,
    settleMillis: Int = MotionDefaults.POP_SETTLE_MILLIS
): JuiceState {
    val coroutineScope = rememberCoroutineScope()
    val isReduceMotionEnabled = rememberUpdatedState(LocalReduceMotion.current)
    return remember(peakScale, peakRotationDegrees, riseMillis, settleMillis) {
        JuiceState(
            coroutineScope = coroutineScope,
            isReduceMotionEnabled = isReduceMotionEnabled,
            peakScale = peakScale,
            peakRotationDegrees = peakRotationDegrees,
            riseMillis = riseMillis,
            settleMillis = settleMillis
        )
    }
}

/**
 * Applies the scale and rotation of [juiceState] to the element.
 *
 * @param juiceState State that [JuiceState.pop] is called on.
 */
fun Modifier.juice(juiceState: JuiceState): Modifier = graphicsLayer {
    scaleX = juiceState.scale.value
    scaleY = juiceState.scale.value
    rotationZ = juiceState.rotationDegrees.value
}
