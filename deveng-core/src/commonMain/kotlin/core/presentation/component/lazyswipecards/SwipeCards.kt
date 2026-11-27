package core.presentation.component.lazyswipecards

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.SpringSpec
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ScaleFactor
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import core.presentation.component.CustomIconButton
import core.presentation.theme.LocalComponentTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.DrawableResource
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.min

/**
 * A stacked card deck with horizontal swipe gestures and optional overlay action buttons.
 * Overlay buttons and swipe-direction highlight colors use [LocalComponentTheme] (swipeCards / SwipeCardsTheme).
 *
 * @param modifier Modifier for the outer container.
 * @param cardModifier Modifier applied to each card in the stack (after swipe transform).
 * @param cardShape Clip and shape for lightweight card mode; shape for [Surface] when using elevated card.
 * @param cardColor Background color of each card when using lightweight card mode.
 * @param cardContentColor Preferred content color for lightweight card mode.
 * @param cardTonalElevation Tonal elevation when using [Surface] for cards.
 * @param cardShadowElevation Shadow elevation when using [Surface] for cards.
 * @param cardBorder Optional border when using [Surface] for cards.
 * @param scaleFactor Scale applied per stack index for cards behind the front card.
 * @param translateSize Vertical offset per stack index for cards behind the front card.
 * @param rotateDegree Maximum rotation (degrees) of the front card at full swipe ratio.
 * @param stackTracksDragRatio When true, rear cards follow drag ratio for parallax; when false, static stack (better for heavy content).
 * @param visibleItemCount How many cards are composed in the stack.
 * @param contentPadding Padding around the swipe area (defaults from translate size and stack depth).
 * @param swipeThreshold Width fraction used with drag distance to compute swipe ratio (default 0.5).
 * @param minRatioBound Minimum absolute ratio to commit a swipe after release.
 * @param animationSpec Spring (or other) spec for programmatic swipe and snap-back animations.
 * @param isEndless When true, indices wrap; when false, swiping ends when the list is consumed.
 * @param state Swipe state; use [rememberSwipeCardsState].
 * @param showSwipeButtons When true and icons are provided, shows bottom overlay buttons for swipe/revert.
 * @param negativeButtonIcon Drawable for swipe-left action; null hides the button.
 * @param positiveButtonIcon Drawable for swipe-right action; null hides the button.
 * @param revertButtonIcon Drawable for undo last left-swipe; null hides the button.
 * @param negativeButtonContentDescription Accessibility label for the negative button.
 * @param positiveButtonContentDescription Accessibility label for the positive button.
 * @param revertButtonContentDescription Accessibility label for the revert button.
 * @param onAllItemsConsumed Called once when a non-endless list has no remaining cards after a swipe.
 * @param onSwipeLeft Called with the swiped item key after a committed left swipe; use stable keys from [SwipeCardsScope.items].
 * @param onSwipeRight Called with the swiped item key after a committed right swipe.
 * @param onRevert Called with the reverted item key after a successful revert animation.
 * @param pendingRevertKey When non-null, shows revert and wires it for external pending-undo flows; see [onRevert].
 * @param contentSeedKey When changed, forces item content to recompose (e.g. pass [pendingRevertKey]).
 * @param content DSL to declare items and optional [SwipeCardsScope.onSwiped] handler.
 */
@Composable
fun SwipeCards(
    modifier: Modifier = Modifier,
    cardModifier: Modifier = Modifier,
    cardShape: Shape = RectangleShape,
    cardColor: Color = MaterialTheme.colorScheme.surface,
    cardContentColor: Color = contentColorFor(cardColor),
    cardTonalElevation: Dp = 0.dp,
    cardShadowElevation: Dp = 2.dp,
    cardBorder: BorderStroke? = null,
    scaleFactor: ScaleFactor = ScaleFactor(
        scaleX = 0.1f,
        scaleY = 0.1f,
    ),
    translateSize: Dp = 24.dp,
    rotateDegree: Float = 14f,
    stackTracksDragRatio: Boolean = false,
    visibleItemCount: Int = 4,
    contentPadding: PaddingValues = defaultContentPadding(
        translateSize = translateSize,
        visibleItemCount = visibleItemCount,
    ),
    swipeThreshold: Float = 0.5f,
    minRatioBound: Float = MAX_RATIO,
    animationSpec: AnimationSpec<Float> = SpringSpec(),
    isEndless: Boolean = false,
    state: SwipeCardsState = rememberSwipeCardsState(),
    showSwipeButtons: Boolean = false,
    negativeButtonIcon: DrawableResource? = null,
    positiveButtonIcon: DrawableResource? = null,
    revertButtonIcon: DrawableResource? = null,
    negativeButtonContentDescription: String = "Swipe left",
    positiveButtonContentDescription: String = "Swipe right",
    revertButtonContentDescription: String = "Revert",
    onAllItemsConsumed: (() -> Unit)? = null,
    onSwipeLeft: ((Any?) -> Unit)? = null,
    onSwipeRight: ((Any?) -> Unit)? = null,
    onRevert: ((Any?) -> Unit)? = null,
    pendingRevertKey: Any? = null,
    contentSeedKey: Any? = null,
    content: SwipeCardsScope.() -> Unit,
) {
    val itemProvider = rememberSwipeCardsItemProvider(
        content = content,
        state = state,
        isEndless = isEndless,
        contentSeedKey = contentSeedKey,
        onAllItemsConsumed = onAllItemsConsumed,
        onSwipeLeft = onSwipeLeft,
        onSwipeRight = onSwipeRight,
    )
    // Peek / onSwiping: do NOT snapshotFlow on every drag frame (Animatable updates flood the main thread).
    // During finger drag, we sample only after each batched snapTo in SwipePointer. During animateTo/decay,
    // isAnimationRunning is true and a narrow snapshotFlow runs; when animation ends we sync once more.
    val swipeThrRef = rememberUpdatedState(swipeThreshold)
    val itemProviderRef = rememberUpdatedState(itemProvider)
    val onDragOffsetApplied = remember(state) {
        {
            applySwipePeekSample(state, swipeThrRef.value, itemProviderRef.value)
        }
    }
    LaunchedEffect(Unit) {
        var prevRunning = false
        snapshotFlow { state.isAnimationRunning }
            .collect { running ->
                if (prevRunning && !running) {
                    applySwipePeekSample(state, swipeThrRef.value, itemProviderRef.value)
                }
                prevRunning = running
            }
    }
    LaunchedEffect(Unit) {
        snapshotFlow { state.isAnimationRunning }
            .flatMapLatest { running ->
                if (running) {
                    snapshotFlow { state.offsetX to state.viewportWidth }
                        .distinctUntilChanged { (ox1, w1), (ox2, w2) ->
                            if (w1 != w2) return@distinctUntilChanged false
                            val thr = swipeThrRef.value
                            val r1 = calculateRatio(ox1, w1, thr)
                            val r2 = calculateRatio(ox2, w2, thr)
                            (r1 * 100f).toInt() == (r2 * 100f).toInt()
                        }
                } else {
                    emptyFlow()
                }
            }
            .collect { (ox, w) ->
                val thr = swipeThrRef.value
                val r = calculateRatio(ox, w, thr)
                state.syncRatioForCallbacks(r)
                itemProviderRef.value.onSwiping(ox, r)
            }
    }

    val scope = rememberCoroutineScope()
    val swipeCardsTheme = LocalComponentTheme.current.swipeCards
    val layoutDensity = LocalDensity.current.density
    val useLightweightCard =
        cardShadowElevation == 0.dp && cardTonalElevation == 0.dp && cardBorder == null
    Box(
        modifier = modifier
            .onSizeChanged(state::onSizeChanged)
            .padding(contentPadding),
        contentAlignment = Alignment.Center,
    ) {
        state.bind(
            isEndless = isEndless,
            visibleItemCount = visibleItemCount,
            itemProvider = itemProvider,
        ) { cardIndex, content ->
            val stackModifier = Modifier
                .swipe(
                    cardIndex = cardIndex,
                    state = state,
                    rotateDegree = rotateDegree,
                    minRatioBound = minRatioBound,
                    swipeThreshold = swipeThreshold,
                    scaleFactor = scaleFactor,
                    translateSize = translateSize,
                    visibleItemCount = visibleItemCount,
                    layoutDensity = layoutDensity,
                    animationSpec = animationSpec,
                    onSwipe = itemProvider::onSwiped,
                    onDragOffsetApplied = onDragOffsetApplied,
                    stackTracksDragRatio = stackTracksDragRatio,
                ) then cardModifier
            if (useLightweightCard) {
                CompositionLocalProvider(LocalContentColor provides cardContentColor) {
                    Box(
                        modifier = stackModifier
                            .clip(cardShape)
                            .background(cardColor, cardShape),
                    ) {
                        content()
                    }
                }
            } else {
                Surface(
                    modifier = stackModifier,
                    shape = cardShape,
                    color = cardColor,
                    contentColor = cardContentColor,
                    tonalElevation = cardTonalElevation,
                    shadowElevation = cardShadowElevation,
                    border = cardBorder,
                    content = content,
                )
            }
        }
        if (showSwipeButtons && (negativeButtonIcon != null || revertButtonIcon != null || positiveButtonIcon != null)) {
            val allConsumed = !isEndless && state.selectedItemIndex >= itemProvider.itemCount
            val leftHighlight = state.swipeNegativeButtonHighlight
            val rightHighlight = state.swipePositiveButtonHighlight
            val iconBlend =
                swipeCardsTheme.swipeHighlightIconTintBlend.coerceIn(0f, 1f)
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (negativeButtonIcon != null) {
                    if (!allConsumed) {
                        CustomIconButton(
                            buttonSize = swipeCardsTheme.buttonSize,
                            backgroundColor = lerp(
                                swipeCardsTheme.buttonBackgroundColor,
                                swipeCardsTheme.negativeSwipeHighlightColor,
                                leftHighlight,
                            ),
                            iconTint = lerp(
                                swipeCardsTheme.buttonIconTint,
                                Color.White,
                                leftHighlight * iconBlend,
                            ),
                            shadowElevation = swipeCardsTheme.buttonShadowElevation,
                            icon = negativeButtonIcon,
                            iconDescription = negativeButtonContentDescription,
                            onClick = {
                                state.lastSwipeDirection = SwipeDirection.LEFT
                                scope.launch {
                                    state.animateSwipe(SwipeDirection.LEFT)
                                }
                            }
                        )
                    } else {
                        Spacer(modifier = Modifier.size(swipeCardsTheme.buttonSize))
                    }
                }
                if (positiveButtonIcon != null) {
                    if (!allConsumed) {
                        CustomIconButton(
                            buttonSize = swipeCardsTheme.buttonSize,
                            backgroundColor = lerp(
                                swipeCardsTheme.buttonBackgroundColor,
                                swipeCardsTheme.positiveSwipeHighlightColor,
                                rightHighlight,
                            ),
                            iconTint = lerp(
                                swipeCardsTheme.buttonIconTint,
                                Color.White,
                                rightHighlight * iconBlend,
                            ),
                            shadowElevation = swipeCardsTheme.buttonShadowElevation,
                            icon = positiveButtonIcon,
                            iconDescription = positiveButtonContentDescription,
                            onClick = {
                                state.lastSwipeDirection = SwipeDirection.RIGHT
                                scope.launch {
                                    state.animateSwipe(SwipeDirection.RIGHT)
                                }
                            }
                        )
                    } else {
                        Spacer(modifier = Modifier.size(swipeCardsTheme.buttonSize))
                    }
                }
                if (revertButtonIcon != null) {
                    if (state.canRevert || pendingRevertKey != null) {
                        CustomIconButton(
                            buttonSize = swipeCardsTheme.buttonSize,
                            backgroundColor = swipeCardsTheme.buttonBackgroundColor,
                            iconTint = swipeCardsTheme.buttonIconTint,
                            shadowElevation = swipeCardsTheme.buttonShadowElevation,
                            icon = revertButtonIcon,
                            iconDescription = revertButtonContentDescription,
                            onClick = {
                                if (pendingRevertKey != null) {
                                    revertPendingSwipe(
                                        state = state,
                                        scope = scope,
                                        pendingRevertKey = pendingRevertKey,
                                        onRevert = onRevert,
                                    )
                                } else {
                                    val last = state.popLastSwipe() ?: return@CustomIconButton
                                    scope.launch {
                                        state.animateBackSwipe(last.second)
                                        onRevert?.invoke(last.first)
                                    }
                                }
                            }
                        )
                    } else {
                        Spacer(modifier = Modifier.size(swipeCardsTheme.buttonSize))
                    }
                }
            }
        }
    }
}

/**
 * Handles revert when the host provides [pendingRevertKey] (pending left-swipe not yet committed).
 * If the library has the swipe in history, animates it back; otherwise snaps offset and selected index
 * to 0 so the reverted card becomes the current card, then notifies host via [onRevert].
 */
private fun revertPendingSwipe(
    state: SwipeCardsState,
    scope: CoroutineScope,
    pendingRevertKey: Any?,
    onRevert: ((Any?) -> Unit)?,
) {
    val last = state.popLastSwipe()
    scope.launch {
        if (last != null) {
            state.animateBackSwipe(last.second)
        } else {
            state.offsetXAnimatable.snapTo(0f)
            state.snapTo(0)
        }
        onRevert?.invoke(pendingRevertKey)
    }
}

private fun applySwipePeekSample(
    state: SwipeCardsState,
    swipeThreshold: Float,
    itemProvider: SwipeCardsItemProvider,
) {
    val ox = state.offsetX
    val w = state.viewportWidth
    val r = calculateRatio(ox, w, swipeThreshold)
    state.syncRatioForCallbacks(r)
    itemProvider.onSwiping(ox, r)
}

private fun defaultContentPadding(
    translateSize: Dp,
    visibleItemCount: Int,
): PaddingValues {
    val absTranslateSize = translateSize.value.absoluteValue.dp
    return PaddingValues(
        vertical = absTranslateSize * visibleItemCount,
        horizontal = absTranslateSize,
    )
}

internal fun calculateRatio(
    offsetX: Float,
    width: Int,
    swipeThreshold: Float,
): Float {
    return min(
        MAX_RATIO, max(
            -MAX_RATIO,
            offsetX / (width * swipeThreshold)
        )
    )
}

private const val MAX_RATIO = 1f
