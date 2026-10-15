package core.util.image

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.datetime.TimeZone

class ExifDateTimeTagsTest {

    @Test
    fun buildExifDateTimeTags_inPositiveOffsetZone_writesLocalWallClockAndOffset() {
        val tags = buildExifDateTimeTags(epochMillis = CAPTURE_EPOCH_MILLIS, timeZone = TimeZone.of("Europe/Istanbul"))

        assertEquals(ExifDateTimeTags(dateTime = "2026:08:16 02:30:05", offset = "+03:00"), tags)
    }

    @Test
    fun buildExifDateTimeTags_inNegativeHalfHourOffsetZone_writesSignedOffsetWithMinutes() {
        val tags = buildExifDateTimeTags(epochMillis = CAPTURE_EPOCH_MILLIS, timeZone = TimeZone.of("America/St_Johns"))

        assertEquals(ExifDateTimeTags(dateTime = "2026:08:15 21:00:05", offset = "-02:30"), tags)
    }

    @Test
    fun buildExifDateTimeTags_inUtc_writesZeroOffset() {
        val tags = buildExifDateTimeTags(epochMillis = SINGLE_DIGIT_FIELDS_EPOCH_MILLIS, timeZone = TimeZone.UTC)

        assertEquals(ExifDateTimeTags(dateTime = "2026:01:02 03:04:05", offset = "+00:00"), tags)
    }

    @Test
    fun buildExifDateTimeTags_parsedBack_returnsSameCaptureTime() {
        val tags = buildExifDateTimeTags(epochMillis = CAPTURE_EPOCH_MILLIS, timeZone = TimeZone.of("Asia/Kolkata"))

        val parsed = parseExifDateTime(raw = tags.dateTime, offsetRaw = tags.offset)

        assertEquals(
            ExifCaptureDateTime(year = 2026, month = 8, day = 16, hour = 5, minute = 0, second = 5, offsetMinutes = 330),
            parsed,
        )
    }

    private companion object {
        // 2026-08-15T23:30:05Z
        const val CAPTURE_EPOCH_MILLIS = 1_786_836_605_000L

        // 2026-01-02T03:04:05Z
        const val SINGLE_DIGIT_FIELDS_EPOCH_MILLIS = 1_767_323_045_000L
    }
}
