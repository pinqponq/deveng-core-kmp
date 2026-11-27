package core.presentation.component.lazyswipecards

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ScaleFactor
import androidx.compose.ui.unit.Dp
import kotlin.math.absoluteValue
import kotlin.math.sign

internal fun Modifier.swipe(
    cardIndex: Int,
    state: SwipeCardsState,
    rotateDegree: Float,
    minRatioBound: Float,
    scaleFactor: ScaleFactor,
    translateSize: Dp,
    visibleItemCount: Int,
    swipeThreshold: Float,
    layoutDensity: Float,
    animationSpec: AnimationSpec<Float>,
    onSwipe: (Float) -> Unit,
    onDragOffsetApplied: () -> Unit,
    /**
     * When false, rear cards do not read [SwipeCardsState.offsetX] in [graphicsLayer] (static stack while dragging).
     * [SwipeCards] defaults this to false for performance on heavy stacks; set true for parallax.
     */
    stackTracksDragRatio: Boolean,
): Modifier {
    return if (cardIndex == 0) {
        swipeListener(
            state = state,
            rotateDegree = rotateDegree,
            translateSize = translateSize,
            layoutDensity = layoutDensity,
            swipeThreshold = swipeThreshold,
            minRatioBound = minRatioBound,
            animationSpec = animationSpec,
            onSwipe = onSwipe,
            onDragOffsetApplied = onDragOffsetApplied,
        ).graphicsLayer {
            val ratio = calculateRatio(
                offsetX = state.offsetX,
                width = state.viewportWidth,
                swipeThreshold = swipeThreshold,
            )
            translationX = state.offsetX
            rotationZ = rotateDegree * ratio
        }
    } else {
        applyTransformation(
            cardIndex = cardIndex,
            state = state,
            swipeThreshold = swipeThreshold,
            scaleFactor = scaleFactor,
            translateSize = translateSize,
            visibleItemCount = visibleItemCount,
            stackTracksDragRatio = stackTracksDragRatio,
        )
    }
}

/**
 * No [Modifier.composed] here: pairing [composed] with [graphicsLayer] that reads [SwipeCardsState.offsetX]
 * each frame can pull the stack into recomposition/measure work. Stack Y uses [SwipeCardsState.viewportHeight]
 * (same as card height when cards are full-bleed in the padded area).
 */
private fun Modifier.applyTransformation(
    cardIndex: Int,
    state: SwipeCardsState,
    swipeThreshold: Float,
    scaleFactor: ScaleFactor,
    translateSize: Dp,
    visibleItemCount: Int,
    stackTracksDragRatio: Boolean,
): Modifier = this.graphicsLayer {
    val density = this.density
    val ratioForStack = if (stackTracksDragRatio) {
        calculateRatio(
            offsetX = state.offsetX,
            width = state.viewportWidth,
            swipeThreshold = swipeThreshold,
        ).absoluteValue
    } else {
        0f
    }
    val indexWithRatio = if (cardIndex == visibleItemCount) {
        visibleItemCount - 1f
    } else {
        cardIndex - ratioForStack
    }
    val scaleY = 1f - indexWithRatio * scaleFactor.scaleY
    val scaleX = 1f - indexWithRatio * scaleFactor.scaleX
    val defY = indexWithRatio * translateSize.value * density
    val itemHeight = state.viewportHeight.toFloat()
    val scaleDiffInY = sign(translateSize.value) *
        (itemHeight * (1f - scaleY)) / 2f
    this.scaleX = scaleX
    this.scaleY = scaleY
    translationY = defY + scaleDiffInY
}
