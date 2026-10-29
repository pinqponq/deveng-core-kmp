package core.data.upload

import co.touchlab.kermit.Logger
import core.data.temp.TempStorageDirProvider
import core.data.upload.session.BackgroundUploadFileNaming
import core.data.upload.session.BackgroundUploadFileStore
import core.domain.camera.utils.toNSData
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import platform.Foundation.NSError
import platform.Foundation.NSFileHandle
import platform.Foundation.NSFileManager
import platform.Foundation.NSFileSize
import platform.Foundation.NSNumber
import platform.Foundation.closeFile
import platform.Foundation.fileHandleForReadingAtPath
import platform.Foundation.readDataOfLength
import platform.Foundation.seekToFileOffset
import platform.Foundation.writeToFile

/** Keeps request bodies as `<prefix><transferId><suffix>` files under [directoryProvider]'s folder. */
@OptIn(ExperimentalForeignApi::class)
internal class FileManagerBackgroundUploadFileStore(
    private val directoryProvider: TempStorageDirProvider,
    private val fileNaming: BackgroundUploadFileNaming,
) : BackgroundUploadFileStore {

    private val fileManager = NSFileManager.defaultManager

    override fun write(uploadId: String, bytes: ByteArray): String? {
        if (bytes.isEmpty()) return null
        val path = filePath(uploadId)
        val isWritten = bytes.toNSData().writeToFile(path, atomically = true)
        if (!isWritten) {
            Logger.e { "[BackgroundUpload] Could not write request body path=$path" }
            return null
        }
        return path
    }

    override fun writePart(transferId: String, sourceFilePath: String, offset: Long, length: Long): String? {
        if (length <= 0) return null
        // NSFileHandle raises an Objective-C exception when it reads past the end, which would
        // crash the app, so a source file that became shorter is caught here first.
        val sourceFileBytes = sourceFileSize(sourceFilePath)
        if (sourceFileBytes == null || offset + length > sourceFileBytes) {
            Logger.e { "[BackgroundUpload] Source file does not hold the part path=$sourceFilePath size=$sourceFileBytes offset=$offset length=$length" }
            return null
        }
        val sourceHandle = NSFileHandle.fileHandleForReadingAtPath(sourceFilePath)
        if (sourceHandle == null) {
            Logger.e { "[BackgroundUpload] Could not open the source file of a part path=$sourceFilePath" }
            return null
        }
        val partData = try {
            sourceHandle.seekToFileOffset(offset.toULong())
            sourceHandle.readDataOfLength(length.toULong())
        } finally {
            sourceHandle.closeFile()
        }
        if (partData.length != length.toULong()) {
            Logger.e { "[BackgroundUpload] Source file is shorter than the part path=$sourceFilePath offset=$offset expected=$length read=${partData.length}" }
            return null
        }
        val path = filePath(transferId)
        if (!partData.writeToFile(path, atomically = true)) {
            Logger.e { "[BackgroundUpload] Could not write part body path=$path" }
            return null
        }
        return path
    }

    override fun delete(uploadId: String) {
        val path = filePath(uploadId)
        if (!fileManager.fileExistsAtPath(path)) return
        memScoped {
            val error = alloc<ObjCObjectVar<NSError?>>()
            if (!fileManager.removeItemAtPath(path, error = error.ptr)) {
                Logger.e { "[BackgroundUpload] Could not delete request body path=$path error=${error.value?.localizedDescription}" }
            }
        }
    }

    override fun listUploadIds(): List<String> = memScoped {
        val error = alloc<ObjCObjectVar<NSError?>>()
        val fileNames = fileManager.contentsOfDirectoryAtPath(directoryPath(), error = error.ptr)
        if (fileNames == null) {
            Logger.e { "[BackgroundUpload] Could not list request bodies error=${error.value?.localizedDescription}" }
        }
        fileNames
            .orEmpty()
            .filterIsInstance<String>()
            .mapNotNull(fileNaming::uploadIdOf)
    }

    private fun sourceFileSize(path: String): Long? = memScoped {
        val error = alloc<ObjCObjectVar<NSError?>>()
        val attributes = fileManager.attributesOfItemAtPath(path, error = error.ptr)
        if (attributes == null) {
            Logger.e { "[BackgroundUpload] Could not read the source file size path=$path error=${error.value?.localizedDescription}" }
            return@memScoped null
        }
        (attributes[NSFileSize] as? NSNumber)?.longLongValue
    }

    private fun directoryPath(): String = directoryProvider.getPath().trimEnd('/')

    private fun filePath(uploadId: String): String = "${directoryPath()}/${fileNaming.fileNameOf(uploadId)}"
}
