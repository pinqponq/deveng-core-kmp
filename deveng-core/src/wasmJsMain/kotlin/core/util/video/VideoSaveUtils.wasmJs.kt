package core.util.video

actual object VideoSaveUtils {

    actual fun setApplicationContext(context: Any?) {}

    actual fun saveVideoToPhotos(videoFilePath: String): SaveVideoResult =
        SaveVideoResult.Error(UnsupportedOperationException("saveVideoToPhotos not supported on WASM/JS"))
}
