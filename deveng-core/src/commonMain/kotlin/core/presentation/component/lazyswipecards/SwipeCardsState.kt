package core.presentation.component.lazyswipecards

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.SpringSpec
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.unit.IntSize
import kotlin.math.max
import kotlin.math.min

@Stable
class SwipeCardsState(
    initialSelectedItemIndex: Int = 0,
) {

    internal var itemProvider: SwipeCardsItemProvider? = null

    private val _selectedIndex = mutableIntStateOf(initialSelectedItemIndex)
    val selectedItemIndex: Int get() = _selectedIndex.value

    internal val offsetXAnimatable = Animatable(0f)
    val offsetX: Float get() = offsetXAnimatable.value
    val isAnimationRunning: Boolean get() = offsetXAnimatable.isRunning

    private val _width = mutableIntStateOf(1)
    val viewportWidth: Int get() = _width.value

    private val _height = mutableIntStateOf(1)
    /** Outer SwipeCards content area height (from [onSizeChanged]); used for back-card stack math without per-card [composed]. */
    val viewportHeight: Int get() = _height.value

    internal var bound: Float = 0f
        get() = max(viewportWidth.toFloat(), field)
        set(value) {
            field = value
            updateBounds()
        }

    /** Extra horizontal bound for swipe decay; updated from top card [onSizeChanged] (not in [composed]). */
    internal var swipeEdgePaddingPx: Float = 0f

    var ratio: Float = 0f
        private set

    private val negativeButtonHighlightState = mutableFloatStateOf(0f)
    private val positiveButtonHighlightState = mutableFloatStateOf(0f)

    /**
     * 0..1 while the user drags or animates toward swipe left; drives button color fade-in.
     */
    val swipeNegativeButtonHighlight: Float get() = negativeButtonHighlightState.floatValue

    /**
     * 0..1 while the user drags or animates toward swipe right; drives button color fade-in.
     */
    val swipePositiveButtonHighlight: Float get() = positiveButtonHighlightState.floatValue

    /**
     * True while [ratio] is past half toward the next card on a positive (right) drag.
     * Only toggles when the condition crosses the threshold so UI can react without per-frame recomposition.
     */
    private val peekNextCardForPlaybackState = mutableStateOf(false)
    val peekNextCardForPlayback: Boolean get() = peekNextCardForPlaybackState.value

    val swipingDirection: SwipeDirection
        get() = getSwipeDirection(offsetX)

    /**
     * Last direction the top card was swiped (by gesture or programmatic swipe).
     * Use for [animateBackSwipe] so the previous card returns from the correct side.
     */
    var lastSwipeDirection: SwipeDirection = SwipeDirection.RIGHT

    /**
     * Key of the last swiped card. Set when a card is swiped; used for [onRevert] callback.
     */
    var lastSwipedKey: Any? = null

    /** History of swipes (key, direction). Revert only allowed when last swipe was LEFT. */
    private val swipeHistory = mutableListOf<Pair<Any?, SwipeDirection>>()

    /** True when the last swipe was LEFT so revert can undo that card. */
    val canRevert: Boolean
        get() = swipeHistory.isNotEmpty() && swipeHistory.last().second == SwipeDirection.LEFT

    /** Pops the last swipe (key, direction). Call only when [canRevert] is true. */
    internal fun popLastSwipe(): Pair<Any?, SwipeDirection>? = swipeHistory.removeLastOrNull()

    /** Records a swipe for revert history. */
    internal fun pushSwipe(key: Any?, direction: SwipeDirection) {
        lastSwipedKey = key
        lastSwipeDirection = direction
        swipeHistory.add(key to direction)
    }

    fun snapTo(index: Int) {
        _selectedIndex.value = index
    }

    suspend fun animateSwipe(
        direction: SwipeDirection,
        animationSpec: AnimationSpec<Float> = SpringSpec(),
        initialVelocity: Float = 0f,
    ) {
        updateBounds()
        val targetOffsetX = if (direction == SwipeDirection.RIGHT) {
            offsetXAnimatable.upperBound!!
        } else {
            offsetXAnimatable.lowerBound!!
        }
        offsetXAnimatable.animateTo(
            targetValue = targetOffsetX,
            animationSpec = animationSpec,
            initialVelocity = initialVelocity,
        )
        itemProvider?.onSwiped(targetOffsetX)
        offsetXAnimatable.snapTo(0f)
    }

    suspend fun animateBackSwipe(
        direction: SwipeDirection,
        animationSpec: AnimationSpec<Float> = SpringSpec(),
        initialVelocity: Float = 0f,
    ) {
        if (selectedItemIndex == 0) {
            return
        }
        _selectedIndex.value--

        updateBounds()
        val initialOffsetX = if (direction == SwipeDirection.RIGHT) {
            offsetXAnimatable.upperBound!!
        } else {
            offsetXAnimatable.lowerBound!!
        }
        offsetXAnimatable.snapTo(initialOffsetX)
        offsetXAnimatable.animateTo(
            targetValue = 0f,
            animationSpec = animationSpec,
            initialVelocity = initialVelocity,
        )
    }

    /**
     * Updates [ratio], [peekNextCardForPlayback], and drives onSwiping; call from [SwipeCards] snapshot flow only.
     * [peekNextCardForPlayback] uses hysteresis so a ratio wobbling around 0.5 does not flip every frame.
     * That avoids full-stack recomposes and host work (e.g. video play/pause thrashing) during drag.
     */
    internal fun syncRatioForCallbacks(ratio: Float) {
        this.ratio = ratio
        val clamped = ratio.coerceIn(-1f, 1f)
        negativeButtonHighlightState.floatValue = if (clamped < 0f) (-clamped).coerceIn(0f, 1f) else 0f
        positiveButtonHighlightState.floatValue = if (clamped > 0f) clamped.coerceIn(0f, 1f) else 0f
        val prev = peekNextCardForPlaybackState.value
        val peek = when {
            ratio >= PeekOnRatio -> true
            ratio <= PeekOffRatio -> false
            else -> prev
        }
        if (peek != prev) {
            peekNextCardForPlaybackState.value = peek
        }
    }

    internal fun selectNextItem() {
        _selectedIndex.value++
    }

    internal fun onSizeChanged(newSize: IntSize) {
        _width.value = max(1, newSize.width)
        _height.value = max(1, newSize.height)
    }

    private fun updateBounds() {
        offsetXAnimatable.updateBounds(
            lowerBound = -bound,
            upperBound = bound,
        )
    }

    companion object {
        private const val PeekOnRatio = 0.52f
        private const val PeekOffRatio = 0.48f

        val Saver: Saver<SwipeCardsState, *> = Saver(
            save = { it.selectedItemIndex },
            restore = { SwipeCardsState(it) },
        )
    }
}

@Composable
internal fun SwipeCardsState.bind(
    isEndless: Boolean,
    visibleItemCount: Int,
    itemProvider: SwipeCardsItemProvider,
    cardComposable: @Composable (cardIndex: Int, content: @Composable () -> Unit) -> Unit,
) {
    DisposableEffect(itemProvider) {
        this@bind.itemProvider = itemProvider
        onDispose {
            this@bind.itemProvider = null
        }
    }

    val selectedIndex = selectedItemIndex
    val maxStackCards = (
        if (isEndless) {
            min(visibleItemCount, itemProvider.itemCount)
        } else {
            min(visibleItemCount, itemProvider.itemCount - selectedIndex)
        }
    ).coerceAtLeast(0)
    for (localIndex in maxStackCards - 1 downTo 0) {
        cardComposable.invoke(localIndex) {
            val itemIndex = if (isEndless) {
                (selectedIndex + localIndex) % itemProvider.itemCount
            } else {
                selectedIndex + localIndex
            }
            itemProvider.Item(itemIndex, itemProvider.getKey(itemIndex))
        }
    }
}

/**
 * Creates and remembers [SwipeCardsState] for use with [SwipeCards].
 *
 * @param initialSelectedItemIndex Used when the state is first created (e.g. when [rememberKey] changes).
 * @param rememberKey When non-null, the state is kept with [remember] keyed by this value (as string)
 *        so the same state instance is preserved across recompositions and the library's internal
 *        updates (e.g. selectedItemIndex after swipe) are kept. Use the same key as the parent
 *        key(swipeCardsKey) so that setting pending does not recreate state. When null, uses
 *        [rememberSaveable] keyed by [initialSelectedItemIndex] for process-death restore.
 */
@Composable
fun rememberSwipeCardsState(
    initialSelectedItemIndex: Int = 0,
    rememberKey: Any? = null,
): SwipeCardsState {
    return if (rememberKey != null) {
        val keyString = rememberKey.toString()
        remember(keyString) {
            SwipeCardsState(initialSelectedItemIndex = initialSelectedItemIndex)
        }
    } else {
        rememberSaveable(
            saver = SwipeCardsState.Saver,
            key = initialSelectedItemIndex.toString(),
        ) {
            SwipeCardsState(initialSelectedItemIndex = initialSelectedItemIndex)
        }
    }
}
