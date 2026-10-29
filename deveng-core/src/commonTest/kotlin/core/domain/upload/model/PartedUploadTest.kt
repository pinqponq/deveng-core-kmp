package core.domain.upload.model

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PartedUploadTest {

    @Test
    fun partCount_withRemainder_countsTheShorterLastPart() {
        assertEquals(3, partedUpload(totalBytes = 25).partCount)
    }

    @Test
    fun partCount_withExactMultiple_hasNoEmptyLastPart() {
        assertEquals(2, partedUpload(totalBytes = 20).partCount)
    }

    @Test
    fun partCount_withFileSmallerThanOnePart_isOne() {
        assertEquals(1, partedUpload(totalBytes = 1).partCount)
    }

    @Test
    fun byteRangeOf_middleAndLastPart_coverTheFileWithoutGaps() {
        val upload = partedUpload(totalBytes = 25)

        assertEquals(PartByteRange(offset = 0, length = 10), upload.byteRangeOf(1))
        assertEquals(PartByteRange(offset = 10, length = 10), upload.byteRangeOf(2))
        assertEquals(PartByteRange(offset = 20, length = 5), upload.byteRangeOf(3))
    }

    @Test
    fun byteRangeOf_partOutsideTheFile_throws() {
        val upload = partedUpload(totalBytes = 25)

        assertFailsWith<IllegalArgumentException> { upload.byteRangeOf(0) }
        assertFailsWith<IllegalArgumentException> { upload.byteRangeOf(4) }
    }

    @Test
    fun nextPendingPartNumber_skipsStoredParts() {
        val upload = partedUpload(totalBytes = 25)
            .withStoredPart(StoredPart(partNumber = 1, entityTag = "a"))
            .withStoredPart(StoredPart(partNumber = 3, entityTag = "c"))

        assertEquals(2, upload.nextPendingPartNumber())
    }

    @Test
    fun nextPendingPartNumber_whenEveryPartIsStored_isNull() {
        val upload = partedUpload(totalBytes = 15)
            .withStoredPart(StoredPart(partNumber = 2, entityTag = "b"))
            .withStoredPart(StoredPart(partNumber = 1, entityTag = "a"))

        assertNull(upload.nextPendingPartNumber())
    }

    @Test
    fun withStoredPart_keepsPartOrderAndReplacesAPartStoredAgain() {
        val upload = partedUpload(totalBytes = 25)
            .withStoredPart(StoredPart(partNumber = 2, entityTag = "b"))
            .withStoredPart(StoredPart(partNumber = 1, entityTag = "a"))
            .withStoredPart(StoredPart(partNumber = 2, entityTag = "b2"))

        assertEquals(
            listOf(StoredPart(partNumber = 1, entityTag = "a"), StoredPart(partNumber = 2, entityTag = "b2")),
            upload.storedParts,
        )
    }

    @Test
    fun withStoredPart_partOutsideTheFile_throws() {
        assertFailsWith<IllegalArgumentException> {
            partedUpload(totalBytes = 25).withStoredPart(StoredPart(partNumber = 4, entityTag = "d"))
        }
    }

    @Test
    fun isStored_tellsStoredAndPendingPartsApart() {
        val upload = partedUpload(totalBytes = 25).withStoredPart(StoredPart(partNumber = 2, entityTag = "b"))

        assertTrue(upload.isStored(2))
        assertFalse(upload.isStored(1))
    }

    @Test
    fun storedBytes_countsTheShorterLastPartByItsOwnLength() {
        val upload = partedUpload(totalBytes = 25)
            .withStoredPart(StoredPart(partNumber = 1, entityTag = "a"))
            .withStoredPart(StoredPart(partNumber = 3, entityTag = "c"))

        assertEquals(15, upload.storedBytes)
    }

    @Test
    fun constructor_withoutBytesOrWithoutPartSize_throws() {
        assertFailsWith<IllegalArgumentException> { partedUpload(totalBytes = 0) }
        assertFailsWith<IllegalArgumentException> { partedUpload(totalBytes = 10, partSizeBytes = 0) }
    }

    @Test
    fun descriptor_storedBeforePartedUploadsExisted_decodesWithoutParts() {
        val storedJson = """{"uploadId":"u1","mediaFileName":"a.jpg","isVideo":false,"contentType":"image/jpeg","mediaId":7}"""

        val descriptor = Json.decodeFromString<BackgroundUploadDescriptor>(storedJson)

        assertEquals(7, descriptor.remoteId)
        assertNull(descriptor.partedUpload)
    }

    @Test
    fun descriptor_withStoredParts_survivesEncoding() {
        val descriptor = BackgroundUploadDescriptor(
            uploadId = "u1",
            mediaFileName = "clip.mp4",
            isVideo = true,
            contentType = "video/mp4",
            partedUpload = partedUpload(totalBytes = 25).withStoredPart(StoredPart(partNumber = 1, entityTag = "\"etag-1\"")),
        )

        assertEquals(descriptor, Json.decodeFromString<BackgroundUploadDescriptor>(Json.encodeToString(descriptor)))
    }

    private fun partedUpload(totalBytes: Long, partSizeBytes: Long = PART_SIZE_BYTES) = PartedUpload(
        remoteKey = "session-1",
        filePath = "/videos/clip.mp4",
        totalBytes = totalBytes,
        partSizeBytes = partSizeBytes,
    )

    private companion object {
        const val PART_SIZE_BYTES = 10L
    }
}
