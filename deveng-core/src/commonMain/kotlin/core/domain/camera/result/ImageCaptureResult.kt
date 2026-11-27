package core.domain.camera.result

import androidx.compose.ui.graphics.ImageBitmap

/**
 * Result of an image capture. The app always receives bytes (and optional bitmap)
 * and is responsible for saving via [core.util.image.PhotoSaveUtils.savePhoto].
 */
sealed class ImageCaptureResult {
    /**
     * Successful capture. Use [byteArray] to save (e.g. after adding location EXIF)
     * with [core.util.image.PhotoSaveUtils.savePhoto].
     *
     * @param byteArray The captured image data as a [ByteArray].
     * @param bitmap Optional decoded [ImageBitmap] for thumbnail UI; may be null.
     */
    data class Success(
        val byteArray: ByteArray,
        val bitmap: ImageBitmap? = null,
    ) : ImageCaptureResult() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false

            other as Success

            return byteArray.contentEquals(other.byteArray) && bitmap == other.bitmap
        }

        override fun hashCode(): Int = 31 * byteArray.contentHashCode() + (bitmap?.hashCode() ?: 0)
    }

    /**
     * Capture failed.
     *
     * @param exception The exception that occurred during image capture.
     */
    data class Error(val exception: Exception) : ImageCaptureResult()
}
