package core.data.temp

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSNumber
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSURLIsExcludedFromBackupKey
import platform.Foundation.NSUserDomainMask

/**
 * iOS: uses NSTemporaryDirectory() + [subdir] (default "temp_storage") so temp files survive until app is cleared.
 */
class IosTempStorageDirProvider(
    private val subdir: String = "temp_storage"
) : TempStorageDirProvider {

    override fun getPath(): String {
        val tempDir = NSTemporaryDirectory()
        val base = tempDir.trimEnd('/')
        return "$base/$subdir"
    }
}

/**
 * iOS: uses the Application Support directory + [subdir]. Unlike NSTemporaryDirectory(), which iOS
 * can purge at any time (including mid-transfer), Application Support persists until the app removes
 * the files. The directory is created on first use and excluded from iCloud/iTunes backup.
 */
@OptIn(ExperimentalForeignApi::class)
class IosPersistentStorageDirProvider(
    private val subdir: String = "persistent_storage"
) : TempStorageDirProvider {

    private val fileManager = NSFileManager.defaultManager

    override fun getPath(): String {
        val dirPath = "${applicationSupportBasePath()}/$subdir"
        if (!fileManager.fileExistsAtPath(dirPath)) {
            fileManager.createDirectoryAtPath(
                dirPath,
                withIntermediateDirectories = true,
                attributes = null,
                error = null,
            )
            NSURL.fileURLWithPath(dirPath).setResourceValue(
                NSNumber(bool = true),
                forKey = NSURLIsExcludedFromBackupKey,
                error = null,
            )
        }
        return dirPath
    }

    private fun applicationSupportBasePath(): String {
        val paths = NSSearchPathForDirectoriesInDomains(
            directory = NSApplicationSupportDirectory,
            domainMask = NSUserDomainMask,
            expandTilde = true,
        )
        val base = paths.firstOrNull() as? String
        return (base ?: NSTemporaryDirectory()).trimEnd('/')
    }
}
