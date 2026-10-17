package core.util.media

import android.app.ActivityManager
import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.media.ExifInterface
import android.os.Debug
import android.provider.MediaStore
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import core.util.image.PhotoSaveUtils
import java.io.File
import java.net.ServerSocket
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlin.concurrent.thread
import kotlin.random.Random
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Measurement for rindle-cmp#475, not a regression test: runs the real "Save selected" path
 * (RemoteMediaExportManager.saveMultipleFilesFromUrls) for one generated JPEG served over local HTTP and
 * records the peak Java heap and native heap while it runs. Run one method per process.
 */
@RunWith(AndroidJUnit4::class)
class SaveSelectedMemoryMeasurement {
    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun rotated12mp() = measure(width = 4000, height = 3000, orientation = ExifInterface.ORIENTATION_ROTATE_90)

    @Test fun rotated48mp() = measure(width = 8000, height = 6000, orientation = ExifInterface.ORIENTATION_ROTATE_90)

    @Test fun normal12mp() = measure(width = 4000, height = 3000, orientation = ExifInterface.ORIENTATION_NORMAL)

    @Test fun normal48mp() = measure(width = 8000, height = 6000, orientation = ExifInterface.ORIENTATION_NORMAL)

    private fun measure(width: Int, height: Int, orientation: Int) {
        PhotoSaveUtils.setApplicationContext(context)
        val label = "${width}x$height-o$orientation"
        val jpeg = generateJpeg(width, height, orientation)
        val ballastMb = InstrumentationRegistry.getArguments().getString("ballastMb")?.toInt() ?: 0
        val ballast = List(ballastMb) { ByteArray(1_048_576) { it.toByte() } }
        repeat(3) { Runtime.getRuntime().gc(); Thread.sleep(300) }

        val runtime = Runtime.getRuntime()
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo().also(activityManager::getMemoryInfo)
        val baseJava = runtime.totalMemory() - runtime.freeMemory()
        val baseNative = Debug.getNativeHeapAllocatedSize()
        val peakJava = AtomicLong(baseJava)
        val peakNative = AtomicLong(baseNative)
        val sampling = AtomicBoolean(true)
        val sampler = thread(name = "heap-sampler") {
            while (sampling.get()) {
                peakJava.accumulateAndGet(runtime.totalMemory() - runtime.freeMemory(), ::maxOf)
                peakNative.accumulateAndGet(Debug.getNativeHeapAllocatedSize(), ::maxOf)
                Thread.sleep(2)
            }
        }

        val server = ServerSocket(0)
        val serverThread = thread(name = "jpeg-server") {
            server.accept().use { socket ->
                socket.getInputStream().bufferedReader().let { reader ->
                    while (reader.readLine()?.isNotEmpty() == true) Unit
                }
                socket.getOutputStream().apply {
                    write(
                        ("HTTP/1.1 200 OK\r\nContent-Type: image/jpeg\r\nContent-Length: ${jpeg.length()}\r\n" +
                            "Connection: close\r\n\r\n").toByteArray(),
                    )
                    jpeg.inputStream().use { it.copyTo(this) }
                    flush()
                }
            }
        }

        val fileName = "measure475_${label}_${System.currentTimeMillis()}.jpg"
        val startNanos = System.nanoTime()
        var error: Throwable? = null
        val savedCount = try {
            runBlocking {
                RemoteMediaExportManager(context).saveMultipleFilesFromUrls(
                    listOf(
                        RemoteMediaFile(
                            fileUrl = "http://127.0.0.1:${server.localPort}/photo.jpg",
                            fileName = fileName,
                            mimeType = "image/jpeg",
                        ),
                    ),
                )
            }
        } catch (throwable: Throwable) {
            error = throwable
            -1
        }
        val durationMs = (System.nanoTime() - startNanos) / 1_000_000
        sampling.set(false)
        sampler.join()
        serverThread.join(5_000)
        server.close()

        val savedOrientation = savedImageOrientation(fileName)
        val mb = { bytes: Long -> "%.1f".format(bytes / 1_048_576.0) }
        val result = "RESULT label=$label ballastMb=${ballast.size} jpegBytes=${jpeg.length()} savedCount=$savedCount " +
            "savedOrientation=$savedOrientation error=${error?.javaClass?.simpleName} durationMs=$durationMs " +
            "javaHeapLimitMb=${mb(runtime.maxMemory())} memoryClassMb=${activityManager.memoryClass} " +
            "deviceRamMb=${mb(memoryInfo.totalMem)} availRamMb=${mb(memoryInfo.availMem)} " +
            "lowRam=${activityManager.isLowRamDevice} " +
            "baseJavaMb=${mb(baseJava)} peakJavaMb=${mb(peakJava.get())} " +
            "peakJavaDeltaMb=${mb(peakJava.get() - baseJava)} " +
            "baseNativeMb=${mb(baseNative)} peakNativeMb=${mb(peakNative.get())} " +
            "peakNativeDeltaMb=${mb(peakNative.get() - baseNative)}"
        Log.i(TAG, result)
        InstrumentationRegistry.getInstrumentation().sendStatus(0, android.os.Bundle().apply { putString("stream", result + "\n") })
        jpeg.delete()
    }

    private fun generateJpeg(width: Int, height: Int, orientation: Int): File {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val random = Random(seed = 475)
        val row = IntArray(width)
        for (y in 0 until height) {
            for (x in 0 until width) {
                val noise = random.nextInt(48)
                val r = (x * 255 / width + noise) and 0xFF
                val g = (y * 255 / height + noise) and 0xFF
                val b = ((x + y) * 127 / (width + height) + noise) and 0xFF
                row[x] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
            }
            bitmap.setPixels(row, 0, width, 0, y, width, 1)
        }
        val file = File(context.cacheDir, "measure475_${width}x$height.jpg")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 92, it) }
        bitmap.recycle()
        ExifInterface(file.absolutePath).apply {
            setAttribute(ExifInterface.TAG_ORIENTATION, orientation.toString())
            saveAttributes()
        }
        return file
    }

    private fun savedImageOrientation(fileName: String): String {
        val resolver = context.contentResolver
        resolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            arrayOf(MediaStore.Images.Media._ID, MediaStore.Images.Media.WIDTH, MediaStore.Images.Media.HEIGHT),
            "${MediaStore.Images.Media.DISPLAY_NAME} = ?",
            arrayOf(fileName),
            null,
        )?.use { cursor ->
            if (!cursor.moveToFirst()) return "not-saved"
            val uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, cursor.getLong(0))
            val exifOrientation = resolver.openInputStream(uri)?.use { ExifInterface(it) }
                ?.getAttributeInt(ExifInterface.TAG_ORIENTATION, -1)
            val size = "${cursor.getInt(1)}x${cursor.getInt(2)}"
            resolver.delete(uri, null, null)
            return "exif$exifOrientation-$size"
        }
        return "query-failed"
    }

    private companion object {
        const val TAG = "SaveSelectedMeasure"
    }
}
