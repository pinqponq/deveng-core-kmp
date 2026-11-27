package com.deveng.calf.io

import com.deveng.calf.core.InternalCalfApi
import com.deveng.calf.core.PlatformContext
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSFileSize
import platform.Foundation.NSNumber
import platform.Foundation.NSURL
import platform.Foundation.NSURLIsDirectoryKey
import platform.Foundation.dataWithContentsOfURL
import platform.posix.memcpy

/**
 * A wrapper class for a file in the platform-specific implementation.
 *
 * @property url A persistent copy of the file that remains accessible after the picker callback.
 * @property originalUrl The original URL returned by the file picker. May become inaccessible
 * due to iOS security scoping — use for metadata only (file name, path).
 */
actual class KmpFile @InternalCalfApi constructor(
    val url: NSURL,
    val originalUrl: NSURL,
    val assetIdentifier: String? = null,
    /** Size in bytes captured at pick time when known, so [getSize] can report it without a copy. */
    val size: Long? = null,
    /** The picked item's registered uniform type identifiers, in the order the picker reported them. */
    val typeIdentifiers: List<String> = emptyList(),
) {
    @OptIn(InternalCalfApi::class)
    constructor(url: NSURL) : this(originalUrl = url, url = url)
}

actual fun KmpFile.exists(context: PlatformContext): Boolean {
    return NSFileManager.defaultManager.fileExistsAtPath(url.path ?: return false)
}

@OptIn(ExperimentalForeignApi::class)
actual suspend fun KmpFile.readByteArray(context: PlatformContext): ByteArray {
    val data = NSData.dataWithContentsOfURL(url) ?: return ByteArray(0)
    val byteArraySize: Int = if (data.length > Int.MAX_VALUE.toUInt()) Int.MAX_VALUE else data.length.toInt()
    return ByteArray(byteArraySize).apply {
        usePinned {
            memcpy(it.addressOf(0), data.bytes, data.length)
        }
    }
}

actual fun KmpFile.getName(context: PlatformContext): String? =
    originalUrl.absoluteString
        ?.removeSuffix("/")
        ?.split('/')
        ?.lastOrNull()

actual fun KmpFile.getPath(context: PlatformContext): String? =
    originalUrl.absoluteString

@OptIn(ExperimentalForeignApi::class)
actual fun KmpFile.getSize(context: PlatformContext): Long? {
    size?.let { return it }
    val filePath = url.path ?: return null
    val attributes = NSFileManager.defaultManager.attributesOfItemAtPath(filePath, error = null) ?: return null
    val fileSize = attributes[NSFileSize] as? NSNumber ?: return null

    return fileSize.longLongValue
}

@OptIn(ExperimentalForeignApi::class)
actual fun KmpFile.isDirectory(context: PlatformContext): Boolean {
    val result = originalUrl.resourceValuesForKeys(listOf(NSURLIsDirectoryKey), error = null)
    return result?.get(NSURLIsDirectoryKey) == true
}

/**
 * Converts this [NSURL] to a [KmpFile].
 */
fun NSURL.toKmpFile(): KmpFile = KmpFile(this)
