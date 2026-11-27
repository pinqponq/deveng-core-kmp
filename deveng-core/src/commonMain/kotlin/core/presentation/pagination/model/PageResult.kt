package core.presentation.pagination.model

data class PageResult<T>(
    val items: List<T>,
    val hasNextPage: Boolean? = null
)