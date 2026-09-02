package core.util.image

/**
 * Result of saving a photo to disk.
 * @property path Absolute path of the saved file on success.
 */
sealed class SavePhotoResult {
    data class Success(val path: String) : SavePhotoResult()
    data class Error(val exception: Throwable) : SavePhotoResult()
}

/**
 * A capture timestamp read from EXIF. The date/time fields are the local wall-clock the camera recorded
 * (EXIF `DateTimeOriginal` carries no timezone). [offsetMinutes] is the UTC offset from `OffsetTimeOriginal`
 * when the file carried one, otherwise `null` — recover the true instant by interpreting the wall-clock at
 * [offsetMinutes] (or a caller-supplied fallback offset such as the device's current offset).
 *
 * @property month 1-12.
 * @property day 1-31.
 * @property offsetMinutes UTC offset in minutes (e.g. `+09:00` → 540), or `null` when the file had no offset tag.
 */
data class ExifCaptureDateTime(
    val year: Int,
    val month: Int,
    val day: Int,
    val hour: Int,
    val minute: Int,
    val second: Int,
    val offsetMinutes: Int?,
)

/**
 * Platform-specific utilities for saving photo bytes to disk and adding location EXIF.
 * Use after capturing: capture → addLocationExif(bytes, lat, lon) → savePhoto(bytes, path).
 */
expect object PhotoSaveUtils {

    /**
     * Optional: call once with the application context so that on Android the saved photo
     * is notified to MediaStore and appears in the system Gallery. No-op on other platforms.
     */
    fun setApplicationContext(context: Any?)

    /**
     * Writes [imageBytes] to [targetPath]. Creates parent directories if needed.
     * On Android, if [setApplicationContext] was called, notifies MediaStore so the photo appears in the Gallery.
     *
     * @param imageBytes JPEG or PNG image bytes (e.g. from [ImageCaptureResult.Success.byteArray]).
     * @param targetPath Absolute path where the file should be written.
     * @return [SavePhotoResult.Success] with the path, or [SavePhotoResult.Error] on failure.
     */
    fun savePhoto(imageBytes: ByteArray, targetPath: String): SavePhotoResult

    /**
     * Returns image bytes with orientation normalized: EXIF orientation is applied to pixels
     * and the result has orientation = normal. Use before saving so the saved file displays
     * correctly in all viewers (e.g. when camera returns EXIF 6 = 90° but pixels are not rotated).
     * On unsupported platforms or on error, returns [imageBytes] unchanged.
     */
    fun imageBytesWithNormalOrientation(imageBytes: ByteArray): ByteArray

    /**
     * Returns a copy of [imageBytes] with GPS location EXIF tags set.
     * Preserves existing EXIF (e.g. orientation). Best used with JPEG bytes.
     *
     * @param imageBytes JPEG (or PNG) image bytes.
     * @param latitude Latitude in decimal degrees (e.g. 41.0082).
     * @param longitude Longitude in decimal degrees (e.g. 28.9784).
     * @return New byte array with location EXIF added; or [imageBytes] unchanged on unsupported platform or error.
     */
    fun addLocationExif(
        imageBytes: ByteArray,
        latitude: Double,
        longitude: Double,
    ): ByteArray

    /**
     * Reads GPS latitude/longitude from JPEG (or compatible) [imageBytes] if present.
     * Use for upload metadata: prefer capture-time location embedded with [addLocationExif]
     * instead of a fresh GPS fix at swipe/upload time.
     *
     * @return `(latitude, longitude)` in decimal degrees, or `null` if missing or unsupported.
     */
    fun readLocationFromExif(imageBytes: ByteArray): Pair<Double, Double>?

    /**
     * Reads the original capture date/time from EXIF (`DateTimeOriginal`, falling back to `DateTime`) plus
     * its `OffsetTimeOriginal` if present. Use for collage/timeline metadata: prefer the moment the content
     * was actually captured over the upload time.
     *
     * @param imageBytes JPEG (or compatible) image bytes to read the EXIF metadata from.
     * @return the parsed [ExifCaptureDateTime] (local wall-clock plus optional offset), or `null` if the tag
     *   is missing, malformed, or the platform is unsupported.
     */
    fun readCaptureDateTimeFromExif(imageBytes: ByteArray): ExifCaptureDateTime?
}

/**
 * Parses an EXIF date/time string (`"yyyy:MM:dd HH:mm:ss"`, the `-` date separator also tolerated) together
 * with an optional EXIF offset string into an [ExifCaptureDateTime]. Returns `null` when [raw] is absent or
 * malformed. Shared by the platform actuals so the parsing lives in one place.
 *
 * @param raw the EXIF `DateTimeOriginal`/`DateTime` string, e.g. `"2026:08:15 23:30:05"`.
 * @param offsetRaw the EXIF `OffsetTimeOriginal` string, e.g. `"+09:00"`, or `null` when absent.
 */
internal fun parseExifDateTime(raw: String?, offsetRaw: String?): ExifCaptureDateTime? {
    val value = raw?.trim().orEmpty()
    if (value.isEmpty()) return null
    val dateAndTime = value.split(' ')
    if (dateAndTime.size != 2) return null
    val dateParts = dateAndTime[0].split(':', '-')
    val timeParts = dateAndTime[1].split(':')
    if (dateParts.size != 3 || timeParts.size < 3) return null

    val year = dateParts[0].toIntOrNull() ?: return null
    val month = dateParts[1].toIntOrNull() ?: return null
    val day = dateParts[2].toIntOrNull() ?: return null
    val hour = timeParts[0].toIntOrNull() ?: return null
    val minute = timeParts[1].toIntOrNull() ?: return null
    // Some cameras append fractional seconds ("05.12"); keep the whole-second part only.
    val second = timeParts[2].substringBefore('.').toIntOrNull() ?: return null

    val isWithinCalendarBounds = year in 1..9999 && month in 1..12 && day in 1..31 &&
        hour in 0..23 && minute in 0..59 && second in 0..60
    if (!isWithinCalendarBounds) return null
    // EXIF fills unset date/time with zeros ("0000:00:00 00:00:00"); treat that as "no capture time".
    if (year == 0) return null

    return ExifCaptureDateTime(
        year = year,
        month = month,
        day = day,
        hour = hour,
        minute = minute,
        second = second,
        offsetMinutes = parseExifOffsetMinutes(offsetRaw),
    )
}

/**
 * Parses an EXIF offset string (`"+09:00"`, `"-05:30"`, or `"Z"`) into total minutes, or `null` when the
 * value is absent, blank, or malformed.
 *
 * @param raw the EXIF `OffsetTimeOriginal` string, or `null`.
 */
internal fun parseExifOffsetMinutes(raw: String?): Int? {
    val value = raw?.trim().orEmpty()
    if (value.isEmpty()) return null
    if (value.equals("Z", ignoreCase = true)) return 0

    val sign = when (value.first()) {
        '+' -> 1
        '-' -> -1
        else -> return null
    }
    val hoursAndMinutes = value.drop(1).split(':')
    if (hoursAndMinutes.size != 2) return null
    val hours = hoursAndMinutes[0].toIntOrNull() ?: return null
    val minutes = hoursAndMinutes[1].toIntOrNull() ?: return null
    if (hours !in 0..14 || minutes !in 0..59) return null

    return sign * (hours * 60 + minutes)
}
