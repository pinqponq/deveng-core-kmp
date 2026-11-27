package core.data.file

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

actual class DefaultMediaFileBytesReader actual constructor() : MediaFileBytesReader {
    actual override suspend fun read(path: String): ByteArray =
        withContext(Dispatchers.IO) {
            File(path).readBytes()
        }
}
