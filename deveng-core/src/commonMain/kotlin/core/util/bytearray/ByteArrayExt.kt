package core.util.bytearray

import androidx.compose.ui.graphics.ImageBitmap

expect fun ByteArray.toImageBitmap(): ImageBitmap

expect fun ImageBitmap.toByteArray(): ByteArray?

/** JPEG quality for video-recording preview thumbnails only (not still-photo capture). */
const val VIDEO_THUMBNAIL_JPEG_QUALITY = 50

/** Encodes a video-session thumbnail; uses [VIDEO_THUMBNAIL_JPEG_QUALITY], not photo [toByteArray]. */
expect fun ImageBitmap.toVideoThumbnailByteArray(): ByteArray?