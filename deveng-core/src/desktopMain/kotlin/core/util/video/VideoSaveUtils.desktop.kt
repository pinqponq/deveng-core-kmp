package core.util.video

actual object VideoSaveUtils {

    actual fun setApplicationContext(context: Any?) {}

    actual fun saveVideoToPhotos(videoFilePath: String): SaveVideoResult =
        SaveVideoResult.Success(videoFilePath)
}
