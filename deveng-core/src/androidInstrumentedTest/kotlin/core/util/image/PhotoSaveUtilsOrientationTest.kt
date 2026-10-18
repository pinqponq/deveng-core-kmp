package core.util.image

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PhotoSaveUtilsOrientationTest {

    @Test
    fun imageBytesWithNormalOrientation_withRotatedPhotoAndLocation_keepsLocationAndSetsNormalOrientation() {
        val rotatedPhotoBytes = PhotoSaveUtils.addLocationExif(createRotatedJpegBytes(), LATITUDE, LONGITUDE)

        val normalizedPhotoBytes = PhotoSaveUtils.imageBytesWithNormalOrientation(rotatedPhotoBytes)

        val location = assertNotNull(PhotoSaveUtils.readLocationFromExif(normalizedPhotoBytes))
        assertEquals(LATITUDE, location.first, LOCATION_TOLERANCE)
        assertEquals(LONGITUDE, location.second, LOCATION_TOLERANCE)
        assertEquals(ExifInterface.ORIENTATION_NORMAL, readOrientation(normalizedPhotoBytes))
        assertEquals(PHOTO_HEIGHT to PHOTO_WIDTH, readSize(normalizedPhotoBytes))
    }

    @Test
    fun imageBytesWithNormalOrientation_withRotatedPhotoWithoutLocation_returnsUprightPhotoWithoutLocation() {
        val normalizedPhotoBytes = PhotoSaveUtils.imageBytesWithNormalOrientation(createRotatedJpegBytes())

        assertNull(PhotoSaveUtils.readLocationFromExif(normalizedPhotoBytes))
        assertEquals(ExifInterface.ORIENTATION_NORMAL, readOrientation(normalizedPhotoBytes))
        assertEquals(PHOTO_HEIGHT to PHOTO_WIDTH, readSize(normalizedPhotoBytes))
    }

    @Test
    fun imageBytesWithNormalOrientation_withRotatedPhoto_matchesInMemoryJpegAtFullQuality() {
        val rotatedPhotoBytes = createRotatedJpegBytes()
        val inMemoryUprightBytes = encodeUprightInMemory(rotatedPhotoBytes)

        val normalizedPhotoBytes = PhotoSaveUtils.imageBytesWithNormalOrientation(rotatedPhotoBytes)

        assertEquals(readSize(inMemoryUprightBytes), readSize(normalizedPhotoBytes))
        assertContentEquals(readPixels(inMemoryUprightBytes), readPixels(normalizedPhotoBytes))
    }

    @Test
    fun imageBytesWithNormalOrientation_withRotatedPhoto_removesUprightTempFile() {
        val tempFilesBefore = uprightTempFileNames()

        PhotoSaveUtils.imageBytesWithNormalOrientation(createRotatedJpegBytes())

        assertEquals(tempFilesBefore, uprightTempFileNames())
    }

    private fun createRotatedJpegBytes(): ByteArray {
        val bitmap = Bitmap.createBitmap(PHOTO_WIDTH, PHOTO_HEIGHT, Bitmap.Config.ARGB_8888)
        val random = Random(seed = RANDOM_SEED)
        val pixels = IntArray(PHOTO_WIDTH * PHOTO_HEIGHT) { (0xFF shl 24) or random.nextInt(0x1000000) }
        bitmap.setPixels(pixels, 0, PHOTO_WIDTH, 0, 0, PHOTO_WIDTH, PHOTO_HEIGHT)
        val photo = File.createTempFile("orientation_test_source", ".jpg")
        try {
            photo.outputStream().use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, SOURCE_JPEG_QUALITY, outputStream)
            }

            bitmap.recycle()
            ExifInterface(photo.absolutePath).apply {
                setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_ROTATE_90.toString())
                saveAttributes()
            }

            return photo.readBytes()
        } finally {
            photo.delete()
        }
    }

    private fun encodeUprightInMemory(rotatedPhotoBytes: ByteArray): ByteArray {
        val bitmap = BitmapFactory.decodeByteArray(rotatedPhotoBytes, 0, rotatedPhotoBytes.size)
        val matrix = Matrix().apply { postRotate(90f) }
        val upright = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        val outputStream = ByteArrayOutputStream()
        upright.compress(Bitmap.CompressFormat.JPEG, FULL_JPEG_QUALITY, outputStream)
        bitmap.recycle()
        upright.recycle()
        return outputStream.toByteArray()
    }

    private fun readOrientation(photoBytes: ByteArray): Int =
        ExifInterface(ByteArrayInputStream(photoBytes))
            .getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED)

    private fun readSize(photoBytes: ByteArray): Pair<Int, Int> {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(photoBytes, 0, photoBytes.size, bounds)
        return bounds.outWidth to bounds.outHeight
    }

    private fun readPixels(photoBytes: ByteArray): IntArray {
        val bitmap = BitmapFactory.decodeByteArray(photoBytes, 0, photoBytes.size)
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        bitmap.recycle()
        return pixels
    }

    private fun uprightTempFileNames(): Set<String> =
        File(checkNotNull(System.getProperty("java.io.tmpdir")))
            .listFiles { file -> file.name.startsWith(UPRIGHT_TEMP_FILE_PREFIX) }
            .orEmpty()
            .map { file -> file.name }
            .toSet()

    private companion object {
        const val PHOTO_WIDTH = 400
        const val PHOTO_HEIGHT = 300
        const val SOURCE_JPEG_QUALITY = 92
        const val FULL_JPEG_QUALITY = 100
        const val RANDOM_SEED = 475
        const val LATITUDE = 41.0082
        const val LONGITUDE = 28.9784
        const val LOCATION_TOLERANCE = 0.0001
        const val UPRIGHT_TEMP_FILE_PREFIX = "exif_orient_out"
    }
}
