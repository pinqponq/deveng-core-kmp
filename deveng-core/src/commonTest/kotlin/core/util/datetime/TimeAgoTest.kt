package core.util.datetime

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals

class TimeAgoTest {

    @Test
    fun calculateTimeAgo_withUnderOneMinute_returnsJustNow() {
        val dateTime = LocalDateTime(2026, 10, 12, 11, 59, 1)

        assertEquals(TimeAgo.JustNow, calculateTimeAgo(dateTime, CURRENT_DATE_TIME, TIME_ZONE))
    }

    @Test
    fun calculateTimeAgo_withExactlyOneMinute_returnsOneMinuteAgo() {
        val dateTime = LocalDateTime(2026, 10, 12, 11, 59, 0)

        assertEquals(TimeAgo.MinutesAgo(1), calculateTimeAgo(dateTime, CURRENT_DATE_TIME, TIME_ZONE))
    }

    @Test
    fun calculateTimeAgo_withUnderOneHour_returnsWholeMinutes() {
        val dateTime = LocalDateTime(2026, 10, 12, 11, 0, 1)

        assertEquals(TimeAgo.MinutesAgo(59), calculateTimeAgo(dateTime, CURRENT_DATE_TIME, TIME_ZONE))
    }

    @Test
    fun calculateTimeAgo_withExactlyOneHour_returnsOneHourAgo() {
        val dateTime = LocalDateTime(2026, 10, 12, 11, 0, 0)

        assertEquals(TimeAgo.HoursAgo(1), calculateTimeAgo(dateTime, CURRENT_DATE_TIME, TIME_ZONE))
    }

    @Test
    fun calculateTimeAgo_withUnderOneDay_returnsWholeHours() {
        val dateTime = LocalDateTime(2026, 10, 11, 12, 0, 1)

        assertEquals(TimeAgo.HoursAgo(23), calculateTimeAgo(dateTime, CURRENT_DATE_TIME, TIME_ZONE))
    }

    @Test
    fun calculateTimeAgo_withExactlyOneDay_returnsOneDayAgo() {
        val dateTime = LocalDateTime(2026, 10, 11, 12, 0, 0)

        assertEquals(TimeAgo.DaysAgo(1), calculateTimeAgo(dateTime, CURRENT_DATE_TIME, TIME_ZONE))
    }

    @Test
    fun calculateTimeAgo_withUnderOneWeek_returnsWholeDays() {
        val dateTime = LocalDateTime(2026, 10, 5, 12, 0, 1)

        assertEquals(TimeAgo.DaysAgo(6), calculateTimeAgo(dateTime, CURRENT_DATE_TIME, TIME_ZONE))
    }

    @Test
    fun calculateTimeAgo_withExactlyOneWeek_returnsOneWeekAgo() {
        val dateTime = LocalDateTime(2026, 10, 5, 12, 0, 0)

        assertEquals(TimeAgo.WeeksAgo(1), calculateTimeAgo(dateTime, CURRENT_DATE_TIME, TIME_ZONE))
    }

    @Test
    fun calculateTimeAgo_withUnderOneYear_returnsWholeWeeks() {
        val dateTime = LocalDateTime(2025, 10, 12, 12, 0, 1)

        assertEquals(TimeAgo.WeeksAgo(52), calculateTimeAgo(dateTime, CURRENT_DATE_TIME, TIME_ZONE))
    }

    @Test
    fun calculateTimeAgo_withOneYearOrMore_returnsPaddedAbsoluteDateTime() {
        val dateTime = LocalDateTime(2025, 3, 4, 7, 5, 0)

        assertEquals(
            TimeAgo.Absolute("04/03/2025 07:05"),
            calculateTimeAgo(dateTime, CURRENT_DATE_TIME, TIME_ZONE),
        )
    }

    private companion object {
        val CURRENT_DATE_TIME = LocalDateTime(2026, 10, 12, 12, 0, 0)
        val TIME_ZONE = TimeZone.UTC
    }
}
