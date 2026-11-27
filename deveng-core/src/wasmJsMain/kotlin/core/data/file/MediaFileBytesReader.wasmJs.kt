package core.data.file

actual class DefaultMediaFileBytesReader actual constructor() : MediaFileBytesReader {
    actual override suspend fun read(path: String): ByteArray = ByteArray(0)
}
