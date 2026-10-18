package core.util.image

import android.graphics.Bitmap
import android.media.ExifInterface
import java.io.File
import kotlin.random.Random

internal object PhotoTestFixtures {
    const val UPRIGHT_TEMP_FILE_PREFIX = "exif_orient_out"

    fun writeRotatedJpeg(target: File, width: Int, height: Int) {
        val bitmap = createNoiseBitmap(width, height)
        target.outputStream().use { outputStream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, SOURCE_JPEG_QUALITY, outputStream)
        }

        bitmap.recycle()
        ExifInterface(target.absolutePath).apply {
            setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_ROTATE_90.toString())
            saveAttributes()
        }
    }

    fun fillHeapUntilFree(bytes: Long): List<ByteArray> {
        val runtime = Runtime.getRuntime()
        runtime.gc()
        val usedBytes = runtime.totalMemory() - runtime.freeMemory()
        val ballastChunkCount = ((runtime.maxMemory() - usedBytes - bytes) / BALLAST_CHUNK_BYTES).toInt()
        return List(ballastChunkCount.coerceAtLeast(0)) { ByteArray(BALLAST_CHUNK_BYTES) { 1 } }
    }

    fun uprightTempFileNames(): Set<String> =
        File(checkNotNull(System.getProperty("java.io.tmpdir")))
            .listFiles { file -> file.name.startsWith(UPRIGHT_TEMP_FILE_PREFIX) }
            .orEmpty()
            .map { file -> file.name }
            .toSet()

    private fun createNoiseBitmap(width: Int, height: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val random = Random(seed = RANDOM_SEED)
        val row = IntArray(width)
        for (y in 0 until height) {
            for (x in 0 until width) {
                val noise = random.nextInt(PIXEL_NOISE_RANGE)
                val red = (x * MAX_CHANNEL / width + noise) and MAX_CHANNEL
                val green = (y * MAX_CHANNEL / height + noise) and MAX_CHANNEL
                row[x] = OPAQUE_ALPHA_MASK or (red shl RED_SHIFT) or (green shl GREEN_SHIFT) or noise
            }

            bitmap.setPixels(row, 0, width, 0, y, width, 1)
        }

        return bitmap
    }

    private const val SOURCE_JPEG_QUALITY = 92
    private const val PIXEL_NOISE_RANGE = 48
    private const val RANDOM_SEED = 475
    private const val MAX_CHANNEL = 0xFF
    private const val OPAQUE_ALPHA_MASK = 0xFF shl 24
    private const val RED_SHIFT = 16
    private const val GREEN_SHIFT = 8
    private const val BALLAST_CHUNK_BYTES = 1_048_576
}
