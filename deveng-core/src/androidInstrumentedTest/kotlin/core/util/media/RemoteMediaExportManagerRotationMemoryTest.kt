package core.util.media

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.ExifInterface
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import core.util.image.PhotoSaveUtils
import java.io.File
import java.net.ServerSocket
import java.net.SocketException
import kotlin.concurrent.thread
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RemoteMediaExportManagerRotationMemoryTest {
    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private val savedFileName = "rotation_memory_test_${System.currentTimeMillis()}.jpg"
    private lateinit var sourcePhoto: File
    private var heapBallast: List<ByteArray> = emptyList()
    private var photoServer: ServerSocket? = null

    @BeforeTest
    fun setUp() {
        PhotoSaveUtils.setApplicationContext(context)
        sourcePhoto = createRotatedJpeg()
    }

    @AfterTest
    fun tearDown() {
        heapBallast = emptyList()
        photoServer?.close()
        sourcePhoto.delete()
        findSavedPhotoUri()?.let { savedPhotoUri -> context.contentResolver.delete(savedPhotoUri, null, null) }
    }

    @Test
    fun saveMultipleFilesFromUrls_withRotated48MegapixelPhotoAndLowFreeHeap_savesUprightPhoto() = runTest(timeout = SAVE_TIMEOUT) {
        heapBallast = fillHeapUntilFree(bytes = FREE_HEAP_BYTES)

        val savedCount = RemoteMediaExportManager(context).saveMultipleFilesFromUrls(
            listOf(
                RemoteMediaFile(
                    fileUrl = servePhotoOnce(sourcePhoto),
                    fileName = savedFileName,
                    mimeType = JPEG_MIME_TYPE,
                ),
            ),
        )

        assertEquals(1, savedCount)
        val savedPhotoUri = checkNotNull(findSavedPhotoUri())
        val savedPhotoBounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(savedPhotoUri)?.use { inputStream ->
            BitmapFactory.decodeStream(inputStream, null, savedPhotoBounds)
        }

        val savedOrientation = context.contentResolver.openInputStream(savedPhotoUri)?.use { inputStream ->
            ExifInterface(inputStream).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED)
        }

        assertEquals(PHOTO_HEIGHT to PHOTO_WIDTH, savedPhotoBounds.outWidth to savedPhotoBounds.outHeight)
        assertEquals(ExifInterface.ORIENTATION_NORMAL, savedOrientation)
    }

    @Test
    fun imageBytesWithNormalOrientation_whenRotatedPhotoDoesNotFitInHeap_returnsOriginalBytes() {
        val rotatedPhotoBytes = sourcePhoto.readBytes()
        heapBallast = fillHeapUntilFree(bytes = TOO_LITTLE_FREE_HEAP_BYTES)

        val normalizedPhotoBytes = PhotoSaveUtils.imageBytesWithNormalOrientation(rotatedPhotoBytes)

        assertSame(rotatedPhotoBytes, normalizedPhotoBytes)
    }

    private fun createRotatedJpeg(): File {
        val bitmap = createNoiseBitmap()
        val photo = File(context.cacheDir, "rotation_memory_source.jpg")
        photo.outputStream().use { outputStream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, SOURCE_JPEG_QUALITY, outputStream)
        }

        bitmap.recycle()
        ExifInterface(photo.absolutePath).apply {
            setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_ROTATE_90.toString())
            saveAttributes()
        }

        return photo
    }

    private fun createNoiseBitmap(): Bitmap {
        val bitmap = Bitmap.createBitmap(PHOTO_WIDTH, PHOTO_HEIGHT, Bitmap.Config.ARGB_8888)
        val random = Random(seed = RANDOM_SEED)
        val row = IntArray(PHOTO_WIDTH)
        for (y in 0 until PHOTO_HEIGHT) {
            for (x in 0 until PHOTO_WIDTH) {
                val noise = random.nextInt(PIXEL_NOISE_RANGE)
                val red = (x * 255 / PHOTO_WIDTH + noise) and 0xFF
                val green = (y * 255 / PHOTO_HEIGHT + noise) and 0xFF
                row[x] = (0xFF shl 24) or (red shl 16) or (green shl 8) or noise
            }

            bitmap.setPixels(row, 0, PHOTO_WIDTH, 0, y, PHOTO_WIDTH, 1)
        }

        return bitmap
    }

    private fun fillHeapUntilFree(bytes: Long): List<ByteArray> {
        val runtime = Runtime.getRuntime()
        runtime.gc()
        val usedBytes = runtime.totalMemory() - runtime.freeMemory()
        val ballastChunkCount = ((runtime.maxMemory() - usedBytes - bytes) / BALLAST_CHUNK_BYTES).toInt()
        return List(ballastChunkCount.coerceAtLeast(0)) { ByteArray(BALLAST_CHUNK_BYTES) { 1 } }
    }

    private fun servePhotoOnce(photo: File): String {
        val server = ServerSocket(0).also { photoServer = it }
        thread(name = "rotation-memory-test-server") {
            try {
                server.use { respondWithPhoto(server, photo) }
            } catch (e: SocketException) {
                // tearDown closes the server when the save never connected. The test failure then reports why.
                Log.w(LOG_TAG, "photo server closed before the save connected", e)
            }
        }

        return "http://127.0.0.1:${server.localPort}/photo.jpg"
    }

    private fun respondWithPhoto(server: ServerSocket, photo: File) {
        server.accept().use { socket ->
            val reader = socket.getInputStream().bufferedReader()
            while (reader.readLine()?.isNotEmpty() == true) Unit
            val header = "HTTP/1.1 200 OK\r\nContent-Type: $JPEG_MIME_TYPE\r\n" +
                "Content-Length: ${photo.length()}\r\nConnection: close\r\n\r\n"
            socket.getOutputStream().apply {
                write(header.toByteArray())
                photo.inputStream().use { photoStream -> photoStream.copyTo(this) }
                flush()
            }
        }
    }

    private fun findSavedPhotoUri(): Uri? =
        context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            arrayOf(MediaStore.Images.Media._ID),
            "${MediaStore.Images.Media.DISPLAY_NAME} = ?",
            arrayOf(savedFileName),
            null,
        )?.use { cursor ->
            if (!cursor.moveToFirst()) return@use null
            ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, cursor.getLong(0))
        }

    private companion object {
        const val PHOTO_WIDTH = 8_000
        const val PHOTO_HEIGHT = 6_000
        const val SOURCE_JPEG_QUALITY = 92
        const val PIXEL_NOISE_RANGE = 48
        const val RANDOM_SEED = 475
        const val JPEG_MIME_TYPE = "image/jpeg"
        const val LOG_TAG = "RotationMemoryTest"
        const val BALLAST_CHUNK_BYTES = 1_048_576
        val SAVE_TIMEOUT = 3.minutes

        // On a 192 MB heap, rotating this photo needs about 70 MB above the app's own heap.
        const val FREE_HEAP_BYTES = 110L * 1_048_576

        // Less than the 44 MB the upright copy of this photo needs as one array.
        const val TOO_LITTLE_FREE_HEAP_BYTES = 30L * 1_048_576
    }
}
