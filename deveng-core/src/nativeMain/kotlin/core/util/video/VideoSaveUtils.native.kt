package core.util.video

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Photos.PHAssetChangeRequest
import platform.Photos.PHPhotoLibrary

@OptIn(ExperimentalForeignApi::class)
actual object VideoSaveUtils {

    actual fun setApplicationContext(context: Any?) {}

    actual fun saveVideoToPhotos(videoFilePath: String): SaveVideoResult = try {
        var savedAssetId: String? = null
        var saveError: String? = null
        val semaphore = platform.darwin.dispatch_semaphore_create(0)

        PHPhotoLibrary.sharedPhotoLibrary().performChanges(
            changeBlock = {
                val creationRequest = PHAssetChangeRequest.creationRequestForAssetFromVideoAtFileURL(
                    NSURL.fileURLWithPath(videoFilePath),
                )
                savedAssetId = creationRequest?.placeholderForCreatedAsset?.localIdentifier
            },
            completionHandler = { success, error ->
                if (!success || error != null) {
                    saveError = error?.localizedDescription ?: "Failed to save video to Photos"
                    platform.Foundation.NSLog("VideoSaveUtils: $saveError")
                }
                platform.darwin.dispatch_semaphore_signal(semaphore)
            },
        )

        platform.darwin.dispatch_semaphore_wait(semaphore, platform.darwin.DISPATCH_TIME_FOREVER)

        when {
            saveError != null -> SaveVideoResult.Error(Exception(saveError))
            savedAssetId != null -> {
                // Remove temp file after successful save to Photos (same as photo flow)
                NSFileManager.defaultManager.removeItemAtPath(videoFilePath, null)
                SaveVideoResult.Success("ph://$savedAssetId")
            }
            else -> SaveVideoResult.Error(Exception("Failed to save video to Photos"))
        }
    } catch (e: Exception) {
        SaveVideoResult.Error(e)
    }
}
