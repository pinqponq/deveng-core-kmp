package core.util.datetime

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalMaterial3Api::class, ExperimentalTime::class)
class CustomSelectableDates : SelectableDates {
    private var targetDates = TargetDates.FUTURE

    private var disabledDates: Set<LocalDate> = emptySet()

    private val oldestDateTime = LocalDateTime(
        year = 1900,
        month = 1,
        day = 1,
        hour = 0,
        minute = 0
    )

    fun setTargetDates(targetDates: TargetDates) {
        this.targetDates = targetDates
    }

    fun setDisabledDates(dates: List<LocalDate>) {
        this.disabledDates = dates.toSet()
    }

    override fun isSelectableDate(utcTimeMillis: Long): Boolean {
        val timeZone = TimeZone.currentSystemDefault()
        val todayDate = Clock.System.now().toLocalDateTime(timeZone).date
        val localDate = Instant.fromEpochMilliseconds(utcTimeMillis)
            .toLocalDateTime(TimeZone.UTC).date

        val passesTargetDateCheck = when (targetDates) {
            TargetDates.PAST -> oldestDateTime.date <= localDate && localDate <= todayDate
            TargetDates.FUTURE -> localDate >= todayDate
        }


        val isNotDisabled = !disabledDates.contains(localDate)

        return passesTargetDateCheck && isNotDisabled
    }

    override fun isSelectableYear(year: Int): Boolean {
        val timeZone = TimeZone.currentSystemDefault()
        val currentYear = Clock.System.now().toLocalDateTime(timeZone).year

        val isTargetYear = when (targetDates) {
            TargetDates.PAST -> oldestDateTime.year <= year && year <= currentYear
            TargetDates.FUTURE -> year >= currentYear
        }

        return isTargetYear
    }
}