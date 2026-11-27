package core.util.bytearray

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.ByteArrayOutputStream

actual fun ByteArray.toImageBitmap(): ImageBitmap {
    val bmp = BitmapFactory.decodeByteArray(this, 0, size)
    return bmp.asImageBitmap()
}

actual fun ImageBitmap.toByteArray(): ByteArray? {
    return try {
        val bitmap: Bitmap = this.asAndroidBitmap()
        ByteArrayOutputStream().use { out ->
            if (bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)) {
                out.toByteArray()
            } else {
                null
            }
        }
    } catch (_: UnsupportedOperationException) {
        null
    }
}

actual fun ImageBitmap.toVideoThumbnailByteArray(): ByteArray? {
    return try {
        val bitmap: Bitmap = this.asAndroidBitmap()
        ByteArrayOutputStream().use { out ->
            if (bitmap.compress(Bitmap.CompressFormat.JPEG, VIDEO_THUMBNAIL_JPEG_QUALITY, out)) {
                out.toByteArray()
            } else {
                null
            }
        }
    } catch (_: UnsupportedOperationException) {
        null
    }
}