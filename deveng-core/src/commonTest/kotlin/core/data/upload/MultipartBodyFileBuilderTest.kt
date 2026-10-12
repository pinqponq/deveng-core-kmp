package core.data.upload

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class MultipartBodyFileBuilderTest {

    private val builder = MultipartBodyFileBuilder(boundary = BOUNDARY)

    @Test
    fun build_writesFormFieldsBeforeMediaPartAndClosesBoundary() {
        val body = builder.build(
            mediaFileName = MEDIA_FILE_NAME,
            mediaContentType = MEDIA_CONTENT_TYPE,
            mediaBytes = MEDIA_TEXT.encodeToByteArray(),
            formFields = mapOf("CapturedAt" to CAPTURED_AT, "Caption" to CAPTION),
        )

        assertEquals(EXPECTED_BODY, body.bytes.decodeToString())
        assertEquals("multipart/form-data; boundary=$BOUNDARY", body.contentType)
        assertEquals(EXPECTED_BODY.encodeToByteArray().size.toLong(), body.contentLength)
    }

    @Test
    fun multipartBody_comparesBytesByContent() {
        val body = MultipartBody(bytes = MEDIA_TEXT.encodeToByteArray(), boundary = BOUNDARY)

        assertEquals(body, MultipartBody(bytes = MEDIA_TEXT.encodeToByteArray(), boundary = BOUNDARY))
        assertEquals(body.hashCode(), MultipartBody(bytes = MEDIA_TEXT.encodeToByteArray(), boundary = BOUNDARY).hashCode())
        assertNotEquals(body, MultipartBody(bytes = CAPTION.encodeToByteArray(), boundary = BOUNDARY))
        assertNotEquals(body, MultipartBody(bytes = MEDIA_TEXT.encodeToByteArray(), boundary = OTHER_BOUNDARY))
    }

    private companion object {
        const val BOUNDARY = "TestBoundary"
        const val OTHER_BOUNDARY = "OtherBoundary"
        const val MEDIA_FILE_NAME = "widget.jpg"
        const val MEDIA_CONTENT_TYPE = "image/jpeg"
        const val MEDIA_TEXT = "jpeg-bytes"
        const val CAPTURED_AT = "2026-10-12T10:00:00Z"
        const val CAPTION = "hello"
        const val EXPECTED_BODY = "--TestBoundary\r\n" +
            "Content-Disposition: form-data; name=\"CapturedAt\"\r\n\r\n" +
            "2026-10-12T10:00:00Z\r\n" +
            "--TestBoundary\r\n" +
            "Content-Disposition: form-data; name=\"Caption\"\r\n\r\n" +
            "hello\r\n" +
            "--TestBoundary\r\n" +
            "Content-Disposition: form-data; name=\"File\"; filename=\"widget.jpg\"\r\n" +
            "Content-Type: image/jpeg\r\n\r\n" +
            "jpeg-bytes\r\n" +
            "--TestBoundary--\r\n"
    }
}
