package core.util.bytearray

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlin.math.max

actual class ImageSizeProcessor {
    actual suspend fun resizeAndCompressBytes(
        inputBytes: ByteArray,
        targetMaxSizePx: Int,
        quality: Int
    ): ByteArray {
        if (inputBytes.isEmpty()) {
            return inputBytes
        }

        val boundsOptions = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeByteArray(inputBytes, 0, inputBytes.size, boundsOptions)

        val sourceWidth = boundsOptions.outWidth
        val sourceHeight = boundsOptions.outHeight
        if (sourceWidth <= 0 || sourceHeight <= 0) return inputBytes

        val largestSide = max(sourceWidth, sourceHeight)

        val sampleSize =
            if (largestSide > targetMaxSizePx) (largestSide / targetMaxSizePx).coerceAtLeast(1)
            else 1

        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
        }

        val decodedBitmap = BitmapFactory.decodeByteArray(
            inputBytes,
            0,
            inputBytes.size,
            decodeOptions
        ) ?: return inputBytes

        val rotatedBitmap = applyExifRotation(decodedBitmap, inputBytes)

        val resizedBitmap = scaleBitmapIfNeeded(
            bitmap = rotatedBitmap,
            targetMaxSizePx = targetMaxSizePx
        )

        val jpegBytes = compressToJpeg(
            bitmap = resizedBitmap,
            quality = quality
        )

        if (resizedBitmap !== rotatedBitmap) resizedBitmap.recycle()
        if (rotatedBitmap !== decodedBitmap) rotatedBitmap.recycle()
        decodedBitmap.recycle()

        return jpegBytes
    }

    private fun applyExifRotation(bitmap: Bitmap, inputBytes: ByteArray): Bitmap {
        val degrees = try {
            val exif = ExifInterface(ByteArrayInputStream(inputBytes))
            when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
        } catch (_: Exception) {
            0f
        }
        if (degrees == 0f) return bitmap
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun scaleBitmapIfNeeded(
        bitmap: Bitmap,
        targetMaxSizePx: Int
    ): Bitmap {
        val currentWidth = bitmap.width
        val currentHeight = bitmap.height
        val largestSide = max(currentWidth, currentHeight)

        if (largestSide <= targetMaxSizePx) {
            return bitmap
        }

        val scaleFactor = targetMaxSizePx.toFloat() / largestSide.toFloat()
        val targetWidth = (currentWidth * scaleFactor).toInt()
        val targetHeight = (currentHeight * scaleFactor).toInt()

        return Bitmap.createScaledBitmap(
            bitmap,
            targetWidth,
            targetHeight,
            true
        )
    }

    private fun compressToJpeg(
        bitmap: Bitmap,
        quality: Int
    ): ByteArray {
        val output = ByteArrayOutputStream()
        bitmap.compress(
            Bitmap.CompressFormat.JPEG,
            quality.coerceIn(1, 100),
            output
        )
        return output.toByteArray()
    }
}
