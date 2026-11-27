package core.presentation.component.lazyswipecards

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.layout.IntervalList
import androidx.compose.foundation.lazy.layout.LazyLayoutIntervalContent
import androidx.compose.foundation.lazy.layout.LazyLayoutItemProvider
import androidx.compose.foundation.lazy.layout.getDefaultLazyLayoutKey
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun rememberSwipeCardsItemProvider(
    content: SwipeCardsScope.() -> Unit,
    isEndless: Boolean,
    state: SwipeCardsState,
    contentSeedKey: Any? = null,
    onAllItemsConsumed: (() -> Unit)? = null,
    onSwipeLeft: ((Any?) -> Unit)? = null,
    onSwipeRight: ((Any?) -> Unit)? = null,
): SwipeCardsItemProvider {
    val latestContent = rememberUpdatedState(content)
    val latestOnAllItemsConsumed = rememberUpdatedState(onAllItemsConsumed)
    val latestOnSwipeLeft = rememberUpdatedState(onSwipeLeft)
    val latestOnSwipeRight = rememberUpdatedState(onSwipeRight)

    return remember(isEndless, state, contentSeedKey) {
        val listScope = SwipeCardsScopeImpl()
            .apply(latestContent.value)
        val itemProviderState = derivedStateOf {
            SwipeCardsItemProviderImpl(
                intervals = listScope.intervals,
                onSwiped = listScope.onSwiped,
                onSwiping = listScope.onSwiping,
                isEndless = isEndless,
                state = state,
                onAllItemsConsumed = { latestOnAllItemsConsumed.value?.invoke() },
                onSwipeLeft = { latestOnSwipeLeft.value?.invoke(it) },
                onSwipeRight = { latestOnSwipeRight.value?.invoke(it) },
            )
        }
        delegatingSwipeCardsItemProvider(itemProviderState)
    }
}

@OptIn(ExperimentalFoundationApi::class)
internal interface SwipeCardsItemProvider : LazyLayoutItemProvider {

    fun onSwiped(targetOffset: Float): Boolean
    fun onSwiping(offset: Float, ratio: Float)
}

@OptIn(ExperimentalFoundationApi::class)
private class SwipeCardsItemProviderImpl(
    val intervals: IntervalList<SwipeCardsIntervalContent>,
    val onSwiped: OnSwipedFunction?,
    val onSwiping: OnSwipingFunction?,
    val isEndless: Boolean,
    val state: SwipeCardsState,
    val onAllItemsConsumed: () -> Unit,
    val onSwipeLeft: (Any?) -> Unit,
    val onSwipeRight: (Any?) -> Unit,
) : SwipeCardsItemProvider {

    private val lazyItems: LazyLayoutItemProvider = createLazyLayoutItemProvider(
        intervals = intervals,
        itemContent = { interval, index -> interval.item.invoke(index) },
    )

    override val itemCount: Int get() = lazyItems.itemCount

    @Composable
    override fun Item(index: Int, key: Any) {
        lazyItems.Item(index, key)
    }

    override fun getKey(index: Int): Any = lazyItems.getKey(index)

    override fun getContentType(index: Int): Any? = lazyItems.getContentType(index)

    override fun onSwiping(offset: Float, ratio: Float) {
        onSwiping?.invoke(offset, ratio, getSwipeDirection(offset))
    }

    override fun onSwiped(targetOffset: Float): Boolean {
        val selectedIndex = state.selectedItemIndex
        val realFrontIndex = if (isEndless) {
            selectedIndex % itemCount
        } else {
            if (selectedIndex >= itemCount) {
                return false
            }
            selectedIndex
        }
        val key = getKey(realFrontIndex)
        withLocalIntervalIndex(realFrontIndex, intervals) { localIndex, content ->
            val item = content.list[localIndex]
            val direction = getSwipeDirection(targetOffset)
            state.pushSwipe(key, direction)
            onSwiped?.invoke(item, direction)
            when (direction) {
                SwipeDirection.LEFT -> onSwipeLeft(key)
                SwipeDirection.RIGHT -> onSwipeRight(key)
            }
            state.selectNextItem()
            if (!isEndless && state.selectedItemIndex >= itemCount) {
                onAllItemsConsumed()
            }
        }
        return true
    }
}

private fun delegatingSwipeCardsItemProvider(
    delegate: State<SwipeCardsItemProvider>
): SwipeCardsItemProvider =
    DefaultDelegatingSwipeCardsItemProvider(delegate)

private class DefaultDelegatingSwipeCardsItemProvider(
    private val delegate: State<SwipeCardsItemProvider>
) : SwipeCardsItemProvider {
    override val itemCount: Int get() = delegate.value.itemCount

    @Composable
    override fun Item(index: Int, key: Any) {
        delegate.value.Item(index, key)
    }

    override fun getKey(index: Int): Any = delegate.value.getKey(index)

    override fun getContentType(index: Int): Any? = delegate.value.getContentType(index)

    override fun onSwiped(targetOffset: Float): Boolean = delegate.value.onSwiped(targetOffset)

    override fun onSwiping(offset: Float, ratio: Float) = delegate.value.onSwiping(offset, ratio)
}

@OptIn(ExperimentalFoundationApi::class)
private fun <T : LazyLayoutIntervalContent.Interval> createLazyLayoutItemProvider(
    intervals: IntervalList<T>,
    itemContent: @Composable (interval: T, index: Int) -> Unit,
): LazyLayoutItemProvider =
    DefaultLazyLayoutItemsProvider(itemContent, intervals)

@OptIn(ExperimentalFoundationApi::class)
private class DefaultLazyLayoutItemsProvider<IntervalContent : LazyLayoutIntervalContent.Interval>(
    val itemContentProvider: @Composable IntervalContent.(index: Int) -> Unit,
    val intervals: IntervalList<IntervalContent>
) : LazyLayoutItemProvider {
    override val itemCount get() = intervals.size

    @Composable
    override fun Item(index: Int, key: Any) {
        withLocalIntervalIndex(index, intervals) { localIndex, content ->
            content.itemContentProvider(localIndex)
        }
    }

    override fun getKey(index: Int): Any =
        withLocalIntervalIndex(index, intervals) { localIndex, content ->
            content.key?.invoke(localIndex) ?: getDefaultLazyLayoutKey(index)
        }

    override fun getContentType(index: Int): Any? =
        withLocalIntervalIndex(index, intervals) { localIndex, content ->
            content.type.invoke(localIndex)
        }
}

@OptIn(ExperimentalFoundationApi::class)
internal inline fun <IntervalContent : LazyLayoutIntervalContent.Interval, T> withLocalIntervalIndex(
    index: Int,
    intervals: IntervalList<IntervalContent>,
    block: (localIndex: Int, content: IntervalContent) -> T
): T {
    val interval = intervals[index]
    val localIntervalIndex = index - interval.startIndex
    return block(localIntervalIndex, interval.value)
}
