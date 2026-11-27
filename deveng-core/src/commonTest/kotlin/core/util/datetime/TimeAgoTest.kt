package core.util.datetime

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

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
    fun calculateTimeAgo_withExactlyOneYear_returnsAbsoluteDateTime() {
        val dateTime = LocalDateTime(2025, 10, 12, 12, 0, 0)

        assertEquals(
            TimeAgo.Absolute("12/10/2025 12:00"),
            calculateTimeAgo(dateTime, CURRENT_DATE_TIME, TIME_ZONE),
        )
    }

    @Test
    fun calculateTimeAgo_withEachRelativeUnit_exposesTheElapsedAmount() {
        val minutesAgo = calculateTimeAgo(LocalDateTime(2026, 10, 12, 11, 55, 0), CURRENT_DATE_TIME, TIME_ZONE)
        val hoursAgo = calculateTimeAgo(LocalDateTime(2026, 10, 12, 9, 0, 0), CURRENT_DATE_TIME, TIME_ZONE)
        val daysAgo = calculateTimeAgo(LocalDateTime(2026, 10, 9, 12, 0, 0), CURRENT_DATE_TIME, TIME_ZONE)
        val weeksAgo = calculateTimeAgo(LocalDateTime(2026, 9, 21, 12, 0, 0), CURRENT_DATE_TIME, TIME_ZONE)
        val absolute = calculateTimeAgo(LocalDateTime(2024, 1, 2, 3, 4, 0), CURRENT_DATE_TIME, TIME_ZONE)

        assertEquals(5, assertIs<TimeAgo.MinutesAgo>(minutesAgo).minutes)
        assertEquals(3, assertIs<TimeAgo.HoursAgo>(hoursAgo).hours)
        assertEquals(3, assertIs<TimeAgo.DaysAgo>(daysAgo).days)
        assertEquals(3, assertIs<TimeAgo.WeeksAgo>(weeksAgo).weeks)
        assertEquals("02/01/2024 03:04", assertIs<TimeAgo.Absolute>(absolute).formattedDateTime)
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
