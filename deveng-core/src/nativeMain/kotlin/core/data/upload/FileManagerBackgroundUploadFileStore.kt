package core.data.upload

import co.touchlab.kermit.Logger
import core.data.temp.TempStorageDirProvider
import core.data.upload.session.BackgroundUploadFileNaming
import core.data.upload.session.BackgroundUploadFileStore
import core.domain.camera.utils.toNSData
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSFileManager
import platform.Foundation.writeToFile

/** Keeps request bodies as `<prefix><uploadId><suffix>` files under [directoryProvider]'s folder. */
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

    override fun delete(uploadId: String) {
        fileManager.removeItemAtPath(filePath(uploadId), error = null)
    }

    override fun listUploadIds(): List<String> =
        fileManager.contentsOfDirectoryAtPath(directoryPath(), error = null)
            .orEmpty()
            .filterIsInstance<String>()
            .mapNotNull(fileNaming::uploadIdOf)

    private fun directoryPath(): String = directoryProvider.getPath().trimEnd('/')

    private fun filePath(uploadId: String): String = "${directoryPath()}/${fileNaming.fileNameOf(uploadId)}"
}
