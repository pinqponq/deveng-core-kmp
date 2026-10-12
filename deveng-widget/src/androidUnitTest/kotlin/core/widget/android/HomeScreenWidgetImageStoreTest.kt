package core.widget.android

import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class HomeScreenWidgetImageStoreTest {
    private val rootDirectory: File = Files.createTempDirectory("widget-image-store").toFile()
    private val imageDirectory = File(rootDirectory, IMAGE_DIRECTORY_NAME)
    private val imageStore = HomeScreenWidgetImageStore(imageDirectory = imageDirectory, imageFileName = IMAGE_FILE_NAME)

    @AfterTest
    fun tearDown() {
        rootDirectory.deleteRecursively()
    }

    @Test
    fun replaceImageAtomically_withMissingDirectory_createsItAndLeavesOnlyTheImage() {
        imageStore.replaceImageAtomically(FIRST_IMAGE_BYTES)

        assertContentEquals(FIRST_IMAGE_BYTES, imageStore.widgetImageFile.readBytes())
        assertEquals(listOf(IMAGE_FILE_NAME), imageDirectory.list()?.toList())
    }

    @Test
    fun replaceImageAtomically_overExistingImage_keepsTheNewestBytes() {
        imageStore.replaceImageAtomically(FIRST_IMAGE_BYTES)

        imageStore.replaceImageAtomically(SECOND_IMAGE_BYTES)

        assertContentEquals(SECOND_IMAGE_BYTES, imageStore.widgetImageFile.readBytes())
        assertEquals(listOf(IMAGE_FILE_NAME), imageDirectory.list()?.toList())
    }

    @Test
    fun deleteImage_removesTheStoredImage() {
        imageStore.replaceImageAtomically(FIRST_IMAGE_BYTES)

        imageStore.deleteImage()

        assertFalse(imageStore.widgetImageFile.exists())
    }

    private companion object {
        const val IMAGE_DIRECTORY_NAME = "home_screen_widget"
        const val IMAGE_FILE_NAME = "latest_widget.jpg"
        val FIRST_IMAGE_BYTES = byteArrayOf(1, 2, 3)
        val SECOND_IMAGE_BYTES = byteArrayOf(4, 5, 6, 7)
    }
}
