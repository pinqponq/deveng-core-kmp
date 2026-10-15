package core.util.media

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.FileProvider
import core.util.image.ExifExportDiagnostics
import core.util.image.JpegDebugProbe
import core.util.image.PhotoSaveUtils
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.TimeZone

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual class RemoteMediaExportManager(
    private val context: Context,
) {
    private companion object {
        const val TAG = "RemoteMediaExportManager"
        private const val EXPORT_TAG = ExifExportDiagnostics.LOG_TAG
        private const val CONNECT_TIMEOUT_MS = 30_000
        private const val READ_TIMEOUT_MS = 120_000
        private const val SHARED_MEDIA_DIR_NAME = "shared_media"
        private const val JPEG_MIME_TYPE = "image/jpeg"
        private const val VIDEO_MIME_TYPE_PREFIX = "video/"
    }

    actual suspend fun shareSingleFileFromUrl(
        fileUrl: String,
        fileName: String,
        mimeType: String,
    ): Boolean = withContext(Dispatchers.IO) {
        if (fileUrl.isBlank()) {
            return@withContext false
        }

        return@withContext runCatching {
            val sharedFile = downloadToSharedFile(
                fileUrl = fileUrl,
                fileName = fileName.ifBlank { "shared_${System.currentTimeMillis()}" },
            ) ?: return@runCatching false
            shareFileInternal(sharedFile = sharedFile, mimeType = mimeType)
        }.onFailure { throwable ->
            Log.e(TAG, "shareSingleFileFromUrl failed url=$fileUrl", throwable)
        }.getOrDefault(false)
    }

    actual suspend fun shareMultipleFilesFromUrls(
        files: List<RemoteMediaFile>,
    ): Boolean = withContext(Dispatchers.IO) {
        if (files.isEmpty()) {
            return@withContext false
        }

        return@withContext runCatching {
            val uris = ArrayList<Uri>()
            files.forEachIndexed { index, remoteFile ->
                if (remoteFile.fileUrl.isBlank()) {
                    return@forEachIndexed
                }

                val fallbackName = "shared_${System.currentTimeMillis()}_$index"
                val sharedFile = downloadToSharedFile(
                    fileUrl = remoteFile.fileUrl,
                    fileName = remoteFile.fileName.ifBlank { fallbackName },
                ) ?: return@forEachIndexed
                uris.add(sharedFileUri(sharedFile))
            }

            if (uris.isEmpty()) {
                false
            } else {
                val uniqueMimeTypes = files.map { it.mimeType }.toSet()
                val mimeType = if (uniqueMimeTypes.size == 1) {
                    uniqueMimeTypes.first()
                } else {
                    "*/*"
                }

                val sendIntent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                    type = mimeType
                    putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }

                val chooserIntent = Intent.createChooser(sendIntent, null).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(chooserIntent)
                true
            }
        }.onFailure { throwable ->
            Log.e(TAG, "shareMultipleFilesFromUrls failed count=${files.size}", throwable)
        }.getOrDefault(false)
    }

    actual suspend fun saveSingleFileFromUrl(
        fileUrl: String,
        fileName: String,
        mimeType: String,
    ): Boolean = withContext(Dispatchers.IO) {
        if (fileUrl.isBlank()) {
            return@withContext false
        }
        return@withContext runCatching {
            Log.d(EXPORT_TAG, "saveSingle START fileName=$fileName mimeType=$mimeType url=${urlForLog(fileUrl)}")
            val saved = if (mimeType.startsWith("video/")) {
                streamVideoToMediaStore(fileUrl = fileUrl, fileName = fileName, mimeType = mimeType)
            } else {
                val bytes = openStreamWithTimeout(fileUrl).use { it.readBytes() }
                Log.d(
                    EXPORT_TAG,
                    "saveSingle DOWNLOADED size=${bytes.size} ${JpegDebugProbe.describe(bytes)} " +
                        ExifExportDiagnostics.describeExif(bytes),
                )
                val exportBytes = normalizeImageBytesForExport(bytes, mimeType, fileName)
                saveBytesToMediaStore(fileName = fileName, mimeType = mimeType, fileBytes = exportBytes)
            }
            Log.d(EXPORT_TAG, "saveSingle DONE saved=$saved fileName=$fileName")
            saved
        }.onFailure { throwable ->
            Log.e(EXPORT_TAG, "saveSingle FAILED fileName=$fileName url=${urlForLog(fileUrl)}", throwable)
            Log.e(TAG, "saveSingleFileFromUrl failed url=$fileUrl", throwable)
        }.getOrDefault(false)
    }

    actual suspend fun saveMultipleFilesFromUrls(
        files: List<RemoteMediaFile>,
    ): Int = withContext(Dispatchers.IO) {
        if (files.isEmpty()) {
            return@withContext 0
        }
        return@withContext runCatching {
            var successfulSaveCount = 0
            files.forEach { remoteFile ->
                if (remoteFile.fileUrl.isBlank()) {
                    return@forEach
                }
                Log.d(
                    EXPORT_TAG,
                    "saveBulk item fileName=${remoteFile.fileName} mime=${remoteFile.mimeType} " +
                        "url=${urlForLog(remoteFile.fileUrl)}",
                )
                val isSaved = if (remoteFile.mimeType.startsWith("video/")) {
                    streamVideoToMediaStore(
                        fileUrl = remoteFile.fileUrl,
                        fileName = remoteFile.fileName,
                        mimeType = remoteFile.mimeType,
                    )
                } else {
                    val bytes = openStreamWithTimeout(remoteFile.fileUrl).use { it.readBytes() }
                    Log.d(
                        EXPORT_TAG,
                        "saveBulk DOWNLOADED fileName=${remoteFile.fileName} size=${bytes.size} " +
                            "${JpegDebugProbe.describe(bytes)} ${ExifExportDiagnostics.describeExif(bytes)}",
                    )
                    val exportBytes = normalizeImageBytesForExport(
                        bytes = bytes,
                        mimeType = remoteFile.mimeType,
                        fileName = remoteFile.fileName,
                    )
                    saveBytesToMediaStore(
                        fileName = remoteFile.fileName,
                        mimeType = remoteFile.mimeType,
                        fileBytes = exportBytes,
                    )
                }
                Log.d(EXPORT_TAG, "saveBulk item DONE fileName=${remoteFile.fileName} saved=$isSaved")
                if (isSaved) {
                    successfulSaveCount++
                }
            }
            successfulSaveCount
        }.onFailure { throwable ->
            Log.e(TAG, "saveMultipleFilesFromUrls failed count=${files.size}", throwable)
        }.getOrDefault(0)
    }

    actual suspend fun saveMediaToGallery(
        request: RemoteMediaSaveRequest,
    ): RemoteMediaSaveResult = withContext(Dispatchers.IO) {
        if (request.fileUrl.isBlank()) {
            return@withContext RemoteMediaSaveResult.Failed(IllegalArgumentException("fileUrl is blank"))
        }
        try {
            val connection = openConnectionWithTimeout(request.fileUrl)
            try {
                val statusCode = connection.responseCode
                if (!isSuccessHttpStatus(statusCode)) {
                    Log.w(TAG, "saveMediaToGallery HTTP $statusCode fileName=${request.fileName}")
                    return@withContext RemoteMediaSaveResult.HttpFailure(statusCode)
                }
                connection.inputStream.use { networkStream -> writeToGallery(networkStream, request) }
                RemoteMediaSaveResult.Saved
            } finally {
                connection.disconnect()
            }
        } catch (exception: Exception) {
            Log.e(TAG, "saveMediaToGallery failed fileName=${request.fileName} url=${urlForLog(request.fileUrl)}", exception)
            RemoteMediaSaveResult.Failed(exception)
        }
    }

    private fun shareFileInternal(
        sharedFile: File,
        mimeType: String,
    ): Boolean {
        val fileUri = sharedFileUri(sharedFile)

        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, fileUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val chooserIntent = Intent.createChooser(sendIntent, null).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        context.startActivity(chooserIntent)
        return true
    }

    /**
     * Streams the remote file into the FileProvider cache directory instead of reading it into a
     * byte array: a video of a few dozen MB already exhausts the heap while `readBytes()` doubles its
     * buffer. Returns null when the server sent no bytes.
     */
    private fun downloadToSharedFile(
        fileUrl: String,
        fileName: String,
    ): File? {
        val sharedDir = File(context.cacheDir, SHARED_MEDIA_DIR_NAME).apply {
            mkdirs()
        }
        val sharedFile = File(sharedDir, fileName)
        try {
            openStreamWithTimeout(fileUrl).use { inputStream ->
                sharedFile.outputStream().use { outputStream -> inputStream.copyTo(outputStream) }
            }
        } catch (throwable: Throwable) {
            sharedFile.delete()
            throw throwable
        }

        if (sharedFile.length() == 0L) {
            sharedFile.delete()
            return null
        }

        return sharedFile
    }

    private fun sharedFileUri(sharedFile: File): Uri {
        val authority = "${context.applicationContext.packageName}.provider"
        return FileProvider.getUriForFile(context, authority, sharedFile)
    }

    private fun openStreamWithTimeout(fileUrl: String): InputStream {
        val connection = openConnectionWithTimeout(fileUrl)
        connection.connect()
        return connection.inputStream
    }

    private fun openConnectionWithTimeout(fileUrl: String): HttpURLConnection {
        val connection = URL(fileUrl).openConnection() as HttpURLConnection
        connection.connectTimeout = CONNECT_TIMEOUT_MS
        connection.readTimeout = READ_TIMEOUT_MS
        return connection
    }

    private fun writeToGallery(
        networkStream: InputStream,
        request: RemoteMediaSaveRequest,
    ) {
        val isVideo = request.mimeType.startsWith(VIDEO_MIME_TYPE_PREFIX)
        val resolver = context.contentResolver
        val itemUri = insertPendingGalleryItem(request = request, isVideo = isVideo)
        try {
            val outputStream = resolver.openOutputStream(itemUri)
                ?: throw IOException("MediaStore output stream unavailable uri=$itemUri")
            val writtenByteCount = outputStream.use { galleryStream ->
                if (isVideo) {
                    networkStream.copyTo(galleryStream)
                } else {
                    val photoBytes = photoBytesWithCaptureMetadata(networkStream.readBytes(), request)
                    galleryStream.write(photoBytes)
                    photoBytes.size.toLong()
                }
            }

            if (writtenByteCount == 0L) {
                throw IOException("Downloaded file is empty fileName=${request.fileName}")
            }

            publishGalleryItem(itemUri = itemUri, capturedAtEpochMillis = request.capturedAtEpochMillis)
        } catch (exception: Exception) {
            resolver.delete(itemUri, null, null)
            throw exception
        }
    }

    private fun insertPendingGalleryItem(
        request: RemoteMediaSaveRequest,
        isVideo: Boolean,
    ): Uri {
        val fileName = request.fileName.ifBlank { "media_${System.currentTimeMillis()}" }
        val publicDirectoryName = if (isVideo) Environment.DIRECTORY_MOVIES else Environment.DIRECTORY_PICTURES
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, request.mimeType)
            request.capturedAtEpochMillis?.let { put(MediaStore.MediaColumns.DATE_TAKEN, it) }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, galleryRelativePath(publicDirectoryName, request.albumName))
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            } else {
                put(MediaStore.MediaColumns.DATA, legacyGalleryFile(publicDirectoryName, request.albumName, fileName).absolutePath)
            }
        }
        val targetCollection = if (isVideo) {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }
        return context.contentResolver.insert(targetCollection, contentValues)
            ?: throw IOException("MediaStore insert failed fileName=$fileName")
    }

    private fun galleryRelativePath(
        publicDirectoryName: String,
        albumName: String?,
    ): String = if (albumName.isNullOrBlank()) publicDirectoryName else "$publicDirectoryName/$albumName"

    @Suppress("DEPRECATION")
    private fun legacyGalleryFile(
        publicDirectoryName: String,
        albumName: String?,
        fileName: String,
    ): File {
        val publicDirectory = Environment.getExternalStoragePublicDirectory(publicDirectoryName)
        val albumDirectory = if (albumName.isNullOrBlank()) publicDirectory else File(publicDirectory, albumName)
        albumDirectory.mkdirs()
        return File(albumDirectory, fileName)
    }

    private fun publishGalleryItem(
        itemUri: Uri,
        capturedAtEpochMillis: Long?,
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
        // MediaProvider rescans the file when IS_PENDING is cleared and can overwrite DATE_TAKEN from
        // the file's own metadata (a re-encoded video carries the upload time), so it is set again here.
        val publishedValues = ContentValues().apply {
            put(MediaStore.MediaColumns.IS_PENDING, 0)
            capturedAtEpochMillis?.let { put(MediaStore.MediaColumns.DATE_TAKEN, it) }
        }
        context.contentResolver.update(itemUri, publishedValues, null, null)
    }

    private fun photoBytesWithCaptureMetadata(
        downloadedBytes: ByteArray,
        request: RemoteMediaSaveRequest,
    ): ByteArray {
        val exportBytes = normalizeImageBytesForExport(downloadedBytes, request.mimeType, request.fileName)
        val hasCaptureMetadata = request.capturedAtEpochMillis != null || request.location != null
        if (request.mimeType != JPEG_MIME_TYPE || !hasCaptureMetadata) {
            return exportBytes
        }
        return writeMissingCaptureMetadata(photoBytes = exportBytes, request = request)
    }

    private fun writeMissingCaptureMetadata(
        photoBytes: ByteArray,
        request: RemoteMediaSaveRequest,
    ): ByteArray {
        val capturedAtEpochMillis = request.capturedAtEpochMillis
        val photoBytesWithDate = if (capturedAtEpochMillis != null) {
            PhotoSaveUtils.addMissingCaptureDateTimeExif(
                imageBytes = photoBytes,
                capturedAtEpochMillis = capturedAtEpochMillis,
                timeZone = TimeZone.currentSystemDefault(),
            )
        } else {
            photoBytes
        }

        val location = request.location
        val isLocationMissing = PhotoSaveUtils.readLocationFromExif(photoBytesWithDate) == null
        if (location == null || !isLocationMissing) {
            return photoBytesWithDate
        }

        return PhotoSaveUtils.addLocationExif(photoBytesWithDate, location.latitude, location.longitude)
    }

    /**
     * Streams video bytes directly from the network into MediaStore without loading the full file
     * into memory. This avoids OOM for large videos and respects the download timeout properly.
     */
    private fun streamVideoToMediaStore(
        fileUrl: String,
        fileName: String,
        mimeType: String,
    ): Boolean {
        val normalizedFileName = fileName.ifBlank { "media_${System.currentTimeMillis()}" }
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, normalizedFileName)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_MOVIES)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }
        val resolver = context.contentResolver
        val itemUri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues)
            ?: return false
        return try {
            Log.d(EXPORT_TAG, "streamVideo START fileName=$fileName url=${urlForLog(fileUrl)}")
            val connection = URL(fileUrl).openConnection() as HttpURLConnection
            connection.connectTimeout = CONNECT_TIMEOUT_MS
            connection.readTimeout = READ_TIMEOUT_MS
            connection.connect()
            val totalBytes = connection.inputStream.use { networkStream ->
                resolver.openOutputStream(itemUri)?.use { outputStream ->
                    networkStream.copyTo(outputStream)
                } ?: 0L
            }
            Log.d(EXPORT_TAG, "streamVideo DOWNLOADED totalBytes=$totalBytes fileName=$fileName")
            if (totalBytes == 0L) {
                resolver.delete(itemUri, null, null)
                return false
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                resolver.update(itemUri, ContentValues().apply {
                    put(MediaStore.MediaColumns.IS_PENDING, 0)
                }, null, null)
            }
            Log.d(EXPORT_TAG, "streamVideo OK fileName=$fileName")
            true
        } catch (e: Exception) {
            Log.e(EXPORT_TAG, "streamVideo FAILED fileName=$fileName url=${urlForLog(fileUrl)}", e)
            resolver.delete(itemUri, null, null)
            false
        }
    }

    private fun normalizeImageBytesForExport(
        bytes: ByteArray,
        mimeType: String,
        fileName: String,
    ): ByteArray {
        if (!mimeType.startsWith("image/")) {
            Log.d(EXPORT_TAG, "normalize SKIP non-image fileName=$fileName mimeType=$mimeType")
            return bytes
        }
        Log.d(EXPORT_TAG, "normalize CALL imageBytesWithNormalOrientation fileName=$fileName")
        return PhotoSaveUtils.imageBytesWithNormalOrientation(bytes)
    }

    private fun urlForLog(fileUrl: String): String = try {
        val url = URL(fileUrl)
        val path = url.path?.takeLast(80).orEmpty()
        "${url.host}$path"
    } catch (_: Exception) {
        "(invalid-url)"
    }

    private fun saveBytesToMediaStore(
        fileName: String,
        mimeType: String,
        fileBytes: ByteArray,
    ): Boolean {
        if (fileBytes.isEmpty()) {
            Log.w(EXPORT_TAG, "mediaStore SKIP empty fileName=$fileName")
            return false
        }
        Log.d(
            EXPORT_TAG,
            "mediaStore INSERT fileName=$fileName mimeType=$mimeType size=${fileBytes.size} " +
                "${JpegDebugProbe.describe(fileBytes)} ${ExifExportDiagnostics.describeExif(fileBytes)}",
        )

        val normalizedFileName = fileName.ifBlank { "media_${System.currentTimeMillis()}" }
        val relativePath = when {
            mimeType.startsWith("video/") -> Environment.DIRECTORY_MOVIES
            else -> Environment.DIRECTORY_PICTURES
        }

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, normalizedFileName)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }

        val resolver = context.contentResolver
        val targetCollection = if (mimeType.startsWith("video/")) {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

        val itemUri = resolver.insert(targetCollection, contentValues) ?: return false
        return try {
            resolver.openOutputStream(itemUri)?.use { outputStream ->
                outputStream.write(fileBytes)
            } ?: return false

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val publishedValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.IS_PENDING, 0)
                }
                resolver.update(itemUri, publishedValues, null, null)
            }
            Log.d(EXPORT_TAG, "mediaStore OK uri=$itemUri fileName=$fileName")
            true
        } catch (e: Exception) {
            Log.e(EXPORT_TAG, "mediaStore FAILED fileName=$fileName", e)
            resolver.delete(itemUri, null, null)
            false
        }
    }
}
