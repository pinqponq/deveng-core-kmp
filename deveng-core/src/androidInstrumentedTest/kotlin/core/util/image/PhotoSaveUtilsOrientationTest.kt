package core.util.image

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PhotoSaveUtilsOrientationTest {

    @Test
    fun imageBytesWithNormalOrientation_withRotatedPhotoAndLocation_keepsLocation() {
        val rotatedPhotoBytes = PhotoSaveUtils.addLocationExif(createRotatedJpegBytes(), LATITUDE, LONGITUDE)

        val normalizedPhotoBytes = PhotoSaveUtils.imageBytesWithNormalOrientation(rotatedPhotoBytes)

        val location = assertNotNull(PhotoSaveUtils.readLocationFromExif(normalizedPhotoBytes))
        assertEquals(LATITUDE, location.first, LOCATION_TOLERANCE)
        assertEquals(LONGITUDE, location.second, LOCATION_TOLERANCE)
    }

    @Test
    fun imageBytesWithNormalOrientation_withRotatedPhotoAndLocation_returnsUprightPhoto() {
        val rotatedPhotoBytes = PhotoSaveUtils.addLocationExif(createRotatedJpegBytes(), LATITUDE, LONGITUDE)

        val normalizedPhotoBytes = PhotoSaveUtils.imageBytesWithNormalOrientation(rotatedPhotoBytes)

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
        val tempFilesBefore = PhotoTestFixtures.uprightTempFileNames()

        PhotoSaveUtils.imageBytesWithNormalOrientation(createRotatedJpegBytes())

        assertEquals(tempFilesBefore, PhotoTestFixtures.uprightTempFileNames())
    }

    private fun createRotatedJpegBytes(): ByteArray {
        val photo = File.createTempFile("orientation_test_source", ".jpg")
        try {
            PhotoTestFixtures.writeRotatedJpeg(target = photo, width = PHOTO_WIDTH, height = PHOTO_HEIGHT)
            return photo.readBytes()
        } finally {
            photo.delete()
        }
    }

    private fun encodeUprightInMemory(rotatedPhotoBytes: ByteArray): ByteArray {
        val bitmap = BitmapFactory.decodeByteArray(rotatedPhotoBytes, 0, rotatedPhotoBytes.size)
        val matrix = Matrix().apply { postRotate(ROTATION_DEGREES) }
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

    private companion object {
        const val PHOTO_WIDTH = 400
        const val PHOTO_HEIGHT = 300
        const val FULL_JPEG_QUALITY = 100
        const val ROTATION_DEGREES = 90f
        const val LATITUDE = 41.0082
        const val LONGITUDE = 28.9784
        const val LOCATION_TOLERANCE = 0.0001
    }
}
