package core.widget.domain.image

private const val ARGB_BYTES_PER_PIXEL = 4

/** Power-of-two decode sample sizes (BitmapFactory.Options.inSampleSize) for the widget image. */
object HomeScreenWidgetImageSampling {

    /** The largest sample size that still keeps the longest side at or above [maxSidePx]; the rest is scaled. */
    fun calculateSampleSizeForMaxSide(width: Int, height: Int, maxSidePx: Int): Int {
        var sampleSize = 1
        while (maxOf(width, height) / (sampleSize * 2) >= maxSidePx) {
            sampleSize *= 2
        }
        return sampleSize
    }

    /** The smallest sample size whose decoded ARGB bitmap fits in [budgetBytes]. */
    fun calculateSampleSizeForByteBudget(width: Int, height: Int, budgetBytes: Long): Int {
        var sampleSize = 1
        while (calculateBytesAfterSampling(width, height, sampleSize) > budgetBytes) {
            sampleSize *= 2
        }
        return sampleSize
    }

    private fun calculateBytesAfterSampling(width: Int, height: Int, sampleSize: Int): Long {
        return (width / sampleSize).toLong() * (height / sampleSize) * ARGB_BYTES_PER_PIXEL
    }
}
