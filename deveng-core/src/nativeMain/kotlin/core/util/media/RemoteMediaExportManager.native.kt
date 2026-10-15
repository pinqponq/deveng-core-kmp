package core.util.media

import core.util.multiplatform.IosShareSheetPresenter
import kotlin.coroutines.resume
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import platform.CoreLocation.CLLocation
import platform.Foundation.NSDate
import platform.Foundation.NSError
import platform.Foundation.NSFileManager
import platform.Foundation.NSHTTPURLResponse
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSURLSession
import platform.Foundation.NSURLSessionConfiguration
import platform.Foundation.NSLog
import platform.Foundation.dateWithTimeIntervalSince1970
import platform.Foundation.downloadTaskWithURL
import platform.Foundation.timeIntervalSince1970
import platform.Photos.PHAssetChangeRequest
import platform.Photos.PHPhotoLibrary

private sealed interface TempFileDownload {
    data class Downloaded(val file: NSURL) : TempFileDownload

    data class HttpFailure(val statusCode: Int) : TempFileDownload

    data class Failed(val reason: String) : TempFileDownload
}

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual class RemoteMediaExportManager {
    private companion object {
        // Idle timeout between received packets, matching the Android read timeout. There is no cap
        // on the total duration, so a large video on a slow connection still completes.
        private const val REQUEST_TIMEOUT_SECONDS = 120.0
        private const val MILLIS_PER_SECOND = 1000.0
        private const val VIDEO_MIME_TYPE_PREFIX = "video/"
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
            saveToPhotosAndDeleteFile(tempFile, mimeType)
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
                if (saveToPhotosAndDeleteFile(tempFile, remoteFile.mimeType)) count++
            }
            count
        }.getOrDefault(0)
    }

    actual suspend fun saveMediaToGallery(request: RemoteMediaSaveRequest): RemoteMediaSaveResult =
        withContext(Dispatchers.Default) {
            val url = NSURL.URLWithString(request.fileUrl)
                ?: return@withContext RemoteMediaSaveResult.Failed(
                    IllegalArgumentException("fileUrl is not a valid URL"),
                )
            when (val download = downloadToTempFileWithStatus(url, request.fileName)) {
                is TempFileDownload.HttpFailure -> {
                    NSLog("RemoteMediaExportManager: saveMediaToGallery HTTP %d", download.statusCode)
                    RemoteMediaSaveResult.HttpFailure(download.statusCode)
                }
                is TempFileDownload.Failed -> {
                    NSLog("RemoteMediaExportManager: saveMediaToGallery download failed: %@", download.reason)
                    RemoteMediaSaveResult.Failed(IllegalStateException(download.reason))
                }
                is TempFileDownload.Downloaded -> saveDownloadedMedia(download.file, request)
            }
        }

    private suspend fun saveDownloadedMedia(
        tempFile: NSURL,
        request: RemoteMediaSaveRequest,
    ): RemoteMediaSaveResult {
        val creationDate = request.capturedAtEpochMillis?.let { epochMillis ->
            NSDate.dateWithTimeIntervalSince1970(epochMillis / MILLIS_PER_SECOND)
        }

        val captureLocation = request.location?.let { location ->
            CLLocation(latitude = location.latitude, longitude = location.longitude)
        }

        val isSaved = saveToPhotosAndDeleteFile(
            fileUrl = tempFile,
            mimeType = request.mimeType,
            creationDate = creationDate,
            captureLocation = captureLocation,
        )
        return if (isSaved) {
            RemoteMediaSaveResult.Saved
        } else {
            RemoteMediaSaveResult.Failed(IllegalStateException("Photos rejected fileName=${request.fileName}"))
        }
    }

    private suspend fun downloadToTempFile(url: NSURL, fileName: String): NSURL? =
        (downloadToTempFileWithStatus(url, fileName) as? TempFileDownload.Downloaded)?.file

    /**
     * Downloads straight to a file in the temporary directory. The previous
     * `NSData.dataWithContentsOfURL` held the whole file in memory (a large video could get the app
     * killed) and, being a blocking call, could not be interrupted by a coroutine timeout.
     */
    private suspend fun downloadToTempFileWithStatus(url: NSURL, fileName: String): TempFileDownload =
        suspendCancellableCoroutine { continuation ->
            val task = downloadSession.downloadTaskWithURL(url) { location, response, error ->
                val statusCode = (response as? NSHTTPURLResponse)?.statusCode?.toInt()
                if (statusCode != null && !isSuccessHttpStatus(statusCode)) {
                    continuation.resume(TempFileDownload.HttpFailure(statusCode))
                    return@downloadTaskWithURL
                }

                if (error != null || location == null) {
                    continuation.resume(TempFileDownload.Failed(downloadFailureReason(error)))
                    return@downloadTaskWithURL
                }

                // The system deletes `location` as soon as this handler returns, so it is moved now.
                val movedFile = moveToTempFile(location, fileName)
                val download = if (movedFile != null) {
                    TempFileDownload.Downloaded(movedFile)
                } else {
                    TempFileDownload.Failed("Moving the downloaded file failed")
                }
                continuation.resume(download)
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

    private fun downloadFailureReason(error: NSError?): String =
        error?.let { "${it.domain} ${it.code}: ${it.localizedDescription}" } ?: "Download finished without a file"

    /**
     * Photos copies the file into its library during `performChanges`, so the temp copy is deleted
     * afterwards; otherwise every saved item would stay in the temp folder and take its size twice.
     */
    private suspend fun saveToPhotosAndDeleteFile(
        fileUrl: NSURL,
        mimeType: String,
        creationDate: NSDate? = null,
        captureLocation: CLLocation? = null,
    ): Boolean = try {
        saveToPhotos(fileUrl, mimeType, creationDate, captureLocation)
    } finally {
        deleteTempFile(fileUrl)
    }

    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
    private fun deleteTempFile(fileUrl: NSURL) = memScoped {
        val deleteError = alloc<ObjCObjectVar<NSError?>>()
        val isDeleted = NSFileManager.defaultManager.removeItemAtURL(fileUrl, error = deleteError.ptr)
        if (!isDeleted) {
            NSLog("RemoteMediaExportManager: deleting temp file failed: %@", deleteError.value?.localizedDescription)
        }
    }

    private suspend fun saveToPhotos(
        fileUrl: NSURL,
        mimeType: String,
        creationDate: NSDate?,
        captureLocation: CLLocation?,
    ): Boolean = suspendCancellableCoroutine { continuation ->
        PHPhotoLibrary.sharedPhotoLibrary().performChanges({
            val creationRequest = if (mimeType.startsWith(VIDEO_MIME_TYPE_PREFIX)) {
                PHAssetChangeRequest.creationRequestForAssetFromVideoAtFileURL(fileUrl)
            } else {
                PHAssetChangeRequest.creationRequestForAssetFromImageAtFileURL(fileUrl)
            }
            creationDate?.let { creationRequest?.creationDate = it }
            captureLocation?.let { creationRequest?.location = it }
        }) { success, error ->
            if (!success) {
                NSLog("RemoteMediaExportManager: saving to Photos failed: %@", error?.localizedDescription)
            }
            continuation.resume(success)
        }
    }
}
