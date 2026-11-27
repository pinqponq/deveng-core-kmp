@file:OptIn(ExperimentalForeignApi::class)

package core.data.file

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.Foundation.NSData
import platform.Foundation.NSURL
import platform.Foundation.dataWithContentsOfURL
import platform.posix.memcpy

private fun fileUrlForPath(path: String): NSURL? {
    val trimmed = path.trim()
    if (trimmed.isEmpty()) return null
    return if (trimmed.startsWith(prefix = "file:", ignoreCase = true)) {
        NSURL.URLWithString(trimmed)
    } else {
        NSURL.fileURLWithPath(trimmed)
    }
}

actual class DefaultMediaFileBytesReader actual constructor() : MediaFileBytesReader {
    actual override suspend fun read(path: String): ByteArray =
        withContext(Dispatchers.Default) {
            val url = fileUrlForPath(path = path)
                ?: error(message = "Invalid path: $path")

            val data = NSData.dataWithContentsOfURL(url)
                ?: error(message = "Cannot read file: $path")

            val byteCount = data.length
            if (byteCount == 0UL) {
                ByteArray(size = 0)
            } else {
                val lengthInt = byteCount.toInt()
                require(value = lengthInt >= 0) {
                    "File too large to read into ByteArray: $path"
                }
                val src = data.bytes ?: error(message = "NSData has no bytes buffer: $path")
                ByteArray(size = lengthInt).apply {
                    usePinned { pinned ->
                        memcpy(pinned.addressOf(0), src, byteCount)
                    }
                }
            }
        }
}
