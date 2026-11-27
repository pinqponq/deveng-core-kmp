package core.domain.pagination.model

import core.data.model.pagination.PagedListResponse

data class PagedList<T>(
    val items: List<T> = emptyList(),
    val page: Int = 0,
    val totalPageCount: Int,
    val totalItemCount: Int,
    val hasNextPage: Boolean,
    val firstRowOnPage: Int,
    val lastRowOnPage: Int
)

fun <T, R> PagedListResponse<T>.mapItems(transform: (T) -> R): PagedList<R> {
    return PagedList(
        items = this.results.map(transform),
        page = this.page,
        totalPageCount = this.pageCount,
        totalItemCount = this.rowCount,
        hasNextPage = this.hasNextPage,
        firstRowOnPage = this.firstRowOnPage,
        lastRowOnPage = this.lastRowOnPage
    )
}