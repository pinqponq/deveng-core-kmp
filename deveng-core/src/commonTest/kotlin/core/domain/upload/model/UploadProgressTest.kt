package core.domain.upload.model

import kotlin.test.Test
import kotlin.test.assertEquals

class UploadProgressTest {

    @Test
    fun fraction_isSentShareOfTotal() {
        assertEquals(0.25f, progress(bytesSent = 25, bytesTotal = 100).fraction)
    }

    @Test
    fun fraction_withUnknownTotal_isZero() {
        assertEquals(0f, progress(bytesSent = 25, bytesTotal = 0).fraction)
    }

    @Test
    fun fraction_withMoreSentThanTotal_isCappedAtOne() {
        assertEquals(1f, progress(bytesSent = 150, bytesTotal = 100).fraction)
    }

    private fun progress(bytesSent: Long, bytesTotal: Long) =
        UploadProgress(uploadId = UPLOAD_ID, bytesSent = bytesSent, bytesTotal = bytesTotal)

    private companion object {
        const val UPLOAD_ID = "upload-1"
    }
}
