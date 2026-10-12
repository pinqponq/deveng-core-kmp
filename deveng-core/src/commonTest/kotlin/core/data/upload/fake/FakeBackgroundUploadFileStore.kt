package core.data.upload.fake

import core.data.upload.session.BackgroundUploadFileStore

class FakeBackgroundUploadFileStore : BackgroundUploadFileStore {
    val filesByUploadId = mutableMapOf<String, ByteArray>()
    var isWritable = true

    override fun write(uploadId: String, bytes: ByteArray): String? {
        if (!isWritable) return null
        filesByUploadId[uploadId] = bytes
        return pathOf(uploadId)
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
