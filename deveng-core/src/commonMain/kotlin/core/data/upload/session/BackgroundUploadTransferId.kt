package core.data.upload.session

private const val PART_SEPARATOR = "~part"

/**
 * The id a transfer is created with. A whole-file upload uses its upload id. A part of a parted
 * upload uses `<uploadId>~part<partNumber>`, so the transfer service can report each part on its
 * own while body files and completions still map back to the upload.
 */
data class BackgroundUploadTransferId(
    val uploadId: String,
    val partNumber: Int? = null,
) {
    val value: String get() = if (partNumber == null) uploadId else "$uploadId$PART_SEPARATOR$partNumber"

    companion object {
        fun parse(value: String): BackgroundUploadTransferId {
            val separatorIndex = value.lastIndexOf(PART_SEPARATOR)
            if (separatorIndex <= 0) return BackgroundUploadTransferId(uploadId = value)
            val partNumber = value.substring(separatorIndex + PART_SEPARATOR.length).toIntOrNull()
            val isPartTransfer = partNumber != null && partNumber > 0
            if (!isPartTransfer) return BackgroundUploadTransferId(uploadId = value)
            return BackgroundUploadTransferId(uploadId = value.substring(0, separatorIndex), partNumber = partNumber)
        }
    }
}
