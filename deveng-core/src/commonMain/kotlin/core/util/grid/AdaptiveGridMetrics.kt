package core.util.grid

/**
 * Column count and square cell size of an adaptive grid row.
 *
 * @property columnCount number of cells in one row.
 * @property cellSizePx edge length of each cell in pixels.
 */
data class AdaptiveGridMetrics(
    val columnCount: Int,
    val cellSizePx: Int,
)

/**
 * Calculates how many cells fit in a row and how large each cell is.
 * Works in integer pixels so that dp-to-px rounding can never push the last column out of the row.
 *
 * @param availableWidthPx width of the row in pixels, usually `BoxWithConstraints.constraints.maxWidth`.
 * @param gapPx space between cells in pixels.
 * @param minCellSizePx smallest allowed cell size in pixels; decides the column count.
 * @param maxColumns upper limit for the column count; unlimited by default.
 */
fun calculateAdaptiveGridMetrics(
    availableWidthPx: Int,
    gapPx: Int,
    minCellSizePx: Int,
    maxColumns: Int = Int.MAX_VALUE,
): AdaptiveGridMetrics {
    val columnCount = (availableWidthPx / (minCellSizePx + gapPx))
        .coerceIn(1, maxColumns)
    val cellSizePx = (availableWidthPx - gapPx * (columnCount - 1)) / columnCount
    return AdaptiveGridMetrics(
        columnCount = columnCount,
        cellSizePx = cellSizePx,
    )
}
