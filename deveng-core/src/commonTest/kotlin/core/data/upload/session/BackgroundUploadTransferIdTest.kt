package core.data.upload.session

import kotlin.test.Test
import kotlin.test.assertEquals

class BackgroundUploadTransferIdTest {

    @Test
    fun value_withoutPart_isTheUploadId() {
        assertEquals("video-1", BackgroundUploadTransferId(uploadId = "video-1").value)
    }

    @Test
    fun value_withPart_appendsThePartNumber() {
        assertEquals("video-1~part12", BackgroundUploadTransferId(uploadId = "video-1", partNumber = 12).value)
    }

    @Test
    fun parse_partTransfer_returnsUploadIdAndPart() {
        assertEquals(BackgroundUploadTransferId(uploadId = "video-1", partNumber = 3), BackgroundUploadTransferId.parse("video-1~part3"))
    }

    @Test
    fun parse_uploadIdWithoutPart_returnsItWhole() {
        assertEquals(BackgroundUploadTransferId(uploadId = "photo-1"), BackgroundUploadTransferId.parse("photo-1"))
    }

    @Test
    fun parse_separatorWithoutAPositivePartNumber_isAPlainUploadId() {
        listOf("video-1~part", "video-1~partx", "video-1~part0", "video-1~part-2", "~part3").forEach { value ->
            assertEquals(BackgroundUploadTransferId(uploadId = value), BackgroundUploadTransferId.parse(value), value)
        }
    }

    @Test
    fun parse_usesTheLastSeparator() {
        assertEquals(
            BackgroundUploadTransferId(uploadId = "a~part1", partNumber = 2),
            BackgroundUploadTransferId.parse("a~part1~part2"),
        )
    }
}
