package core.presentation.motion

import androidx.compose.animation.core.animate
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import core.presentation.haptics.AppHaptics
import core.presentation.haptics.rememberAppHaptics
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.pow

private const val REST_OFFSET = 0f
private const val HORIZONTAL_GATE_RATIO = 3f

/** The edge a swipe to reply starts from. Both follow the layout direction, so right-to-left layouts are mirrored. */
enum class SwipeToReplyDirection {
    StartToEnd,
    EndToStart
}

/**
 * Holds how far an element is swiped by [swipeToReply]. Read [offset], [triggerProgress] or
 * [isPastTrigger] to draw a reply icon next to the element.
 *
 * The element follows the finger until the trigger distance. Past it the drag gets harder and
 * stops at the maximum distance. On release the element returns to its place, at once while
 * reduce motion is on.
 */
@Stable
class SwipeToReplyState internal constructor(
    private val coroutineScope: CoroutineScope,
    private val isReduceMotionEnabled: State<Boolean>,
    private val triggerDistancePx: Float,
    private val maxDistancePx: Float,
    private val resistanceExponent: Float,
    private val returnMillis: Int
) {
    /** Distance in pixels the element is moved in the swipe direction, from 0 to the maximum distance. */
    var offset by mutableFloatStateOf(REST_OFFSET)
        private set

    /** [offset] divided by the trigger distance, from 0 to 1. */
    val triggerProgress: Float
        get() = (offset / triggerDistancePx).coerceIn(0f, 1f)

    /** True while a release would call `onTrigger`. */
    val isPastTrigger: Boolean
        get() = isTriggerReached(offset = offset, triggerDistance = triggerDistancePx)

    private var returnJob: Job? = null

    internal fun startDrag() {
        returnJob?.cancel()
    }

    /** Moves the element by [dragDelta] pixels and returns true when this move crossed the trigger distance. */
    internal fun dragBy(dragDelta: Float): Boolean {
        val wasPastTrigger = isPastTrigger
        offset = swipeToReplyOffset(
            currentOffset = offset,
            dragDelta = dragDelta,
            triggerDistance = triggerDistancePx,
            maxDistance = maxDistancePx,
            resistanceExponent = resistanceExponent
        )
        return !wasPastTrigger && isPastTrigger
    }

    internal fun returnToRest() {
        returnJob?.cancel()
        returnJob = coroutineScope.launch {
            animate(
                initialValue = offset,
                targetValue = REST_OFFSET,
                animationSpec = MotionTokens.tweenOrSnap(
                    isReduceMotionEnabled = isReduceMotionEnabled.value,
                    durationMillis = returnMillis,
                    easing = MotionTokens.StandardDecelerateEasing
                )
            ) { animatedOffset, _ -> offset = animatedOffset }
        }
    }
}

/**
 * Remembers a [SwipeToReplyState].
 *
 * @param triggerDistance Distance the element must be moved for a release to call `onTrigger`.
 * @param maxDistance Largest distance the element can be moved. Must be larger than [triggerDistance].
 * @param resistanceExponent How fast the drag gets harder between [triggerDistance] and [maxDistance].
 * A larger value makes it harder sooner.
 * @param returnMillis Time the element takes to return to its place after release.
 */
@Composable
fun rememberSwipeToReplyState(
    triggerDistance: Dp = MotionDefaults.SWIPE_TO_REPLY_TRIGGER_DISTANCE_DP.dp,
    maxDistance: Dp = MotionDefaults.SWIPE_TO_REPLY_MAX_DISTANCE_DP.dp,
    resistanceExponent: Float = MotionDefaults.SWIPE_TO_REPLY_RESISTANCE_EXPONENT,
    returnMillis: Int = MotionDefaults.SWIPE_TO_REPLY_RETURN_MILLIS
): SwipeToReplyState {
    require(maxDistance > triggerDistance) { "maxDistance must be larger than triggerDistance" }
    val coroutineScope = rememberCoroutineScope()
    val isReduceMotionEnabled = rememberUpdatedState(LocalReduceMotion.current)
    val density = LocalDensity.current
    return remember(density, triggerDistance, maxDistance, resistanceExponent, returnMillis) {
        SwipeToReplyState(
            coroutineScope = coroutineScope,
            isReduceMotionEnabled = isReduceMotionEnabled,
            triggerDistancePx = with(density) { triggerDistance.toPx() },
            maxDistancePx = with(density) { maxDistance.toPx() },
            resistanceExponent = resistanceExponent,
            returnMillis = returnMillis
        )
    }
}

/**
 * Lets the user swipe the element sideways to reply, as in chat apps. The swipe starts only when
 * the finger moves from [direction]'s start edge and mostly sideways (the sideways distance divided
 * by 3 is larger than the up or down distance), so a scrolling list around the element still
 * scrolls. [AppHaptics.tap] plays when the element crosses the trigger distance. On release past
 * the trigger distance [onTrigger] is called, and the element always returns to its place.
 *
 * @param state State from [rememberSwipeToReplyState]; it also holds the distances.
 * @param onTrigger Called once per swipe that is released past the trigger distance.
 * @param direction Edge the swipe starts from.
 * @param enabled When false, the element ignores swipes.
 */
fun Modifier.swipeToReply(
    state: SwipeToReplyState,
    onTrigger: () -> Unit,
    direction: SwipeToReplyDirection = SwipeToReplyDirection.StartToEnd,
    enabled: Boolean = true
): Modifier = composed {
    val haptics = rememberAppHaptics()
    val currentOnTrigger by rememberUpdatedState(onTrigger)
    val directionSign = swipeToReplyDirectionSign(direction = direction, layoutDirection = LocalLayoutDirection.current)

    Modifier
        .pointerInput(state, directionSign, enabled, haptics) {
            if (!enabled) return@pointerInput
            detectSwipeToReply(
                state = state,
                directionSign = directionSign,
                onTriggerCrossed = haptics::tap,
                onTrigger = { currentOnTrigger() }
            )
        }
        .graphicsLayer { translationX = state.offset * directionSign }
}

private suspend fun PointerInputScope.detectSwipeToReply(
    state: SwipeToReplyState,
    directionSign: Float,
    onTriggerCrossed: () -> Unit,
    onTrigger: () -> Unit
) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        val swipeStartChange = awaitSwipeStart(pointerId = down.id, directionSign = directionSign) ?: return@awaitEachGesture
        state.startDrag()
        val isReleased = horizontalDrag(swipeStartChange.id) { change ->
            val hasCrossedTrigger = state.dragBy(change.positionChange().x * directionSign)
            if (hasCrossedTrigger) onTriggerCrossed()
            change.consume()
        }
        if (isReleased && state.isPastTrigger) onTrigger()
        state.returnToRest()
    }
}

private suspend fun AwaitPointerEventScope.awaitSwipeStart(pointerId: PointerId, directionSign: Float): PointerInputChange? {
    var totalDrag = Offset.Zero
    while (true) {
        val change = awaitPointerEvent().changes.firstOrNull { it.id == pointerId } ?: return null
        if (!change.pressed || change.isConsumed) return null
        totalDrag += change.positionChange()
        if (totalDrag.getDistance() < viewConfiguration.touchSlop) continue
        val isSwipe = isSwipeToReplyStart(
            directionalDistance = totalDrag.x * directionSign,
            verticalDistance = totalDrag.y
        )
        if (!isSwipe) return null
        change.consume()
        return change
    }
}

internal fun swipeToReplyDirectionSign(direction: SwipeToReplyDirection, layoutDirection: LayoutDirection): Float {
    val isStartToEnd = direction == SwipeToReplyDirection.StartToEnd
    val isLeftToRight = layoutDirection == LayoutDirection.Ltr
    return if (isStartToEnd == isLeftToRight) 1f else -1f
}

internal fun isSwipeToReplyStart(directionalDistance: Float, verticalDistance: Float): Boolean =
    directionalDistance / HORIZONTAL_GATE_RATIO > abs(verticalDistance)

internal fun isTriggerReached(offset: Float, triggerDistance: Float): Boolean = offset >= triggerDistance

internal fun swipeToReplyOffset(
    currentOffset: Float,
    dragDelta: Float,
    triggerDistance: Float,
    maxDistance: Float,
    resistanceExponent: Float
): Float {
    if (dragDelta <= 0f) return (currentOffset + dragDelta).coerceAtLeast(REST_OFFSET)
    val freeDelta = (triggerDistance - currentOffset).coerceIn(0f, dragDelta)
    val resistedStartOffset = currentOffset + freeDelta
    val resistedDelta = dragDelta - freeDelta
    if (resistedDelta == 0f) return resistedStartOffset
    val remainingFraction = 1f - (resistedStartOffset - triggerDistance) / (maxDistance - triggerDistance)
    val resistance = remainingFraction.coerceIn(0f, 1f).pow(resistanceExponent)
    return (resistedStartOffset + resistedDelta * resistance).coerceAtMost(maxDistance)
}
