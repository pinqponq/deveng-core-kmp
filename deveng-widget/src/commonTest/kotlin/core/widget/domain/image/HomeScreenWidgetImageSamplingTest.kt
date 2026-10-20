package core.widget.domain.image

import kotlin.test.Test
import kotlin.test.assertEquals

class HomeScreenWidgetImageSamplingTest {

    @Test
    fun calculateSampleSizeForMaxSide_withImageSmallerThanTwiceTheLimit_keepsFullSize() {
        assertEquals(1, HomeScreenWidgetImageSampling.calculateSampleSizeForMaxSide(width = 2879, height = 1000, maxSidePixels = MAX_SIDE_PIXELS))
    }

    @Test
    fun calculateSampleSizeForMaxSide_withExactlyTwiceTheLimit_halves() {
        assertEquals(2, HomeScreenWidgetImageSampling.calculateSampleSizeForMaxSide(width = 1000, height = 2880, maxSidePixels = MAX_SIDE_PIXELS))
    }

    @Test
    fun calculateSampleSizeForMaxSide_withLargePortraitImage_keepsLongestSideAboveTheLimit() {
        assertEquals(4, HomeScreenWidgetImageSampling.calculateSampleSizeForMaxSide(width = 3000, height = 6000, maxSidePixels = MAX_SIDE_PIXELS))
    }

    @Test
    fun calculateSampleSizeForByteBudget_withImageInsideBudget_keepsFullSize() {
        val exactBudgetBytes = 1000L * 500 * ARGB_BYTES_PER_PIXEL

        assertEquals(1, HomeScreenWidgetImageSampling.calculateSampleSizeForByteBudget(width = 1000, height = 500, budgetBytes = exactBudgetBytes))
    }

    @Test
    fun calculateSampleSizeForByteBudget_withImageOverBudget_samplesUntilItFits() {
        val budgetBytes = 1000L * 500 * ARGB_BYTES_PER_PIXEL / 4 - 1

        assertEquals(4, HomeScreenWidgetImageSampling.calculateSampleSizeForByteBudget(width = 1000, height = 500, budgetBytes = budgetBytes))
    }

    private companion object {
        const val MAX_SIDE_PIXELS = 1440
        const val ARGB_BYTES_PER_PIXEL = 4
    }
}
