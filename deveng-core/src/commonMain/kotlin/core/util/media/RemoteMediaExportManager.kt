package core.util.media

/**
 * Remote media file metadata for bulk export (share or gallery save).
 */
data class RemoteMediaFile(
    val fileUrl: String,
    val fileName: String,
    val mimeType: String,
)

/**
 * Downloads media from remote URLs and exports it: share to other apps or save to the device
 * gallery (platform-specific). Host apps must configure Android `FileProvider` paths for cache if
 * sharing is used.
 */
@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
expect class RemoteMediaExportManager {
    suspend fun shareSingleFileFromUrl(
        fileUrl: String,
        fileName: String,
        mimeType: String,
    ): Boolean

    suspend fun shareMultipleFilesFromUrls(
        files: List<RemoteMediaFile>,
    ): Boolean

    suspend fun saveSingleFileFromUrl(
        fileUrl: String,
        fileName: String,
        mimeType: String,
    ): Boolean

    suspend fun saveMultipleFilesFromUrls(
        files: List<RemoteMediaFile>,
    ): Int
}
