package core.presentation.component.lazyswipecards

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.layout.IntervalList
import androidx.compose.foundation.lazy.layout.LazyLayoutIntervalContent
import androidx.compose.foundation.lazy.layout.MutableIntervalList
import androidx.compose.runtime.Composable

internal typealias OnSwipingFunction = (offset: Float, ratio: Float, direction: SwipeDirection) -> Unit
internal typealias OnSwipedFunction = (item: Any?, direction: SwipeDirection) -> Unit

interface SwipeCardsScope {

    fun onSwiping(function: OnSwipingFunction)

    fun onSwiped(function: OnSwipedFunction)

    fun <T> itemsIndexed(
        items: List<T>,
        itemContent: @Composable (index: Int, item: T) -> Unit
    )

    fun <T> itemsIndexed(
        items: List<T>,
        key: (index: Int, item: T) -> Any,
        itemContent: @Composable (index: Int, item: T) -> Unit
    )

    fun <T> items(
        items: List<T>,
        itemContent: @Composable (item: T) -> Unit
    )

    fun <T> items(
        items: List<T>,
        key: (item: T) -> Any,
        itemContent: @Composable (item: T) -> Unit
    )
}

@OptIn(ExperimentalFoundationApi::class)
internal class SwipeCardsScopeImpl : SwipeCardsScope {

    private val _intervals = MutableIntervalList<SwipeCardsIntervalContent>()
    val intervals: IntervalList<SwipeCardsIntervalContent> = _intervals

    var onSwiped: OnSwipedFunction? = null
    var onSwiping: OnSwipingFunction? = null

    override fun <T> items(
        items: List<T>,
        itemContent: @Composable (item: T) -> Unit
    ) {
        _intervals.addInterval(
            size = items.size,
            SwipeCardsIntervalContent(items, key = null) {
                itemContent(items[it])
            }
        )
    }

    override fun <T> items(
        items: List<T>,
        key: (item: T) -> Any,
        itemContent: @Composable (item: T) -> Unit
    ) {
        _intervals.addInterval(
            size = items.size,
            SwipeCardsIntervalContent(items, key = { key(items[it]) }) {
                itemContent(items[it])
            }
        )
    }

    override fun <T> itemsIndexed(
        items: List<T>,
        itemContent: @Composable (index: Int, item: T) -> Unit
    ) {
        _intervals.addInterval(
            size = items.size,
            SwipeCardsIntervalContent(items, key = null) {
                itemContent(it, items[it])
            }
        )
    }

    override fun <T> itemsIndexed(
        items: List<T>,
        key: (index: Int, item: T) -> Any,
        itemContent: @Composable (index: Int, item: T) -> Unit
    ) {
        _intervals.addInterval(
            size = items.size,
            SwipeCardsIntervalContent(items, key = { key(it, items[it]) }) {
                itemContent(it, items[it])
            }
        )
    }

    override fun onSwiped(function: OnSwipedFunction) {
        onSwiped = function
    }

    override fun onSwiping(function: OnSwipingFunction) {
        onSwiping = function
    }
}

@OptIn(ExperimentalFoundationApi::class)
internal class SwipeCardsIntervalContent(
    val list: List<*>,
    override val key: ((Int) -> Any)?,
    val item: @Composable (index: Int) -> Unit
) : LazyLayoutIntervalContent.Interval
