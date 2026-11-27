package core.data.file

interface MediaFileBytesReader {
    suspend fun read(path: String): ByteArray
}

expect class DefaultMediaFileBytesReader() : MediaFileBytesReader {
    override suspend fun read(path: String): ByteArray
}
