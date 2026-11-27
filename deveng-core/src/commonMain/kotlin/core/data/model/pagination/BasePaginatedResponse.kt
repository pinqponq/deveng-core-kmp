package core.data.model.pagination

import kotlinx.serialization.Serializable

@Serializable
open class BasePaginatedResponse(
    open val page: Int = 0,
    open val size: Int = 0,
    open val rowCount: Int = 0,
    open val pageCount: Int = 0,
    open val hasNextPage: Boolean = false,
    open val firstRowOnPage: Int = 0,
    open val lastRowOnPage: Int = 0
)