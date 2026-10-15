package core.util.image

import kotlin.math.abs
import kotlin.time.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.format.Padding
import kotlinx.datetime.format.char
import kotlinx.datetime.offsetAt
import kotlinx.datetime.toLocalDateTime

/**
 * EXIF `DateTimeOriginal` (`"yyyy:MM:dd HH:mm:ss"`, local wall-clock) and `OffsetTimeOriginal`
 * (`"+03:00"`) values for one capture instant. The reverse of [parseExifDateTime].
 */
internal data class ExifDateTimeTags(
    val dateTime: String,
    val offset: String,
)

private val exifDateTimeFormat = LocalDateTime.Format {
    year()
    char(':')
    monthNumber(padding = Padding.ZERO)
    char(':')
    day(padding = Padding.ZERO)
    char(' ')
    hour(padding = Padding.ZERO)
    char(':')
    minute(padding = Padding.ZERO)
    char(':')
    second(padding = Padding.ZERO)
}

internal fun buildExifDateTimeTags(
    epochMillis: Long,
    timeZone: TimeZone,
): ExifDateTimeTags {
    val instant = Instant.fromEpochMilliseconds(epochMillis)
    val dateTime = instant.toLocalDateTime(timeZone).format(exifDateTimeFormat)
    val offsetMinutes = timeZone.offsetAt(instant).totalSeconds / SECONDS_PER_MINUTE
    return ExifDateTimeTags(dateTime = dateTime, offset = formatExifOffset(offsetMinutes))
}

private fun formatExifOffset(offsetMinutes: Int): String {
    val sign = if (offsetMinutes < 0) "-" else "+"
    val absoluteMinutes = abs(offsetMinutes)
    val hours = (absoluteMinutes / MINUTES_PER_HOUR).toString().padStart(OFFSET_PART_DIGITS, '0')
    val minutes = (absoluteMinutes % MINUTES_PER_HOUR).toString().padStart(OFFSET_PART_DIGITS, '0')
    return "$sign$hours:$minutes"
}

private const val SECONDS_PER_MINUTE = 60
private const val MINUTES_PER_HOUR = 60
private const val OFFSET_PART_DIGITS = 2
