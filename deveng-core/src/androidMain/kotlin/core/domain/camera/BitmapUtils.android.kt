package core.domain.camera

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import java.io.InputStream

object BitmapUtils {
    fun getBitmapFromUri(uri: Uri, contentResolver: ContentResolver): Bitmap? {
        try {
            val degrees = contentResolver.openInputStream(uri)?.use { stream ->
                try {
                    val exif = ExifInterface(stream)
                    when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                        ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                        ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                        ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                        else -> 0f
                    }
                } catch (_: Exception) {
                    0f
                }
            } ?: 0f

            var inputStream: InputStream? = null
            val bitmap: Bitmap?
            try {
                inputStream = contentResolver.openInputStream(uri)
                bitmap = BitmapFactory.decodeStream(inputStream)
            } finally {
                inputStream?.close()
            }

            if (bitmap == null || degrees == 0f) return bitmap

            return try {
                val matrix = Matrix().apply { postRotate(degrees) }
                val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                bitmap.recycle()
                rotated
            } catch (_: OutOfMemoryError) {
                bitmap
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
}
