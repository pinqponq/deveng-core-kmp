package core.domain.camera.video

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.ImageBitmap

/**
 * Sealed class representing the result of a completed video recording.
 * Mirrors [core.domain.camera.result.ImageCaptureResult] in structure.
 */
@Immutable
sealed class VideoCaptureResult {

    /**
     * Recording completed successfully.
     *
     * @property filePath Absolute path to the saved video file.
     * @property durationMs Actual recorded duration in milliseconds.
     * @property thumbnailBitmap Optional thumbnail (e.g. preview frame captured after recording start). Use for preview without platform-specific extraction.
     */
    @Immutable
    data class Success(
        val filePath: String,
        val durationMs: Long,
        val thumbnailBitmap: ImageBitmap? = null,
    ) : VideoCaptureResult()

    /**
     * Recording failed or was cancelled.
     *
     * @property exception The underlying cause.
     */
    @Immutable
    data class Error(val exception: Exception) : VideoCaptureResult()
}
