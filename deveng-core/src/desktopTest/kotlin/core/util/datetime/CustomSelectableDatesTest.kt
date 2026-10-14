package core.util.datetime

import androidx.compose.material3.ExperimentalMaterial3Api
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalMaterial3Api::class, ExperimentalTime::class)
class CustomSelectableDatesTest {

    private val originalTimeZone = java.util.TimeZone.getDefault()

    @BeforeTest
    fun useTimeZoneWestOfUtc() {
        java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone(TIME_ZONE_WEST_OF_UTC))
    }

    @AfterTest
    fun restoreTimeZone() {
        java.util.TimeZone.setDefault(originalTimeZone)
    }

    @Test
    fun isSelectableDate_withFutureTargetWestOfUtc_acceptsTodayAndRejectsYesterday() {
        val selectableDates = CustomSelectableDates().apply { setTargetDates(TargetDates.FUTURE) }
        val today = localToday()

        assertEquals(
            listOf(true, false),
            listOf(today, today.minus(DatePeriod(days = 1))).map { date ->
                selectableDates.isSelectableDate(utcMidnightMillis(date))
            }
        )
    }

    @Test
    fun isSelectableDate_withPastTargetWestOfUtc_acceptsTodayAndRejectsTomorrow() {
        val selectableDates = CustomSelectableDates().apply { setTargetDates(TargetDates.PAST) }
        val today = localToday()

        assertEquals(
            listOf(true, false),
            listOf(today, today.plus(DatePeriod(days = 1))).map { date ->
                selectableDates.isSelectableDate(utcMidnightMillis(date))
            }
        )
    }

    @Test
    fun isSelectableDate_withDisabledDate_rejectsOnlyThatDate() {
        val today = localToday()
        val yesterday = today.minus(DatePeriod(days = 1))
        val selectableDates = CustomSelectableDates().apply {
            setTargetDates(TargetDates.PAST)
            setDisabledDates(listOf(yesterday))
        }

        assertEquals(
            listOf(false, true),
            listOf(yesterday, today).map { date -> selectableDates.isSelectableDate(utcMidnightMillis(date)) }
        )
    }

    private fun localToday(): LocalDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

    private fun utcMidnightMillis(date: LocalDate): Long = date.atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()

    private companion object {
        const val TIME_ZONE_WEST_OF_UTC = "America/New_York"
    }
}
