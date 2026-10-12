package core.util.datetime

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.format.Padding
import kotlinx.datetime.format.char
import kotlinx.datetime.toInstant
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.ExperimentalTime

private val slashDateTimeFormat = LocalDateTime.Format {
    day(padding = Padding.ZERO)
    char('/')
    monthNumber(padding = Padding.ZERO)
    char('/')
    year()
    char(' ')
    hour(padding = Padding.ZERO)
    char(':')
    minute(padding = Padding.ZERO)
}

private const val DAYS_PER_WEEK = 7
private const val DAYS_PER_YEAR = 365

/**
 * How long ago a moment was, in the largest unit that fits.
 * The label itself is formatted by the caller with a localized string resource.
 */
sealed class TimeAgo {
    data object JustNow : TimeAgo()
    data class MinutesAgo(val minutes: Long) : TimeAgo()
    data class HoursAgo(val hours: Long) : TimeAgo()
    data class DaysAgo(val days: Long) : TimeAgo()
    data class WeeksAgo(val weeks: Long) : TimeAgo()
    data class Absolute(val formattedDateTime: String) : TimeAgo()
}

/**
 * Calculates the [TimeAgo] between [dateTime] and [currentDateTime].
 * Under a minute is [TimeAgo.JustNow]; a year or more is [TimeAgo.Absolute] as `dd/MM/yyyy HH:mm`.
 *
 * @param dateTime the past moment.
 * @param currentDateTime the moment to measure from.
 * @param timeZone the time zone both values are in.
 */
@OptIn(ExperimentalTime::class)
fun calculateTimeAgo(
    dateTime: LocalDateTime,
    currentDateTime: LocalDateTime,
    timeZone: TimeZone,
): TimeAgo {
    val elapsedDuration = currentDateTime.toInstant(timeZone) - dateTime.toInstant(timeZone)

    return when {
        elapsedDuration < 1.minutes -> TimeAgo.JustNow
        elapsedDuration < 1.hours -> TimeAgo.MinutesAgo(elapsedDuration.inWholeMinutes)
        elapsedDuration < 1.days -> TimeAgo.HoursAgo(elapsedDuration.inWholeHours)
        elapsedDuration < DAYS_PER_WEEK.days -> TimeAgo.DaysAgo(elapsedDuration.inWholeDays)
        elapsedDuration < DAYS_PER_YEAR.days -> TimeAgo.WeeksAgo(elapsedDuration.inWholeDays / DAYS_PER_WEEK)
        else -> TimeAgo.Absolute(dateTime.format(slashDateTimeFormat))
    }
}
