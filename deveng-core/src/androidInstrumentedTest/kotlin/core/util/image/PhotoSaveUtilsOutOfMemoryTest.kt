package core.util.image

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PhotoSaveUtilsOutOfMemoryTest {
    private val sourcePhoto = File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir, "out_of_memory_source.jpg")
    private var heapBallast: List<ByteArray> = emptyList()

    @BeforeTest
    fun setUp() {
        PhotoTestFixtures.writeRotatedJpeg(target = sourcePhoto, width = PHOTO_WIDTH, height = PHOTO_HEIGHT)
    }

    @AfterTest
    fun tearDown() {
        heapBallast = emptyList()
        sourcePhoto.delete()
    }

    @Test
    fun imageBytesWithNormalOrientation_whenRotatedPhotoDoesNotFitInHeap_returnsOriginalBytes() {
        val rotatedPhotoBytes = sourcePhoto.readBytes()
        heapBallast = PhotoTestFixtures.fillHeapUntilFree(bytes = TOO_LITTLE_FREE_HEAP_BYTES)

        val normalizedPhotoBytes = PhotoSaveUtils.imageBytesWithNormalOrientation(rotatedPhotoBytes)

        assertSame(rotatedPhotoBytes, normalizedPhotoBytes)
    }

    @Test
    fun imageBytesWithNormalOrientation_whenRotatedPhotoDoesNotFitInHeap_removesUprightTempFile() {
        val rotatedPhotoBytes = sourcePhoto.readBytes()
        val tempFilesBefore = PhotoTestFixtures.uprightTempFileNames()
        heapBallast = PhotoTestFixtures.fillHeapUntilFree(bytes = TOO_LITTLE_FREE_HEAP_BYTES)

        PhotoSaveUtils.imageBytesWithNormalOrientation(rotatedPhotoBytes)

        heapBallast = emptyList()
        assertEquals(tempFilesBefore, PhotoTestFixtures.uprightTempFileNames())
    }

    private companion object {
        const val PHOTO_WIDTH = 8_000
        const val PHOTO_HEIGHT = 6_000

        // The upright JPEG of this photo is read back as one array of 44.7 MB
        // (OutOfMemoryError in encodeUprightJpeg on the Medium_Phone emulator, API 37, 192 MB heap limit).
        const val TOO_LITTLE_FREE_HEAP_BYTES = 30L * 1_048_576
    }
}
