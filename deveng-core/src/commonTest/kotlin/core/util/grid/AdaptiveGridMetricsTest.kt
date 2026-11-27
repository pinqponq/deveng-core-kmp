package core.util.grid

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AdaptiveGridMetricsTest {

    @Test
    fun calculateAdaptiveGridMetrics_withRoomForSeveralCells_fillsTheRowExactly() {
        val metrics = calculateAdaptiveGridMetrics(availableWidthPx = 1_000, gapPx = GAP_PX, minCellSizePx = MIN_CELL_SIZE_PX)

        assertGridMetrics(metrics, columnCount = 4, cellSizePx = 242)
    }

    @Test
    fun calculateAdaptiveGridMetrics_withWidthBelowOneCell_returnsOneColumnOfFullWidth() {
        val metrics = calculateAdaptiveGridMetrics(availableWidthPx = 150, gapPx = GAP_PX, minCellSizePx = MIN_CELL_SIZE_PX)

        assertGridMetrics(metrics, columnCount = 1, cellSizePx = 150)
    }

    @Test
    fun calculateAdaptiveGridMetrics_withMaxColumns_capsTheColumnCount() {
        val metrics = calculateAdaptiveGridMetrics(
            availableWidthPx = 1_000,
            gapPx = GAP_PX,
            minCellSizePx = MIN_CELL_SIZE_PX,
            maxColumns = 2,
        )

        assertGridMetrics(metrics, columnCount = 2, cellSizePx = 495)
    }

    @Test
    fun calculateAdaptiveGridMetrics_withWidthNotDivisibleByColumns_neverOverflowsTheRow() {
        val availableWidthPx = 1_001
        val metrics = calculateAdaptiveGridMetrics(availableWidthPx = availableWidthPx, gapPx = GAP_PX, minCellSizePx = MIN_CELL_SIZE_PX)

        val usedWidthPx = metrics.columnCount * metrics.cellSizePx + (metrics.columnCount - 1) * GAP_PX
        assertGridMetrics(metrics, columnCount = 4, cellSizePx = 242)
        assertTrue(usedWidthPx <= availableWidthPx)
    }

    private fun assertGridMetrics(metrics: AdaptiveGridMetrics, columnCount: Int, cellSizePx: Int) {
        assertEquals(columnCount, metrics.columnCount)
        assertEquals(cellSizePx, metrics.cellSizePx)
    }

    private companion object {
        const val GAP_PX = 10
        const val MIN_CELL_SIZE_PX = 200
    }
}
