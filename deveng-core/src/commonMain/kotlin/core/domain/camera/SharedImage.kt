package core.domain.camera

import androidx.compose.ui.graphics.ImageBitmap

expect class SharedImage {
    val fileName: String?
    fun toByteArray(): ByteArray?
    fun toImageBitmap(): ImageBitmap?
}