package core.util.media

import platform.Foundation.NSCondition
import platform.Foundation.NSData
import platform.Foundation.NSDate
import platform.Foundation.NSOperationQueue
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.dataWithContentsOfURL
import platform.Foundation.timeIntervalSince1970
import platform.Foundation.writeToURL
import kotlinx.coroutines.withTimeoutOrNull
import platform.Photos.PHAssetChangeRequest
import platform.Photos.PHPhotoLibrary
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual class RemoteMediaExportManager {
    private companion object {
        // NSData.dataWithContentsOfURL blocks the calling thread; the timeout fires at the
        // coroutine level so the operation returns null after this window even though the
        // underlying thread may still be waiting on the OS connection.
        private const val RESOURCE_TIMEOUT_MS = 120_000L
    }

    actual suspend fun shareSingleFileFromUrl(
        fileUrl: String,
        fileName: String,
        mimeType: String,
    ): Boolean = withContext(Dispatchers.Default) {
        if (fileUrl.isBlank()) return@withContext false
        return@withContext runCatching {
            val url = NSURL.URLWithString(fileUrl) ?: return@runCatching false
            val data = downloadDataSynchronously(url) ?: return@runCatching false
            val tempFile = writeTempFile(data, fileName)
            NSOperationQueue.mainQueue.addOperationWithBlock {
                shareFiles(listOf(tempFile))
            }
            true
        }.getOrDefault(false)
    }

    actual suspend fun shareMultipleFilesFromUrls(files: List<RemoteMediaFile>): Boolean =
        withContext(Dispatchers.Default) {
        if (files.isEmpty()) return@withContext false
        return@withContext runCatching {
            val tempFiles = files.mapNotNull { remoteFile ->
                if (remoteFile.fileUrl.isBlank()) return@mapNotNull null
                val url = NSURL.URLWithString(remoteFile.fileUrl) ?: return@mapNotNull null
                val data = downloadDataSynchronously(url) ?: return@mapNotNull null
                writeTempFile(data, remoteFile.fileName)
            }
            if (tempFiles.isEmpty()) return@runCatching false
            NSOperationQueue.mainQueue.addOperationWithBlock {
                shareFiles(tempFiles)
            }
            true
        }.getOrDefault(false)
    }

    actual suspend fun saveSingleFileFromUrl(
        fileUrl: String,
        fileName: String,
        mimeType: String,
    ): Boolean = withContext(Dispatchers.Default) {
        if (fileUrl.isBlank()) return@withContext false
        return@withContext runCatching {
            val url = NSURL.URLWithString(fileUrl) ?: return@runCatching false
            val data = downloadDataSynchronously(url) ?: return@runCatching false
            val tempFile = writeTempFile(data, fileName)
            saveToPhotos(tempFile, mimeType)
        }.getOrDefault(false)
    }

    actual suspend fun saveMultipleFilesFromUrls(files: List<RemoteMediaFile>): Int =
        withContext(Dispatchers.Default) {
        if (files.isEmpty()) return@withContext 0
        return@withContext runCatching {
            var count = 0
            files.forEach { remoteFile ->
                if (remoteFile.fileUrl.isBlank()) return@forEach
                val url = NSURL.URLWithString(remoteFile.fileUrl) ?: return@forEach
                val data = downloadDataSynchronously(url) ?: return@forEach
                val tempFile = writeTempFile(data, remoteFile.fileName)
                if (saveToPhotos(tempFile, remoteFile.mimeType)) count++
            }
            count
        }.getOrDefault(0)
    }

    /**
     * Downloads data synchronously with explicit connection and resource timeouts so that large
     * video files do not hang indefinitely on slow or stalled connections.
     */
    private suspend fun downloadDataSynchronously(url: NSURL): NSData? =
        withTimeoutOrNull(RESOURCE_TIMEOUT_MS) {
            NSData.dataWithContentsOfURL(url)
        }

    private fun writeTempFile(data: NSData, fileName: String): NSURL {
        val tempDir = NSURL.fileURLWithPath(NSTemporaryDirectory(), isDirectory = true)
        val name = fileName.ifBlank { "media_${NSDate().timeIntervalSince1970.toLong()}" }
        return tempDir.URLByAppendingPathComponent(name)!!.also { fileUrl ->
            data.writeToURL(fileUrl, atomically = true)
        }
    }

    private fun shareFiles(fileUrls: List<NSURL>) {
        val rootVC = UIApplication.sharedApplication.keyWindow?.rootViewController ?: return
        var topVC = rootVC
        while (topVC.presentedViewController != null) {
            topVC = topVC.presentedViewController!!
        }
        val activityVC = UIActivityViewController(
            activityItems = fileUrls,
            applicationActivities = null,
        )
        topVC.presentViewController(activityVC, animated = true, completion = null)
    }

    private fun saveToPhotos(fileUrl: NSURL, mimeType: String): Boolean {
        var result = false
        val condition = NSCondition()
        var completed = false

        PHPhotoLibrary.sharedPhotoLibrary().performChanges({
            if (mimeType.startsWith("video/")) {
                PHAssetChangeRequest.creationRequestForAssetFromVideoAtFileURL(fileUrl)
            } else {
                PHAssetChangeRequest.creationRequestForAssetFromImageAtFileURL(fileUrl)
            }
        }) { success, _ ->
            condition.lock()
            result = success
            completed = true
            condition.signal()
            condition.unlock()
        }

        condition.lock()
        while (!completed) condition.wait()
        condition.unlock()

        return result
    }
}
