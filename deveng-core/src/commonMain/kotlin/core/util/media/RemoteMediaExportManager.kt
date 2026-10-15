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
 * One remote item saved to the device gallery with its original capture metadata, so it lands at
 * its capture date in the gallery's timeline instead of the save date.
 *
 * @property capturedAtEpochMillis Capture instant in milliseconds since the UTC epoch, or `null`
 *   when unknown (the platform then records the save time).
 * @property location Capture location, or `null` when unknown. Android writes it into photo EXIF
 *   only (MediaStore video location columns are read-only); iOS sets it on the Photos asset.
 * @property albumName Folder under the platform's pictures/movies directory on Android
 *   (e.g. `Pictures/Rindle`), or `null` for the root. Ignored on iOS.
 */
data class RemoteMediaSaveRequest(
    val fileUrl: String,
    val fileName: String,
    val mimeType: String,
    val capturedAtEpochMillis: Long? = null,
    val location: MediaCaptureLocation? = null,
    val albumName: String? = null,
)

data class MediaCaptureLocation(
    val latitude: Double,
    val longitude: Double,
)

/**
 * Outcome of [RemoteMediaExportManager.saveMediaToGallery]. [HttpFailure] carries the download
 * status so callers can tell an expired signed link (403) from a deleted item (404).
 */
sealed interface RemoteMediaSaveResult {
    data object Saved : RemoteMediaSaveResult

    data class HttpFailure(val statusCode: Int) : RemoteMediaSaveResult

    data class Failed(val exception: Throwable) : RemoteMediaSaveResult
}

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

    /**
     * Saves one item to the device gallery with its capture date, location and folder. Unlike
     * [saveSingleFileFromUrl] it reports why a save failed. Not supported on desktop.
     */
    suspend fun saveMediaToGallery(
        request: RemoteMediaSaveRequest,
    ): RemoteMediaSaveResult
}
