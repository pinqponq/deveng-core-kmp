package core.presentation.component.mediaviewer.internal

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class VideoSeekPositionTest {

    @Test
    fun takeSeekTime_withKnownDuration_returnsFractionOfDuration() {
        val seekPosition = VideoSeekPosition()

        seekPosition.requestSeek(fraction = HALF)

        assertEquals(DURATION_SECONDS / 2, seekPosition.takeSeekTime(durationSeconds = DURATION_SECONDS))
    }

    @Test
    fun takeSeekTime_beforeDurationIsKnown_keepsSeekUntilDurationArrives() {
        val seekPosition = VideoSeekPosition()

        seekPosition.requestSeek(fraction = HALF)

        assertNull(seekPosition.takeSeekTime(durationSeconds = Double.NaN))
        assertNull(seekPosition.takeSeekTime(durationSeconds = 0.0))
        assertEquals(DURATION_SECONDS / 2, seekPosition.takeSeekTime(durationSeconds = DURATION_SECONDS))
    }

    @Test
    fun takeSeekTime_afterSeekWasApplied_returnsNull() {
        val seekPosition = VideoSeekPosition()
        seekPosition.requestSeek(fraction = HALF)
        seekPosition.takeSeekTime(durationSeconds = DURATION_SECONDS)

        assertNull(seekPosition.takeSeekTime(durationSeconds = DURATION_SECONDS))
    }

    @Test
    fun takeSeekTime_withNewerRequest_usesLatestFraction() {
        val seekPosition = VideoSeekPosition()

        seekPosition.requestSeek(fraction = HALF)
        seekPosition.requestSeek(fraction = QUARTER)

        assertEquals(DURATION_SECONDS / 4, seekPosition.takeSeekTime(durationSeconds = DURATION_SECONDS))
    }

    @Test
    fun takeSeekTime_withFractionOutsideRange_clampsToVideoBounds() {
        val seekPosition = VideoSeekPosition()

        seekPosition.requestSeek(fraction = BEYOND_END)
        assertEquals(DURATION_SECONDS, seekPosition.takeSeekTime(durationSeconds = DURATION_SECONDS))

        seekPosition.requestSeek(fraction = BEFORE_START)
        assertEquals(0.0, seekPosition.takeSeekTime(durationSeconds = DURATION_SECONDS))
    }

    @Test
    fun progress_withKnownDuration_returnsElapsedFraction() {
        assertEquals(QUARTER, VideoSeekPosition().progress(currentTimeSeconds = DURATION_SECONDS / 4, durationSeconds = DURATION_SECONDS))
    }

    @Test
    fun progress_withUnknownDuration_returnsNull() {
        val seekPosition = VideoSeekPosition()

        assertNull(seekPosition.progress(currentTimeSeconds = 1.0, durationSeconds = Double.NaN))
        assertNull(seekPosition.progress(currentTimeSeconds = 1.0, durationSeconds = 0.0))
    }

    @Test
    fun progress_withTimePastDuration_returnsOne() {
        assertEquals(1f, VideoSeekPosition().progress(currentTimeSeconds = DURATION_SECONDS + 1, durationSeconds = DURATION_SECONDS))
    }

    private companion object {
        const val DURATION_SECONDS = 8.0
        const val HALF = 0.5f
        const val QUARTER = 0.25f
        const val BEYOND_END = 1.5f
        const val BEFORE_START = -0.5f
    }
}
