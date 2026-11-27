package core.util.bytearray

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.jetbrains.skia.Image

actual fun ByteArray.toImageBitmap(): ImageBitmap {
    val skiaImage = Image.makeFromEncoded(this)
    return skiaImage.toComposeImageBitmap()
}

actual fun ImageBitmap.toByteArray(): ByteArray? = null

actual fun ImageBitmap.toVideoThumbnailByteArray(): ByteArray? = null