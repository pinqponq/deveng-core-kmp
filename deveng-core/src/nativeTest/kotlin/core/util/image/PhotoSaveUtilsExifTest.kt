package core.util.image

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalEncodingApi::class)
class PhotoSaveUtilsExifTest {

    @Test
    fun readLocationFromExif_withGpsInTheJpeg_returnsSignedCoordinates() {
        val location = PhotoSaveUtils.readLocationFromExif(JPEG_WITH_GPS_AND_CAPTURE_TIME)

        assertEquals(SYDNEY_LATITUDE to SYDNEY_LONGITUDE, location)
    }

    @Test
    fun readLocationFromExif_withoutGps_returnsNull() {
        assertNull(PhotoSaveUtils.readLocationFromExif(JPEG_WITHOUT_METADATA))
    }

    @Test
    fun readCaptureDateTimeFromExif_withDateTimeOriginal_returnsItWithItsOffset() {
        val captureDateTime = PhotoSaveUtils.readCaptureDateTimeFromExif(JPEG_WITH_GPS_AND_CAPTURE_TIME)

        assertEquals(
            ExifCaptureDateTime(
                year = 2026,
                month = 9,
                day = 15,
                hour = 10,
                minute = 30,
                second = 0,
                offsetMinutes = PLUS_THREE_HOURS_IN_MINUTES,
            ),
            captureDateTime,
        )
    }

    @Test
    fun readCaptureDateTimeFromExif_withoutExifDate_returnsNull() {
        assertNull(PhotoSaveUtils.readCaptureDateTimeFromExif(JPEG_WITHOUT_METADATA))
    }

    private companion object {
        const val SYDNEY_LATITUDE = -33.8688
        const val SYDNEY_LONGITUDE = 151.2093
        const val PLUS_THREE_HOURS_IN_MINUTES = 180

        // 8x8 JPEGs written with ImageIO: GPS 33.8688 S / 151.2093 E, DateTimeOriginal
        // 2026:09:15 10:30:00, OffsetTimeOriginal +03:00; and the same image without metadata.
        val JPEG_WITH_GPS_AND_CAPTURE_TIME = Base64.decode(
            "/9j/4AAQSkZJRgABAQAASABIAAD/4QDmRXhpZgAATU0AKgAAAAgAAodpAAQAAAABAAAAJoglAAQAAAABAAAAeAAAAAAABJAD" +
            "AAIAAAAUAAAAXJARAAIAAAAHAAAAcKACAAQAAAABAAAACKADAAQAAAABAAAACAAAAAAyMDI2OjA5OjE1IDEwOjMwOjAwACsw" +
            "MzowMAAAAAQAAQACAAAAAlMAAAAAAgAFAAAAAwAAAK4AAwACAAAAAkUAAAAABAAFAAAAAwAAAMYAAAAAAAAAIQAAAAEAAAA0" +
            "AAAAAQAAAwAAAABkAAAAlwAAAAEAAAAMAAAAAQAADRQAAABk/+0AOFBob3Rvc2hvcCAzLjAAOEJJTQQEAAAAAAAAOEJJTQQl" +
            "AAAAAAAQ1B2M2Y8AsgTpgAmY7PhCfv/AABEIAAgACAMBIgACEQEDEQH/xAAfAAABBQEBAQEBAQAAAAAAAAAAAQIDBAUGBwgJ" +
            "Cgv/xAC1EAACAQMDAgQDBQUEBAAAAX0BAgMABBEFEiExQQYTUWEHInEUMoGRoQgjQrHBFVLR8CQzYnKCCQoWFxgZGiUmJygp" +
            "KjQ1Njc4OTpDREVGR0hJSlNUVVZXWFlaY2RlZmdoaWpzdHV2d3h5eoOEhYaHiImKkpOUlZaXmJmaoqOkpaanqKmqsrO0tba3" +
            "uLm6wsPExcbHyMnK0tPU1dbX2Nna4eLj5OXm5+jp6vHy8/T19vf4+fr/xAAfAQADAQEBAQEBAQEBAAAAAAAAAQIDBAUGBwgJ" +
            "Cgv/xAC1EQACAQIEBAMEBwUEBAABAncAAQIDEQQFITEGEkFRB2FxEyIygQgUQpGhscEJIzNS8BVictEKFiQ04SXxFxgZGiYn" +
            "KCkqNTY3ODk6Q0RFRkdISUpTVFVWV1hZWmNkZWZnaGlqc3R1dnd4eXqCg4SFhoeIiYqSk5SVlpeYmZqio6Slpqeoqaqys7S1" +
            "tre4ubrCw8TFxsfIycrS09TV1tfY2dri4+Tl5ufo6ery8/T19vf4+fr/2wBDAAICAgICAgMCAgMFAwMDBQYFBQUFBggGBgYG" +
            "BggKCAgICAgICgoKCgoKCgoMDAwMDAwODg4ODg8PDw8PDw8PDw//2wBDAQICAgQEBAcEBAcQCwkLEBAQEBAQEBAQEBAQEBAQ" +
            "EBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBD/3QAEAAH/2gAMAwEAAhEDEQA/APhuiiivxs/qQ//Z",
        )
        val JPEG_WITHOUT_METADATA = Base64.decode(
            "/9j/4AAQSkZJRgABAQAASABIAAD/4QBARXhpZgAATU0AKgAAAAgAAYdpAAQAAAABAAAAGgAAAAAAAqACAAQAAAABAAAACKAD" +
            "AAQAAAABAAAACAAAAAD/wAARCAAIAAgDASIAAhEBAxEB/8QAHwAAAQUBAQEBAQEAAAAAAAAAAAECAwQFBgcICQoL/8QAtRAA" +
            "AgEDAwIEAwUFBAQAAAF9AQIDAAQRBRIhMUEGE1FhByJxFDKBkaEII0KxwRVS0fAkM2JyggkKFhcYGRolJicoKSo0NTY3ODk6" +
            "Q0RFRkdISUpTVFVWV1hZWmNkZWZnaGlqc3R1dnd4eXqDhIWGh4iJipKTlJWWl5iZmqKjpKWmp6ipqrKztLW2t7i5usLDxMXG" +
            "x8jJytLT1NXW19jZ2uHi4+Tl5ufo6erx8vP09fb3+Pn6/8QAHwEAAwEBAQEBAQEBAQAAAAAAAAECAwQFBgcICQoL/8QAtREA" +
            "AgECBAQDBAcFBAQAAQJ3AAECAxEEBSExBhJBUQdhcRMiMoEIFEKRobHBCSMzUvAVYnLRChYkNOEl8RcYGRomJygpKjU2Nzg5" +
            "OkNERUZHSElKU1RVVldYWVpjZGVmZ2hpanN0dXZ3eHl6goOEhYaHiImKkpOUlZaXmJmaoqOkpaanqKmqsrO0tba3uLm6wsPE" +
            "xcbHyMnK0tPU1dbX2Nna4uPk5ebn6Onq8vP09fb3+Pn6/9sAQwACAgICAgIDAgIDBQMDAwUGBQUFBQYIBgYGBgYICggICAgI" +
            "CAoKCgoKCgoKDAwMDAwMDg4ODg4PDw8PDw8PDw8P/9sAQwECAgIEBAQHBAQHEAsJCxAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQ" +
            "EBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQ/90ABAAB/9oADAMBAAIRAxEAPwD4booor8bP6kP/2Q==",
        )
    }
}
