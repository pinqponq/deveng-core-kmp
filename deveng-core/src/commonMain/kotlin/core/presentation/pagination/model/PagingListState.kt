package core.presentation.pagination.model

data class PaginatedListState<T>(
    val items: List<T> = emptyList(),
    val isInitialLoad: Boolean = false,
    val isNextPageLoading: Boolean = false,
    val hasNextPage: Boolean = false,
    val hasLoadedBefore: Boolean = false,
    val isError: Boolean = false
)