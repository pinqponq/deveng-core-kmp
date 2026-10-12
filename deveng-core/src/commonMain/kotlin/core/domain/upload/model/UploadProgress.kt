package core.domain.upload.model

/**
 * Byte-level progress for the single in-flight upload. Emitted by schedulers that can observe
 * transfer progress (iOS NSURLSession via didSendBodyData); other platforms don't emit it (their
 * upload call has no byte callback), so callers fall back to item-count progress there.
 */
data class UploadProgress(
    val uploadId: String,
    val bytesSent: Long,
    val bytesTotal: Long,
) {
    val fraction: Float
        get() = if (bytesTotal > 0) {
            (bytesSent.toFloat() / bytesTotal.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }
}
