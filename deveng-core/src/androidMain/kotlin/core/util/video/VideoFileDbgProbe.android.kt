package core.util.video

import android.media.MediaMetadataRetriever
import java.io.File

actual object VideoFileDbgProbe {
    actual fun describe(filePath: String): String {
        val file = File(filePath)
        if (!file.isFile) {
            return "missing path=$filePath"
        }
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(filePath)
            val codedWidth = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                ?.toIntOrNull()
            val codedHeight = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                ?.toIntOrNull()
            val rotation = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
                ?.toIntOrNull()
            val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull()
            val bitrate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)
                ?.toLongOrNull()
            val mime = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)
            val displayWxH = displaySize(codedWidth, codedHeight, rotation)
            buildString {
                append("path=$filePath ")
                append("bytes=${file.length()} ")
                append("coded=${codedWidth}x$codedHeight ")
                append("display=$displayWxH ")
                append("rotation=$rotation ")
                append("durationMs=$durationMs ")
                append("bitrate=$bitrate ")
                append("mime=$mime")
            }
        } catch (e: Exception) {
            "probeFailed path=$filePath bytes=${file.length()} err=${e.message}"
        } finally {
            runCatching { retriever.release() }
        }
    }

    private fun displaySize(width: Int?, height: Int?, rotation: Int?): String {
        if (width == null || height == null) return "unknown"
        val rot = rotation ?: 0
        return if (rot == 90 || rot == 270) {
            "${height}x$width"
        } else {
            "${width}x$height"
        }
    }
}
