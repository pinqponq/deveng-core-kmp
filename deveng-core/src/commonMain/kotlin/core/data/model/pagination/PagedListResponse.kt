package core.data.model.pagination

import kotlinx.serialization.Serializable

@Serializable
data class PagedListResponse<T>(
    val results: List<T>,
) : BasePaginatedResponse()