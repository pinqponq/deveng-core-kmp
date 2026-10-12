package core.util.datetime

/**
 * A remaining duration split into hours, minutes and seconds for a countdown label.
 * Hours are not wrapped into days, so they can exceed 24.
 */
data class CountdownParts(
    val hours: Long,
    val minutes: Long,
    val seconds: Long,
)

/**
 * Splits a remaining duration into [CountdownParts]; a negative duration counts as zero.
 * The label itself is formatted by the caller with a localized string resource.
 *
 * @param remainingMillis remaining time in milliseconds.
 */
fun splitIntoCountdownParts(remainingMillis: Long): CountdownParts {
    val totalSeconds = remainingMillis.coerceAtLeast(0L) / MILLIS_PER_SECOND
    return CountdownParts(
        hours = totalSeconds / SECONDS_PER_HOUR,
        minutes = (totalSeconds % SECONDS_PER_HOUR) / SECONDS_PER_MINUTE,
        seconds = totalSeconds % SECONDS_PER_MINUTE,
    )
}

private const val MILLIS_PER_SECOND = 1_000L
private const val SECONDS_PER_MINUTE = 60L
private const val SECONDS_PER_HOUR = 3_600L
