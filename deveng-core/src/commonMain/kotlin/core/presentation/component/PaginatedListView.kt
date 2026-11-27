package core.presentation.component

import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import core.presentation.component.scrollbar.scrollbarWithLazyListState
import core.presentation.pagination.model.PaginatedListState
import core.presentation.theme.CoreCustomBlackColor
import core.presentation.theme.CoreMediumTextStyle
import core.presentation.theme.LocalComponentTheme
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlin.math.abs


/**
 * A cross-platform, pagination-aware list component for Compose Multiplatform.
 *
 * It supports:
 * - Endless scrolling (auto "load more" when reaching top/bottom depending on layout mode)
 * - Retry failed page loads via pull gestures:
 *      - Normal mode: pull UP from bottom to retry
 *      - Reverse/chat mode: pull DOWN from top to retry
 * - Optional reversed layout behavior (chat style)
 * - LazyColumn key support for stable item reuse
 * - Fully customizable error and retry UI text through composable providers
 *
 * This component does NOT perform data loading itself.
 * It only exposes events:
 *  - [onScrollReachNextPageThreshold] → triggered when user scrolls near the loading edge
 *  - [onSwipeAtListsEnd] → triggered when user performs the retry gesture
 *
 * Pair it with a paging loader (e.g., PaginatedFlowLoader) to manage state.
 *
 * @param state The current pagination UI state containing items, loading state, error, and pagination info.
 * @param onScrollReachNextPageThreshold Callback triggered automatically when the scroll position reaches the pagination boundary.
 * @param onSwipeAtListsEnd Callback triggered by user retry gesture when an error exists.
 * @param itemSlot Composable that renders an item from the list, receives the item of type T.
 * @param spaceBetweenItems Vertical spacing in dp between list items. Default is 12.
 * @param horizontalPadding Horizontal padding around the list in dp. Default is 13.
 * @param listBottomPadding Extra bottom padding in dp. Default is 5.
 * @param modifier Parent modifier to be applied to the list container.
 * @param prefetchThreshold Number of items before the edge to trigger loading of next page. Default is 10.
 * @param isReverseLayout Enables chat-like behavior (load-at-top, retry-at-top). Default is false.
 * @param itemKey Optional function providing stable keys for better LazyColumn performance.
 * @param textStyle Text style for empty list and error messages. Default uses theme medium text style.
 * @param emptyListText Text to display when the list is empty. If null, nothing is shown.
 * @param errorText Text hint for error gesture. If null, nothing is shown.
 * @param pullToRetryText Text hint for pull-to-retry gesture. If null, nothing is shown.
 */
@Composable
fun <T> PaginatedListView(
    state: PaginatedListState<T>,
    listState: LazyListState = rememberLazyListState(),
    onScrollReachNextPageThreshold: () -> Unit,
    onSwipeAtListsEnd: () -> Unit,
    itemSlot: @Composable (T) -> Unit,
    spaceBetweenItems: Int = 12,
    horizontalPadding: Int = 13,
    listBottomPadding: Int = 5,
    modifier: Modifier = Modifier,
    prefetchThreshold: Int = 10,
    isReverseLayout: Boolean = false,
    itemKey: ((T) -> Any)? = null,
    textStyle: TextStyle = CoreMediumTextStyle().copy(
        fontSize = 16.sp,
        color = CoreCustomBlackColor
    ),
    emptyListText: String? = null,
    errorText: String? = null,
    pullToRetryText: String? = null
) {
    // The LazyColumn renders more than [state.items]: reverse mode prepends a status
    // item (loading spinner / empty / error) and every layout appends a trailing Spacer.
    // So the last LAZY index is NOT state.items.lastIndex. Targeting state.items.size + 1
    // (one past the newest message) lands on that trailing Spacer - clamped by the list
    // to the real last index - which reliably pins the viewport to the true bottom
    // regardless of how many status items are present.
    val bottomScrollTargetIndex = state.items.size + 1

    // True while the newest item is (about to be) at the bottom of the viewport.
    val isAtBottom by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val lastVisibleIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            layoutInfo.totalItemsCount > 0 && lastVisibleIndex >= layoutInfo.totalItemsCount - 2
        }
    }

    // Reverse/chat mode auto-scroll. Keyed on the LAST item's key (not the list size)
    // so it reacts only when a new newest item is appended at the bottom - not when an
    // older page is prepended at the top, which would otherwise yank the user back down
    // on their first scroll-up.
    var didAutoScrollToEnd by remember { mutableStateOf(false) }
    val lastItemKey = state.items.lastOrNull()?.let { item -> itemKey?.invoke(item) ?: item }

    LaunchedEffect(isReverseLayout, lastItemKey) {
        if (!isReverseLayout) {
            if (state.items.isEmpty()) didAutoScrollToEnd = false
            return@LaunchedEffect
        }
        if (state.items.isEmpty()) return@LaunchedEffect

        if (!didAutoScrollToEnd) {
            // First load: jump straight to the newest message.
            listState.scrollToItem(bottomScrollTargetIndex)
            didAutoScrollToEnd = true
            return@LaunchedEffect
        }

        // A new newest message arrived. Stay pinned to the bottom only if the user was
        // already there; if they scrolled up to read history, leave them where they are.
        if (isAtBottom) {
            listState.animateScrollToItem(bottomScrollTargetIndex)
        }
    }

    // Reverse/chat mode: keep the newest message visible while the keyboard opens. The
    // list viewport shrinks by the ime inset as it animates in, which would otherwise
    // slide the bottom messages behind the input.
    //
    // The "was the user at the bottom" decision must be made from BEFORE the keyboard
    // starts moving. Reading isAtBottom on the first ime frame is a race: on slower
    // devices the first frame arrives after the layout already shrank, so isAtBottom
    // reads false and the follow never starts. Instead we snapshot isAtBottom
    // continuously while the keyboard is closed (imeBottom == 0) into
    // wasAtBottomBeforeIme, and once it opens we follow the ime height frame by frame
    // based on that pre-keyboard snapshot - independent of device timing. History
    // reading is left undisturbed because the snapshot is false when scrolled up.
    val imeBottom = WindowInsets.ime.getBottom(LocalDensity.current)
    var wasAtBottomBeforeIme by remember { mutableStateOf(true) }

    LaunchedEffect(isReverseLayout, imeBottom, isAtBottom) {
        if (!isReverseLayout || state.items.isEmpty()) return@LaunchedEffect

        if (imeBottom <= 0) {
            wasAtBottomBeforeIme = isAtBottom
            return@LaunchedEffect
        }
        if (wasAtBottomBeforeIme) {
            listState.scrollToItem(bottomScrollTargetIndex)
        }
    }

    // infinite scroll trigger
    LaunchedEffect(
        listState,
        state.items.size,
        state.hasNextPage,
        state.isNextPageLoading,
        state.isError,
        isReverseLayout
    ) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val firstVisible = layoutInfo.visibleItemsInfo.firstOrNull()?.index ?: -1
            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            // totalItemsCount lets us tell a stale, pre-measurement frame from a settled
            // one: right after a page (pre)pends, this still reports the OLD layout until
            // the LazyColumn re-measures.
            val laidOutCount = layoutInfo.totalItemsCount
            Triple(firstVisible, lastVisible, laidOutCount)
        }
            .distinctUntilChanged()
            // Normal (append) lists keep the original drop(1): it skips the synchronous
            // emission when this effect restarts after a page loads, so a freshly loaded
            // page that already fills the viewport does not immediately trigger another
            // load. Reverse/chat lists instead rely on the stale-frame filter below, so a
            // settled fling still triggers the next page without an extra manual scroll.
            // Prepend pushes the first index away from the top after each load, so this
            // cannot run away.
            .let { positionFlow -> if (isReverseLayout) positionFlow else positionFlow.drop(1) }
            .collectLatest { (firstVisibleIndex, lastVisibleIndex, laidOutCount) ->
                val totalItems = state.items.size
                if (totalItems == 0) return@collectLatest

                // Skip stale frames emitted right after a page (pre)pends but before the
                // LazyColumn has measured the new items: the visible indices still describe
                // the old layout. Once measured, the laid-out count covers every item (plus
                // the status/spacer rows), so it is never below the item count.
                if (laidOutCount < totalItems) return@collectLatest

                val shouldLoadMore = if (!isReverseLayout) {
                    val rawTriggerIndex = totalItems - prefetchThreshold
                    if (rawTriggerIndex <= 0) {
                        lastVisibleIndex >= totalItems - 1
                    } else {
                        lastVisibleIndex >= rawTriggerIndex
                    }
                } else {
                    val rawTriggerIndex = prefetchThreshold - 1
                    if (rawTriggerIndex >= totalItems - 1) {
                        firstVisibleIndex <= 0
                    } else {
                        firstVisibleIndex in 0..rawTriggerIndex
                    }
                }

                if (
                    shouldLoadMore &&
                    state.hasNextPage &&
                    !state.isNextPageLoading &&
                    !state.isError
                ) {
                    onScrollReachNextPageThreshold()
                }
            }
    }

    // retry gesture (bottom-up normal, top-down reverse) ----
    val isAtLoadEdge by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            if (!isReverseLayout) {
                val total = layoutInfo.totalItemsCount
                val last = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
                total > 0 && last >= total - 1
            } else {
                listState.firstVisibleItemIndex == 0 &&
                        listState.firstVisibleItemScrollOffset == 0
            }
        }
    }

    val retryEnabled by remember(state.hasNextPage, state.isNextPageLoading, state.isError) {
        mutableStateOf(
            state.hasNextPage &&
                    state.isError &&
                    !state.isNextPageLoading
        )
    }

    var retryPullDistancePx by remember { mutableStateOf(0f) }
    val triggerDistancePx = with(LocalDensity.current) { 60.dp.toPx() }

    val retryDragModifier = Modifier.pointerInput(retryEnabled, isAtLoadEdge, isReverseLayout) {
        detectVerticalDragGestures(
            onVerticalDrag = { change, dragAmount ->
                if (!retryEnabled || !isAtLoadEdge) return@detectVerticalDragGestures

                val isValidDrag = if (!isReverseLayout) dragAmount < 0 else dragAmount > 0
                if (isValidDrag) {
                    retryPullDistancePx += abs(dragAmount)
                    change.consume()
                }
            },
            onDragEnd = {
                if (retryEnabled && retryPullDistancePx >= triggerDistancePx) {
                    onSwipeAtListsEnd()
                }
                retryPullDistancePx = 0f
            },
            onDragCancel = {
                retryPullDistancePx = 0f
            }
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(retryDragModifier)
            .padding(horizontal = horizontalPadding.dp)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            state = listState,
            verticalArrangement = if (state.items.isEmpty()) {
                Arrangement.Center
            } else {
                Arrangement.spacedBy(spaceBetweenItems.dp)
            },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Helper function to add status items (loading, error, empty)
            fun addStatusItems() {
                if (state.isInitialLoad && state.items.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }

                if (state.isNextPageLoading && state.items.isNotEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }

                if (
                    state.items.isEmpty() &&
                    !state.isNextPageLoading &&
                    !state.isInitialLoad &&
                    !state.isError &&
                    state.hasLoadedBefore &&
                    emptyListText != null
                ) {
                    item {
                        Text(
                            modifier = Modifier.padding(horizontal = 75.dp),
                            text = emptyListText,
                            style = textStyle,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                if (state.isError) {
                    item {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            errorText?.let { text ->
                                Text(
                                    modifier = Modifier.padding(horizontal = 75.dp),
                                    text = text,
                                    style = textStyle,
                                    textAlign = TextAlign.Center
                                )
                            }

                            pullToRetryText?.let { text ->
                                Text(
                                    modifier = Modifier.padding(horizontal = 75.dp),
                                    text = text,
                                    style = textStyle,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            // Add status items before content in reverse layout
            if (isReverseLayout) {
                addStatusItems()
            }

            // List items
            if (itemKey == null) {
                items(state.items.size) { index ->
                    itemSlot(state.items[index])
                }
            } else {
                items(
                    items = state.items,
                    key = { item -> itemKey(item) }
                ) { item ->
                    itemSlot(item)
                }
            }

            // Add status items after content in normal layout
            if (!isReverseLayout) {
                addStatusItems()
            }

            item {
                Spacer(modifier = Modifier.height(listBottomPadding.dp))
            }
        }
    }
}