package core.util.image

import android.media.ExifInterface
import java.io.File

/**
 * Logcat helpers for gallery export / EXIF orientation debugging.
 * Filter: `RindleGalleryExport`
 */
object ExifExportDiagnostics {

    const val LOG_TAG = "RindleGalleryExport"

    fun describeExif(imageBytes: ByteArray): String {
        if (imageBytes.isEmpty()) return "exif empty"
        val tempFile = File.createTempFile("exif_diag", ".jpg")
        return try {
            tempFile.writeBytes(imageBytes)
            val exif = ExifInterface(tempFile.absolutePath)
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL,
            )
            val stored = parseStoredSizeFromJpeg(imageBytes)
            val display = stored?.let { (w, h) -> displayDimensions(w, h, orientation) }
            buildString {
                append("exif orientation=$orientation (${orientationLabel(orientation)})")
                if (stored != null) {
                    append(" stored=${stored.first}x${stored.second}")
                }
                if (display != null) {
                    append(" display~=${display.first}x${display.second}")
                    append(" aspect~=${aspectLabel(display.first, display.second)}")
                }
                append(" hasGps=${exif.getLatLong(FloatArray(2))}")
            }
        } catch (e: Exception) {
            "exif readFailed ${e.message}"
        } finally {
            tempFile.delete()
        }
    }

    fun bytesChanged(before: ByteArray, after: ByteArray): String {
        val sameRef = before === after
        val sameContent = !sameRef && before.contentEquals(after)
        return "bytesChanged refSame=$sameRef contentSame=$sameContent before=${before.size} after=${after.size}"
    }

    private fun parseStoredSizeFromJpeg(bytes: ByteArray): Pair<Int, Int>? {
        val desc = JpegDebugProbe.describe(bytes)
        val match = Regex("stored=(\\d+)x(\\d+)").find(desc) ?: return null
        val w = match.groupValues[1].toIntOrNull() ?: return null
        val h = match.groupValues[2].toIntOrNull() ?: return null
        return w to h
    }

    private fun displayDimensions(storedW: Int, storedH: Int, orientation: Int): Pair<Int, Int> {
        return when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90,
            ExifInterface.ORIENTATION_ROTATE_270,
            ExifInterface.ORIENTATION_TRANSPOSE,
            ExifInterface.ORIENTATION_TRANSVERSE,
            -> storedH to storedW
            else -> storedW to storedH
        }
    }

    private fun orientationLabel(orientation: Int): String = when (orientation) {
        ExifInterface.ORIENTATION_NORMAL -> "NORMAL"
        ExifInterface.ORIENTATION_UNDEFINED -> "UNDEFINED"
        ExifInterface.ORIENTATION_ROTATE_90 -> "ROTATE_90"
        ExifInterface.ORIENTATION_ROTATE_180 -> "ROTATE_180"
        ExifInterface.ORIENTATION_ROTATE_270 -> "ROTATE_270"
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> "FLIP_H"
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> "FLIP_V"
        ExifInterface.ORIENTATION_TRANSPOSE -> "TRANSPOSE"
        ExifInterface.ORIENTATION_TRANSVERSE -> "TRANSVERSE"
        else -> "UNKNOWN_$orientation"
    }

    private fun aspectLabel(w: Int, h: Int): String {
        if (w <= 0 || h <= 0) return "?"
        val shortSide = minOf(w, h).toDouble()
        val longSide = maxOf(w, h).toDouble()
        val ratio = shortSide / longSide
        return when {
            kotlin.math.abs(ratio - 9.0 / 16.0) < 0.04 -> "9:16"
            kotlin.math.abs(ratio - 3.0 / 4.0) < 0.04 -> "3:4"
            kotlin.math.abs(ratio - 1.0) < 0.04 -> "1:1"
            ratio < 9.0 / 16.0 -> "portrait~tallerThan9:16"
            else -> "landscape~${String.format("%.2f", ratio)}"
        }
    }
}
