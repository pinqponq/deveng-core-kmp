package core.util.bytearray

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.jetbrains.skia.Image

actual fun ByteArray.toImageBitmap(): ImageBitmap {
    val skiaImage = Image.makeFromEncoded(this)
    return skiaImage.toComposeImageBitmap()
}

actual fun ImageBitmap.toByteArray(): ByteArray? {
    val skiaBitmap = asSkiaBitmap()
    val skiaImage = Image.makeFromBitmap(skiaBitmap)
    return skiaImage.encodeToData(quality = 100)?.bytes
}

actual fun ImageBitmap.toVideoThumbnailByteArray(): ByteArray? {
    val skiaBitmap = asSkiaBitmap()
    val skiaImage = Image.makeFromBitmap(skiaBitmap)
    return skiaImage.encodeToData(quality = VIDEO_THUMBNAIL_JPEG_QUALITY)?.bytes
}