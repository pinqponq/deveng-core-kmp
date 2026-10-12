package core.util.datetime

import kotlin.test.Test
import kotlin.test.assertEquals

class CountdownPartsTest {

    @Test
    fun splitIntoCountdownParts_withHoursMinutesAndSeconds_returnsEachPart() {
        val remainingMillis = 2 * HOUR_MILLIS + 5 * MINUTE_MILLIS + 9 * SECOND_MILLIS

        assertCountdownParts(splitIntoCountdownParts(remainingMillis), hours = 2, minutes = 5, seconds = 9)
    }

    @Test
    fun splitIntoCountdownParts_withMoreThanOneDay_keepsHoursAboveTwentyFour() {
        val remainingMillis = 30 * HOUR_MILLIS + 59 * SECOND_MILLIS

        assertCountdownParts(splitIntoCountdownParts(remainingMillis), hours = 30, minutes = 0, seconds = 59)
    }

    @Test
    fun splitIntoCountdownParts_withPartialSecond_dropsTheRemainder() {
        val remainingMillis = MINUTE_MILLIS + 999

        assertCountdownParts(splitIntoCountdownParts(remainingMillis), hours = 0, minutes = 1, seconds = 0)
    }

    @Test
    fun splitIntoCountdownParts_withNegativeDuration_returnsZero() {
        assertCountdownParts(splitIntoCountdownParts(-HOUR_MILLIS), hours = 0, minutes = 0, seconds = 0)
    }

    private fun assertCountdownParts(countdownParts: CountdownParts, hours: Long, minutes: Long, seconds: Long) {
        assertEquals(hours, countdownParts.hours)
        assertEquals(minutes, countdownParts.minutes)
        assertEquals(seconds, countdownParts.seconds)
    }

    private companion object {
        const val SECOND_MILLIS = 1_000L
        const val MINUTE_MILLIS = 60 * SECOND_MILLIS
        const val HOUR_MILLIS = 60 * MINUTE_MILLIS
    }
}
