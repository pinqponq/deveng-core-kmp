package core.util.datetime

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
fun LocalDateTime.minus(
    value: Long,
    unit: DateTimeUnit,
    timeZone: TimeZone = TimeZone.currentSystemDefault()
): LocalDateTime {
    val instant = this.toInstant(timeZone)
    val newInstant = instant.minus(value = value, unit = unit, timeZone = timeZone)

    return newInstant.toLocalDateTime(timeZone)
}

@OptIn(ExperimentalTime::class)
fun LocalDate.toEpochMillis(
    zone: TimeZone = TimeZone.currentSystemDefault()
): Long = atStartOfDayIn(zone).toEpochMilliseconds()