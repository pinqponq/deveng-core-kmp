package core.domain.camera

import androidx.compose.ui.graphics.ImageBitmap

actual class SharedImage(
    actual val fileName: String? = null
) {
    actual fun toByteArray(): ByteArray? {
        TODO("Not yet implemented")
    }

    actual fun toImageBitmap(): ImageBitmap? {
        TODO("Not yet implemented")
    }
}