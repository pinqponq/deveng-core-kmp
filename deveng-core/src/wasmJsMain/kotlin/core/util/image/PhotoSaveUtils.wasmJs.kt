@file:OptIn(kotlin.io.encoding.ExperimentalEncodingApi::class, kotlin.time.ExperimentalTime::class)

package core.util.image

import kotlin.io.encoding.Base64
import kotlin.time.Clock

actual object PhotoSaveUtils {

    actual fun setApplicationContext(context: Any?) {}

    actual fun imageBytesWithNormalOrientation(imageBytes: ByteArray): ByteArray = imageBytes

    /**
     * On WASM, "saving" means triggering a browser download via a synthetic
     * `<a download="...">` click. There is no real filesystem; the user's browser decides
     * where the bytes land (typically the Downloads folder, or a Save As dialog if the
     * user has that configured).
     *
     * The [targetPath] argument is treated as a *filename suggestion only* — anything
     * before the last `/` is discarded, and an empty result falls back to a timestamped
     * `photo_<epochMs>.jpg`.
     *
     * @return `SavePhotoResult.Success(filename)` if the download was successfully kicked
     *   off (note: the user can still cancel a Save As dialog after this returns —
     *   browsers don't expose that). `SavePhotoResult.Error` if the synthetic click was
     *   blocked or the encode pipeline threw.
     */
    actual fun savePhoto(imageBytes: ByteArray, targetPath: String): SavePhotoResult {
        return try {
            val filename = targetPath.substringAfterLast('/').ifEmpty {
                "photo_${Clock.System.now().toEpochMilliseconds()}.jpg"
            }
            val base64 = Base64.encode(imageBytes)
            val jsError = jsTriggerPhotoDownload(filename, base64)?.toString()
            if (jsError != null) {
                SavePhotoResult.Error(Exception("Browser download failed: $jsError"))
            } else {
                SavePhotoResult.Success(filename)
            }
        } catch (e: Exception) {
            SavePhotoResult.Error(e)
        }
    }

    actual fun addLocationExif(
        imageBytes: ByteArray,
        latitude: Double,
        longitude: Double,
    ): ByteArray = imageBytes

    actual fun readLocationFromExif(imageBytes: ByteArray): Pair<Double, Double>? = null

    actual fun readCaptureDateTimeFromExif(imageBytes: ByteArray): ExifCaptureDateTime? = null
}
