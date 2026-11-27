package core.data.temp

import core.domain.camera.utils.toByteArray
import core.domain.camera.utils.toNSData
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL

@OptIn(ExperimentalForeignApi::class)
actual class TempFileOps actual constructor() {

    private val fileManager = NSFileManager.defaultManager

    private fun normalizedPath(path: String): String {
        val trimmed = path.trim()
        if (trimmed.isEmpty()) return trimmed
        return if (trimmed.startsWith(prefix = "file:", ignoreCase = true)) {
            NSURL.URLWithString(trimmed)?.path ?: trimmed
        } else {
            NSURL.fileURLWithPath(trimmed).path ?: trimmed
        }
    }

    actual fun ensureDir(path: String) {
        val resolved = normalizedPath(path)
        if (!fileManager.fileExistsAtPath(resolved)) {
            fileManager.createDirectoryAtPath(
                resolved,
                withIntermediateDirectories = true,
                attributes = null,
                error = null,
            )
        }
    }

    actual fun writeFile(path: String, bytes: ByteArray) {
        val resolved = normalizedPath(path)
        val nsData: NSData = bytes.toNSData()
        fileManager.createFileAtPath(resolved, nsData, null)
    }

    actual fun readFile(path: String): ByteArray? {
        val resolved = normalizedPath(path)
        val data = fileManager.contentsAtPath(resolved) ?: return null
        if (data.length == 0UL) return null
        return data.toByteArray()
    }

    actual fun deleteFile(path: String) {
        fileManager.removeItemAtPath(normalizedPath(path), null)
    }

    actual fun fileExists(path: String): Boolean {
        return fileManager.fileExistsAtPath(normalizedPath(path))
    }
}
