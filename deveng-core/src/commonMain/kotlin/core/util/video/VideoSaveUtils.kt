package core.util.video

/**
 * Result of saving a video to the device gallery (Photos / MediaStore).
 * @property path On success: asset identifier (e.g. ph:// on iOS) or file path; on error use [SaveVideoResult.Error].
 */
sealed class SaveVideoResult {
    data class Success(val path: String) : SaveVideoResult()
    data class Error(val exception: Throwable) : SaveVideoResult()
}

/**
 * Platform-specific utility to add a video file to the device gallery.
 * Use when you have [VideoCaptureResult.Success]: call [saveVideoToPhotos] with [VideoCaptureResult.Success.filePath]
 * so the video appears in the Photos app (iOS) or Gallery (Android). Same pattern as [core.util.image.PhotoSaveUtils.savePhoto].
 */
expect object VideoSaveUtils {

    /**
     * Optional: call once with the application context so that on Android the video
     * is notified to MediaStore and appears in the Gallery. No-op on other platforms.
     */
    fun setApplicationContext(context: Any?)

    /**
     * Adds the video file at [videoFilePath] to the device gallery.
     * iOS: PHPhotoLibrary; Android: MediaStore scan (requires [setApplicationContext] to have been called).
     *
     * @param videoFilePath Absolute path to the recorded video file (e.g. from [VideoCaptureResult.Success.filePath]).
     * @return [SaveVideoResult.Success] with the asset path (e.g. ph:// on iOS) or file path, or [SaveVideoResult.Error] on failure.
     */
    fun saveVideoToPhotos(videoFilePath: String): SaveVideoResult
}
