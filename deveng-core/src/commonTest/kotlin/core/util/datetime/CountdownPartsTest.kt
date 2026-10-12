package core.util.datetime

import kotlin.test.Test
import kotlin.test.assertEquals

class CountdownPartsTest {

    @Test
    fun splitIntoCountdownParts_withHoursMinutesAndSeconds_returnsEachPart() {
        val remainingMillis = 2 * HOUR_MILLIS + 5 * MINUTE_MILLIS + 9 * SECOND_MILLIS

        assertEquals(CountdownParts(hours = 2, minutes = 5, seconds = 9), splitIntoCountdownParts(remainingMillis))
    }

    @Test
    fun splitIntoCountdownParts_withMoreThanOneDay_keepsHoursAboveTwentyFour() {
        val remainingMillis = 30 * HOUR_MILLIS + 59 * SECOND_MILLIS

        assertEquals(CountdownParts(hours = 30, minutes = 0, seconds = 59), splitIntoCountdownParts(remainingMillis))
    }

    @Test
    fun splitIntoCountdownParts_withPartialSecond_dropsTheRemainder() {
        val remainingMillis = MINUTE_MILLIS + 999

        assertEquals(CountdownParts(hours = 0, minutes = 1, seconds = 0), splitIntoCountdownParts(remainingMillis))
    }

    @Test
    fun splitIntoCountdownParts_withNegativeDuration_returnsZero() {
        assertEquals(CountdownParts(hours = 0, minutes = 0, seconds = 0), splitIntoCountdownParts(-HOUR_MILLIS))
    }

    private companion object {
        const val SECOND_MILLIS = 1_000L
        const val MINUTE_MILLIS = 60 * SECOND_MILLIS
        const val HOUR_MILLIS = 60 * MINUTE_MILLIS
    }
}
