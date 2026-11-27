package core.presentation.pagination

import core.presentation.pagination.model.PageResult
import core.presentation.pagination.model.PaginatedListState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Loads pages of items on demand and exposes them as a [PaginatedListState] flow.
 *
 * @param shouldPrependNewPages When true, each next page is inserted BEFORE the current
 * items instead of appended after them. Use for reverse/chat style lists (paired with
 * PaginatedListView's isReverseLayout) where page 1 is the newest slice shown at the
 * bottom and every next page is an older slice that must land at the top.
 * @param itemKey When provided, a next page is de-duplicated against the already loaded
 * items by this key, keeping the existing copy. Prevents duplicate entries (and the
 * duplicate-key crash in keyed lists) when page-based windows overlap because items were
 * added at the newest end between fetches. When null, pages are combined as-is.
 */
class PaginatedFlowLoader<Key, Item>(
    private val initialKey: Key,
    private val scope: CoroutineScope,
    private val pageSize: Int,
    private var pageSource: suspend (key: Key, size: Int) -> PageResult<Item>,
    private val getNextKey: (Key, List<Item>) -> Key,
    private val shouldPrependNewPages: Boolean = false,
    private val onSuccess: ((List<Item>) -> Unit)? = null,
    private val onError: ((Throwable) -> Unit)? = null,
    private val itemKey: ((Item) -> Any)? = null
) {
    private val _state = MutableStateFlow(PaginatedListState<Item>())
    val state: StateFlow<PaginatedListState<Item>> = _state

    private var currentKey: Key = initialKey
    private var isLoading = false
    private var currentJob: Job? = null

    /**
     * Reloads from the beginning using the SAME pageSource.
     * Use for full refresh or pull-to-refresh.
     */
    fun reset(newInitialKey: Key = initialKey) {
        currentJob?.cancel()
        currentKey = newInitialKey
        isLoading = false
        _state.value = PaginatedListState()
        loadInitialPage()
    }

    /**
     * Updates the underlying pageSource (e.g. same source with different filters,
     * queries or and entirely new sources)
     * and optionally triggers an immediate reload.
     */
    fun updatePageSource(
        newInitialKey: Key,
        newPageSource: suspend (key: Key, size: Int) -> PageResult<Item>,
        reload: Boolean = true
    ) {
        currentJob?.cancel()
        pageSource = newPageSource
        currentKey = newInitialKey
        isLoading = false
        _state.value = PaginatedListState()

        if (reload) {
            loadInitialPage()
        }
    }

    /**
     * Loads the first page. Safe to call multiple times.
     */
    fun loadInitialPage() {
        if (isLoading) return
        isLoading = true

        _state.update { it.copy(isInitialLoad = true, isError = false) }

        currentJob = scope.launch {
            try {
                val result = pageSource(currentKey, pageSize)
                val items = result.items
                val hasNext = result.hasNextPage ?: (items.size == pageSize)

                currentKey = getNextKey(currentKey, items)
                onSuccess?.invoke(items)

                _state.update { current ->
                    // Preserve items that were merged in (via updateItems) WHILE this
                    // initial page was loading - e.g. a realtime/optimistic chat message
                    // that arrived between load start and this response. A plain replace
                    // would drop them. Only keyed lists get this merge: itemKey provides
                    // the de-duplication, and non-keyed lists start from an empty state on
                    // initial load so their behavior is unchanged.
                    val combinedItems = if (itemKey != null && current.items.isNotEmpty()) {
                        combinePages(existingItems = current.items, newItems = items)
                    } else {
                        items
                    }
                    PaginatedListState(
                        items = combinedItems,
                        isInitialLoad = false,
                        isNextPageLoading = false,
                        hasNextPage = hasNext,
                        hasLoadedBefore = true,
                        isError = false
                    )
                }
            } catch (e: Throwable) {
                if (e.shouldAbortPaginationLoad(coroutineContext)) return@launch
                onError?.invoke(e)
                _state.update { it.copy(isInitialLoad = false, isError = true) }
            } finally {
                isLoading = false
            }
        }
    }

    /**
     * Transforms the currently loaded items in place, e.g. to merge in items that
     * arrive outside the paging flow (realtime/socket messages, local edits, deletions).
     *
     * Keeps the loader as the single source of truth: items merged here survive
     * subsequent [loadNextPage] emissions instead of being overwritten by them.
     */
    fun updateItems(transformItems: (List<Item>) -> List<Item>) {
        _state.update { it.copy(items = transformItems(it.items)) }
    }

    /**
     * Combines a freshly loaded page with the already loaded items, prepending or
     * appending per [shouldPrependNewPages]. When [itemKey] is set, items whose key is
     * already present are dropped from the new page so the existing copy (which may
     * carry live updates) is kept and no key appears twice.
     */
    private fun combinePages(existingItems: List<Item>, newItems: List<Item>): List<Item> {
        val key = itemKey
        val uniqueNewItems = if (key == null) {
            newItems
        } else {
            val existingKeys = existingItems.mapTo(HashSet()) { key(it) }
            newItems.filter { key(it) !in existingKeys }
        }
        return if (shouldPrependNewPages) {
            uniqueNewItems + existingItems
        } else {
            existingItems + uniqueNewItems
        }
    }

    /**
     * Loads the next page and appends it to current items
     * (or prepends when [shouldPrependNewPages] is enabled).
     */
    fun loadNextPage() {
        val currentState = _state.value
        if (isLoading || !currentState.hasNextPage) return

        isLoading = true
        _state.update { it.copy(isNextPageLoading = true, isError = false) }

        currentJob = scope.launch {
            try {
                val result = pageSource(currentKey, pageSize)
                val newItems = result.items
                val hasNext = result.hasNextPage ?: (newItems.size == pageSize)

                currentKey = getNextKey(currentKey, newItems)
                onSuccess?.invoke(newItems)

                _state.update {
                    val combinedItems = combinePages(existingItems = it.items, newItems = newItems)
                    it.copy(
                        items = combinedItems,
                        isNextPageLoading = false,
                        hasNextPage = hasNext,
                        hasLoadedBefore = true,
                        isError = false
                    )
                }
            } catch (e: Throwable) {
                if (e.shouldAbortPaginationLoad(coroutineContext)) return@launch
                onError?.invoke(e)
                _state.update { it.copy(isNextPageLoading = false, isError = true) }
            } finally {
                isLoading = false
            }
        }
    }
}

/**
 * Abort without surfacing an error when the load was cancelled or the owning [CoroutineScope] is
 * no longer active (e.g. ViewModel cleared on navigate away). Networking may still report a
 * generic network error instead of [CancellationException].
 */
private fun Throwable.shouldAbortPaginationLoad(
    context: kotlin.coroutines.CoroutineContext,
): Boolean {
    cancellationCauseOrNull()?.let { throw it }
    return !context.isActive
}

private fun Throwable.cancellationCauseOrNull(): CancellationException? {
    var current: Throwable? = this
    while (current != null) {
        if (current is CancellationException) {
            return current
        }
        current = current.cause
    }
    return null
}