package core.util.video

import android.content.Context
import android.media.MediaScannerConnection
import android.util.Log

actual object VideoSaveUtils {

    private const val TAG = "VideoSaveUtils"

    @Volatile
    private var appContext: Context? = null

    actual fun setApplicationContext(context: Any?) {
        appContext = (context as? Context)?.applicationContext
    }

    actual fun saveVideoToPhotos(videoFilePath: String): SaveVideoResult = try {
        appContext?.let { ctx ->
            MediaScannerConnection.scanFile(
                ctx,
                arrayOf(videoFilePath),
                arrayOf("video/mp4"),
                null,
            )
            Log.d(TAG, "MediaStore notified for video: $videoFilePath")
        }
        SaveVideoResult.Success(videoFilePath)
    } catch (e: Exception) {
        Log.e(TAG, "Failed to notify MediaStore for video: ${e.message}")
        SaveVideoResult.Error(e)
    }
}
