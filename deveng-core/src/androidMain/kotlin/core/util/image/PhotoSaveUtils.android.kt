package core.util.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.media.MediaScannerConnection
import android.util.Log
import java.io.ByteArrayOutputStream
import java.io.File

actual object PhotoSaveUtils {

    @Volatile
    private var appContext: Context? = null

    actual fun setApplicationContext(context: Any?) {
        appContext = (context as? Context)?.applicationContext
    }

    actual fun imageBytesWithNormalOrientation(imageBytes: ByteArray): ByteArray {
        Log.d(
            ExifExportDiagnostics.LOG_TAG,
            "orientationNormalize START ${JpegDebugProbe.describe(imageBytes)} " +
                ExifExportDiagnostics.describeExif(imageBytes),
        )
        return try {
            normalizeOrientationInternal(imageBytes)
        } catch (e: Exception) {
            Log.w(ExifExportDiagnostics.LOG_TAG, "orientationNormalize FAILED ${e.message}", e)
            imageBytes
        }.also { result ->
            Log.d(
                ExifExportDiagnostics.LOG_TAG,
                "orientationNormalize END ${ExifExportDiagnostics.bytesChanged(imageBytes, result)} " +
                    "${JpegDebugProbe.describe(result)} ${ExifExportDiagnostics.describeExif(result)}",
            )
        }
    }

    private fun normalizeOrientationInternal(imageBytes: ByteArray): ByteArray {
        val tempFile = File.createTempFile("exif_orient", ".jpg")
        try {
            tempFile.writeBytes(imageBytes)
            val exif = ExifInterface(tempFile.absolutePath)
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL,
            )
            if (orientation == ExifInterface.ORIENTATION_NORMAL ||
                orientation == ExifInterface.ORIENTATION_UNDEFINED
            ) {
                Log.d(
                    ExifExportDiagnostics.LOG_TAG,
                    "orientationNormalize SKIP alreadyNormal orientation=$orientation",
                )
                return imageBytes
            }
            Log.d(
                ExifExportDiagnostics.LOG_TAG,
                "orientationNormalize APPLY orientation=$orientation",
            )
            val gps = readLocationFromExif(imageBytes)
            var bitmap = BitmapFactory.decodeFile(tempFile.absolutePath, null) ?: return imageBytes
            try {
                bitmap = applyExifOrientationToBitmap(bitmap, orientation)
                val out = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
                var result = out.toByteArray()
                val normalizedFile = File.createTempFile("exif_orient_out", ".jpg")
                try {
                    normalizedFile.writeBytes(result)
                    val outExif = ExifInterface(normalizedFile.absolutePath)
                    outExif.setAttribute(
                        ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_NORMAL.toString(),
                    )
                    outExif.saveAttributes()
                    result = normalizedFile.readBytes()
                } finally {
                    normalizedFile.delete()
                }
                if (gps != null) {
                    result = addLocationExif(result, gps.first, gps.second)
                }
                return result
            } finally {
                if (!bitmap.isRecycled) bitmap.recycle()
            }
        } finally {
            tempFile.delete()
        }
        return imageBytes
    }

    actual fun savePhoto(imageBytes: ByteArray, targetPath: String): SavePhotoResult = try {
        val file = File(targetPath)
        file.parentFile?.mkdirs()
        file.writeBytes(imageBytes)
        appContext?.let { ctx ->
            MediaScannerConnection.scanFile(
                ctx,
                arrayOf(targetPath),
                arrayOf("image/jpeg"),
                null,
            )
        }
        SavePhotoResult.Success(targetPath)
    } catch (e: Exception) {
        SavePhotoResult.Error(e)
    }

    actual fun addLocationExif(
        imageBytes: ByteArray,
        latitude: Double,
        longitude: Double,
    ): ByteArray = try {
        val tempFile = File.createTempFile("exif_location", ".jpg")
        try {
            tempFile.writeBytes(imageBytes)
            val exif = ExifInterface(tempFile.absolutePath)
            exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE, toDmsString(latitude))
            exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, if (latitude >= 0) "N" else "S")
            exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, toDmsString(longitude))
            exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, if (longitude >= 0) "E" else "W")
            exif.saveAttributes()
            tempFile.readBytes()
        } finally {
            tempFile.delete()
        }
    } catch (e: Exception) {
        imageBytes
    }

    actual fun readLocationFromExif(imageBytes: ByteArray): Pair<Double, Double>? = try {
        val tempFile = File.createTempFile("exif_read_gps", ".jpg")
        try {
            tempFile.writeBytes(imageBytes)
            val exif = ExifInterface(tempFile.absolutePath)
            val latLong = FloatArray(2)
            if (exif.getLatLong(latLong)) {
                latLong[0].toDouble() to latLong[1].toDouble()
            } else {
                null
            }
        } finally {
            tempFile.delete()
        }
    } catch (_: Exception) {
        null
    }

    actual fun readCaptureDateTimeFromExif(imageBytes: ByteArray): ExifCaptureDateTime? = try {
        val tempFile = File.createTempFile("exif_read_datetime", ".jpg")
        try {
            tempFile.writeBytes(imageBytes)
            val exif = ExifInterface(tempFile.absolutePath)
            val dateTime = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
                ?: exif.getAttribute(ExifInterface.TAG_DATETIME)
            val offset = exif.getAttribute(ExifInterface.TAG_OFFSET_TIME_ORIGINAL)
                ?: exif.getAttribute(ExifInterface.TAG_OFFSET_TIME)
            parseExifDateTime(raw = dateTime, offsetRaw = offset)
        } finally {
            tempFile.delete()
        }
    } catch (e: Exception) {
        Log.w(ExifExportDiagnostics.LOG_TAG, "readCaptureDateTimeFromExif failed: ${e.message}", e)
        null
    }

    private fun applyExifOrientationToBitmap(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.postRotate(90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.postRotate(270f)
                matrix.postScale(-1f, 1f)
            }
            else -> return bitmap
        }
        if (matrix.isIdentity) {
            return bitmap
        }
        val transformed = Bitmap.createBitmap(
            bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true,
        )
        if (transformed !== bitmap) {
            bitmap.recycle()
        }
        return transformed
    }

    /**
     * Converts decimal degrees to EXIF DMS string: "degrees/1,minutes/1,seconds/1000"
     */
    private fun toDmsString(decimalDegrees: Double): String {
        val abs = kotlin.math.abs(decimalDegrees)
        val degrees = abs.toInt()
        val minutesDecimal = (abs - degrees) * 60
        val minutes = minutesDecimal.toInt()
        val seconds = (minutesDecimal - minutes) * 60
        val secondsRational = (seconds * 1000).toInt()
        return "${degrees}/1,${minutes}/1,$secondsRational/1000"
    }
}
