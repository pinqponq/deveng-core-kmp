package core.presentation.component.lazyswipecards

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.animation.core.calculateTargetValue
import androidx.compose.animation.splineBasedDecay
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.absoluteValue
import kotlin.math.cos
import kotlin.math.sin

/**
 * No [Modifier.composed]: avoids tying the top card’s pointer + size handling to composition when
 * [Animatable] updates every frame.
 */
internal fun Modifier.swipeListener(
    state: SwipeCardsState,
    rotateDegree: Float,
    translateSize: Dp,
    layoutDensity: Float,
    swipeThreshold: Float,
    minRatioBound: Float,
    animationSpec: AnimationSpec<Float>,
    onSwipe: (Float) -> Unit,
    /** Called on the apply thread after each batched [Animatable.snapTo] during drag (not during fling [animateTo]). */
    onDragOffsetApplied: () -> Unit,
): Modifier = this
    .onSizeChanged { sz ->
        val cx = sz.width / 2f
        val cy = sz.height / 2f
        val angleRad = rotateDegree * PI / 180.0
        val rotatedX = (-cx * cos(angleRad) + cy * sin(angleRad) + cx).toFloat()
        val padding = rotatedX.absoluteValue + translateSize.value.absoluteValue * layoutDensity
        state.swipeEdgePaddingPx = padding
        state.bound = sz.width + padding
    }
    .pointerInput(rotateDegree, translateSize.value, layoutDensity, swipeThreshold, minRatioBound) {
        val decay = splineBasedDecay<Float>(this)
        coroutineScope {
            while (true) {
                val velocity = awaitSwipe(
                    coroutineScope = this,
                    offsetX = state.offsetXAnimatable,
                    onDragOffsetApplied = onDragOffsetApplied,
                )
                val ratio = calculateRatio(
                    offsetX = state.offsetX,
                    width = state.viewportWidth,
                    swipeThreshold = swipeThreshold,
                ).absoluteValue
                state.bound = size.width.toFloat() + state.swipeEdgePaddingPx

                afterSwipe(
                    coroutineScope = this,
                    offsetX = state.offsetXAnimatable,
                    bound = state.bound,
                    velocity = velocity,
                    ratio = ratio,
                    minRatioBound = minRatioBound,
                    animationSpec = animationSpec,
                    decay = decay,
                    onSwipe = onSwipe,
                )
            }
        }
    }

/**
 * [horizontalDrag]'s lambda is a restricted suspend context and cannot call [Animatable.snapTo] directly.
 * Spawning [CoroutineScope.launch] per pointer event queues thousands of coroutines and stutters.
 * Instead, a single consumer applies deltas from a [Channel] in order.
 */
private suspend fun PointerInputScope.awaitSwipe(
    coroutineScope: CoroutineScope,
    offsetX: Animatable<Float, AnimationVector1D>,
    onDragOffsetApplied: () -> Unit,
): Float {
    offsetX.stop()
    val velocityTracker = VelocityTracker()
    val deltas = Channel<Float>(Channel.UNLIMITED)
    val applyJob = coroutineScope.launch {
        for (dx in deltas) {
            var sum = dx
            while (true) {
                val extra = deltas.tryReceive().getOrNull() ?: break
                sum += extra
            }
            offsetX.snapTo(offsetX.value + sum)
            onDragOffsetApplied()
        }
    }
    try {
        awaitPointerEventScope {
            val pointerId = awaitFirstDown().id
            horizontalDrag(pointerId) { change ->
                deltas.trySend(change.positionChange().x)
                velocityTracker.addPosition(
                    change.uptimeMillis,
                    change.position,
                )
            }
        }
    } finally {
        deltas.close()
        applyJob.join()
    }
    return velocityTracker.calculateVelocity().x
}

private fun afterSwipe(
    coroutineScope: CoroutineScope,
    offsetX: Animatable<Float, AnimationVector1D>,
    velocity: Float,
    ratio: Float,
    bound: Float,
    minRatioBound: Float,
    animationSpec: AnimationSpec<Float>,
    decay: DecayAnimationSpec<Float>,
    onSwipe: (Float) -> Unit,
) {
    val targetOffsetX = decay.calculateTargetValue(
        initialValue = offsetX.value,
        initialVelocity = velocity,
    )

    coroutineScope.launch {
        if (targetOffsetX.absoluteValue < bound) {
            if (ratio >= minRatioBound) {
                offsetX.animateTo(
                    targetValue = if (offsetX.value > 0) {
                        offsetX.upperBound!!
                    } else {
                        offsetX.lowerBound!!
                    },
                    animationSpec = animationSpec,
                    initialVelocity = velocity,
                )
            } else {
                offsetX.animateTo(
                    targetValue = 0f,
                    animationSpec = animationSpec,
                    initialVelocity = velocity,
                )
            }
        } else {
            offsetX.animateDecay(
                initialVelocity = velocity,
                animationSpec = decay,
            )
        }
    }.invokeOnCompletion { error ->
        coroutineScope.launch {
            if (error == null &&
                (targetOffsetX.absoluteValue >= bound ||
                        ratio >= minRatioBound)
            ) {
                onSwipe.invoke(targetOffsetX)
                offsetX.snapTo(0f)
            }
        }
    }
}
