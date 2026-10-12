package core.data.upload

private const val CRLF = "\r\n"

data class MultipartBody(
    val bytes: ByteArray,
    val boundary: String,
) {
    val contentType: String get() = "multipart/form-data; boundary=$boundary"
    val contentLength: Long get() = bytes.size.toLong()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MultipartBody) return false
        return boundary == other.boundary && bytes.contentEquals(other.bytes)
    }

    override fun hashCode(): Int = 31 * bytes.contentHashCode() + boundary.hashCode()
}

/**
 * Builds a whole `multipart/form-data` body in memory, so it can be written to a file and handed to
 * a background transfer that reads its body from disk (iOS background URL sessions only upload files).
 */
class MultipartBodyFileBuilder(
    private val boundary: String,
    private val mediaFieldName: String,
) {

    fun build(
        mediaFileName: String,
        mediaContentType: String,
        mediaBytes: ByteArray,
        formFields: Map<String, String>,
    ): MultipartBody {
        val chunks = mutableListOf<ByteArray>()

        formFields.forEach { (name, value) ->
            chunks += "--$boundary$CRLF".encodeToByteArray()
            chunks += "Content-Disposition: form-data; name=\"$name\"$CRLF".encodeToByteArray()
            chunks += CRLF.encodeToByteArray()
            chunks += value.encodeToByteArray()
            chunks += CRLF.encodeToByteArray()
        }

        chunks += "--$boundary$CRLF".encodeToByteArray()
        chunks += "Content-Disposition: form-data; name=\"$mediaFieldName\"; filename=\"$mediaFileName\"$CRLF".encodeToByteArray()
        chunks += "Content-Type: $mediaContentType$CRLF".encodeToByteArray()
        chunks += CRLF.encodeToByteArray()
        chunks += mediaBytes
        chunks += CRLF.encodeToByteArray()
        chunks += "--$boundary--$CRLF".encodeToByteArray()

        return MultipartBody(bytes = concatenate(chunks), boundary = boundary)
    }

    private fun concatenate(chunks: List<ByteArray>): ByteArray {
        val result = ByteArray(chunks.sumOf { chunk -> chunk.size })
        var offset = 0
        chunks.forEach { chunk ->
            chunk.copyInto(result, offset)
            offset += chunk.size
        }
        return result
    }
}
