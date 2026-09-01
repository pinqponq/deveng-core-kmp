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
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual class RemoteMediaExportManager(
    private val context: Context,
) {
    private companion object {
        const val TAG = "RemoteMediaExportManager"
        private const val EXPORT_TAG = ExifExportDiagnostics.LOG_TAG
        private const val CONNECT_TIMEOUT_MS = 30_000
        private const val READ_TIMEOUT_MS = 120_000
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
            val bytes = openStreamWithTimeout(fileUrl).use { inputStream ->
                inputStream.readBytes()
            }
            shareBytesInternal(
                fileName = fileName,
                mimeType = mimeType,
                fileBytes = bytes,
            )
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
            val sharedDir = File(context.cacheDir, "shared_media").apply {
                mkdirs()
            }
            val authority = "${context.applicationContext.packageName}.provider"

            val uris = ArrayList<Uri>()
            files.forEachIndexed { index, remoteFile ->
                if (remoteFile.fileUrl.isBlank()) {
                    return@forEachIndexed
                }

                val bytes = openStreamWithTimeout(remoteFile.fileUrl).use { inputStream ->
                    inputStream.readBytes()
                }
                if (bytes.isEmpty()) {
                    return@forEachIndexed
                }

                val fallbackName = "shared_${System.currentTimeMillis()}_$index"
                val normalizedFileName = remoteFile.fileName.ifBlank { fallbackName }
                val tempFile = File(sharedDir, normalizedFileName).apply {
                    writeBytes(bytes)
                }

                val fileUri = FileProvider.getUriForFile(
                    context,
                    authority,
                    tempFile,
                )
                uris.add(fileUri)
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

    private fun shareBytesInternal(
        fileName: String,
        mimeType: String,
        fileBytes: ByteArray,
    ): Boolean {
        if (fileBytes.isEmpty()) {
            return false
        }

        val sharedDir = File(context.cacheDir, "shared_media").apply {
            mkdirs()
        }
        val normalizedFileName = fileName.ifBlank { "shared_${System.currentTimeMillis()}" }
        val tempFile = File(sharedDir, normalizedFileName).apply {
            writeBytes(fileBytes)
        }

        val authority = "${context.applicationContext.packageName}.provider"
        val fileUri = FileProvider.getUriForFile(
            context,
            authority,
            tempFile,
        )

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

    private fun openStreamWithTimeout(fileUrl: String): InputStream {
        val connection = URL(fileUrl).openConnection() as HttpURLConnection
        connection.connectTimeout = CONNECT_TIMEOUT_MS
        connection.readTimeout = READ_TIMEOUT_MS
        connection.connect()
        return connection.inputStream
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
