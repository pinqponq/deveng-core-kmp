package core.util.media

import core.util.multiplatform.IosShareSheetPresenter
import kotlin.coroutines.resume
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import platform.Foundation.NSCondition
import platform.Foundation.NSDate
import platform.Foundation.NSFileManager
import platform.Foundation.NSHTTPURLResponse
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSURLSession
import platform.Foundation.NSURLSessionConfiguration
import platform.Foundation.downloadTaskWithURL
import platform.Foundation.timeIntervalSince1970
import platform.Photos.PHAssetChangeRequest
import platform.Photos.PHPhotoLibrary

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual class RemoteMediaExportManager {
    private companion object {
        // Idle timeout between received packets, matching the Android read timeout. There is no cap
        // on the total duration, so a large video on a slow connection still completes.
        private const val REQUEST_TIMEOUT_SECONDS = 120.0
        private const val SUCCESS_STATUS_CODES_START = 200L
        private const val SUCCESS_STATUS_CODES_END = 299L
    }

    private val downloadSession: NSURLSession by lazy {
        val configuration = NSURLSessionConfiguration.defaultSessionConfiguration().apply {
            timeoutIntervalForRequest = REQUEST_TIMEOUT_SECONDS
        }
        NSURLSession.sessionWithConfiguration(configuration)
    }

    actual suspend fun shareSingleFileFromUrl(
        fileUrl: String,
        fileName: String,
        mimeType: String,
    ): Boolean = withContext(Dispatchers.Default) {
        if (fileUrl.isBlank()) return@withContext false
        return@withContext runCatching {
            val url = NSURL.URLWithString(fileUrl) ?: return@runCatching false
            val tempFile = downloadToTempFile(url, fileName) ?: return@runCatching false
            presentShareSheet(listOf(tempFile))
        }.getOrDefault(false)
    }

    actual suspend fun shareMultipleFilesFromUrls(files: List<RemoteMediaFile>): Boolean =
        withContext(Dispatchers.Default) {
        if (files.isEmpty()) return@withContext false
        return@withContext runCatching {
            val tempFiles = files.mapNotNull { remoteFile ->
                if (remoteFile.fileUrl.isBlank()) return@mapNotNull null
                val url = NSURL.URLWithString(remoteFile.fileUrl) ?: return@mapNotNull null
                downloadToTempFile(url, remoteFile.fileName)
            }
            if (tempFiles.isEmpty()) return@runCatching false
            presentShareSheet(tempFiles)
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
            val tempFile = downloadToTempFile(url, fileName) ?: return@runCatching false
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
                val tempFile = downloadToTempFile(url, remoteFile.fileName) ?: return@forEach
                if (saveToPhotos(tempFile, remoteFile.mimeType)) count++
            }
            count
        }.getOrDefault(0)
    }

    /**
     * Downloads straight to a file in the temporary directory. The previous
     * `NSData.dataWithContentsOfURL` held the whole file in memory (a large video could get the app
     * killed) and, being a blocking call, could not be interrupted by a coroutine timeout.
     * Returns null when the request fails or the server answers with a non-2xx status.
     */
    private suspend fun downloadToTempFile(url: NSURL, fileName: String): NSURL? =
        suspendCancellableCoroutine { continuation ->
            val task = downloadSession.downloadTaskWithURL(url) { location, response, error ->
                val statusCode = (response as? NSHTTPURLResponse)?.statusCode
                val isSuccessStatus = statusCode == null ||
                    statusCode in SUCCESS_STATUS_CODES_START..SUCCESS_STATUS_CODES_END
                if (error != null || location == null || !isSuccessStatus) {
                    continuation.resume(null)
                    return@downloadTaskWithURL
                }

                // The system deletes `location` as soon as this handler returns, so it is moved now.
                continuation.resume(moveToTempFile(location, fileName))
            }
            continuation.invokeOnCancellation { task.cancel() }
            task.resume()
        }

    @OptIn(ExperimentalForeignApi::class)
    private fun moveToTempFile(downloadedFile: NSURL, fileName: String): NSURL? {
        val tempDir = NSURL.fileURLWithPath(NSTemporaryDirectory(), isDirectory = true)
        val name = fileName.ifBlank { "media_${NSDate().timeIntervalSince1970.toLong()}" }
        val destination = tempDir.URLByAppendingPathComponent(name) ?: return null
        val fileManager = NSFileManager.defaultManager
        fileManager.removeItemAtURL(destination, error = null)
        val isMoved = fileManager.moveItemAtURL(downloadedFile, toURL = destination, error = null)
        return if (isMoved) destination else null
    }

    /**
     * Presents on the main thread and reports whether a window was available, so a share sheet
     * that could not be shown is returned as a failure instead of a silent success.
     */
    private suspend fun presentShareSheet(fileUrls: List<NSURL>): Boolean =
        withContext(Dispatchers.Main) {
            IosShareSheetPresenter.present(activityItems = fileUrls)
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
