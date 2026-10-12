package core.util.grid

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AdaptiveGridMetricsTest {

    @Test
    fun calculateAdaptiveGridMetrics_withRoomForSeveralCells_fillsTheRowExactly() {
        val metrics = calculateAdaptiveGridMetrics(availableWidthPx = 1_000, gapPx = GAP_PX, minCellSizePx = MIN_CELL_SIZE_PX)

        assertEquals(AdaptiveGridMetrics(columnCount = 4, cellSizePx = 242), metrics)
    }

    @Test
    fun calculateAdaptiveGridMetrics_withWidthBelowOneCell_returnsOneColumnOfFullWidth() {
        val metrics = calculateAdaptiveGridMetrics(availableWidthPx = 150, gapPx = GAP_PX, minCellSizePx = MIN_CELL_SIZE_PX)

        assertEquals(AdaptiveGridMetrics(columnCount = 1, cellSizePx = 150), metrics)
    }

    @Test
    fun calculateAdaptiveGridMetrics_withMaxColumns_capsTheColumnCount() {
        val metrics = calculateAdaptiveGridMetrics(
            availableWidthPx = 1_000,
            gapPx = GAP_PX,
            minCellSizePx = MIN_CELL_SIZE_PX,
            maxColumns = 2,
        )

        assertEquals(AdaptiveGridMetrics(columnCount = 2, cellSizePx = 495), metrics)
    }

    @Test
    fun calculateAdaptiveGridMetrics_withWidthNotDivisibleByColumns_neverOverflowsTheRow() {
        val availableWidthPx = 1_001
        val metrics = calculateAdaptiveGridMetrics(availableWidthPx = availableWidthPx, gapPx = GAP_PX, minCellSizePx = MIN_CELL_SIZE_PX)

        val usedWidthPx = metrics.columnCount * metrics.cellSizePx + (metrics.columnCount - 1) * GAP_PX
        assertEquals(4, metrics.columnCount)
        assertTrue(usedWidthPx <= availableWidthPx)
    }

    private companion object {
        const val GAP_PX = 10
        const val MIN_CELL_SIZE_PX = 200
    }
}
