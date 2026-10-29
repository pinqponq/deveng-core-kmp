package core.data.upload.fake

import core.data.upload.session.BackgroundUploadFileStore

class FakeBackgroundUploadFileStore : BackgroundUploadFileStore {
    val filesByUploadId = mutableMapOf<String, ByteArray>()
    var isWritable = true
    val writtenParts = mutableListOf<WrittenPart>()

    override fun write(uploadId: String, bytes: ByteArray): String? {
        if (!isWritable) return null
        filesByUploadId[uploadId] = bytes
        return pathOf(uploadId)
    }

    override fun writePart(transferId: String, sourceFilePath: String, offset: Long, length: Long): String? {
        if (!isWritable) return null
        writtenParts += WrittenPart(transferId = transferId, sourceFilePath = sourceFilePath, offset = offset, length = length)
        filesByUploadId[transferId] = ByteArray(length.toInt())
        return pathOf(transferId)
    }

    override fun delete(uploadId: String) {
        filesByUploadId.remove(uploadId)
    }

    override fun listUploadIds(): List<String> = filesByUploadId.keys.toList()

    fun pathOf(uploadId: String): String = "$DIRECTORY/$uploadId.multipart"

    private companion object {
        const val DIRECTORY = "/tmp/uploads"
    }
}

data class WrittenPart(
    val transferId: String,
    val sourceFilePath: String,
    val offset: Long,
    val length: Long,
)
