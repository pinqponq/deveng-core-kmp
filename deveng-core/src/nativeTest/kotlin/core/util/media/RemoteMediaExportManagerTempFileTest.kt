package core.util.media

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.test.runTest
import platform.Foundation.NSFileManager
import platform.Foundation.NSString
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.dataUsingEncoding

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
class RemoteMediaExportManagerTempFileTest {

    private val fileManager = NSFileManager.defaultManager
    private val sourceFile = NSURL.fileURLWithPath(NSTemporaryDirectory() + SOURCE_FILE_NAME)
    private val tempFileExistedAtSaveList = mutableListOf<Boolean>()

    @BeforeTest
    fun setUp() {
        fileManager.createFileAtPath(
            path = checkNotNull(sourceFile.path),
            contents = NSString.create(string = SOURCE_FILE_CONTENT).dataUsingEncoding(NSUTF8StringEncoding),
            attributes = null,
        )
    }

    @AfterTest
    fun tearDown() {
        listOf(SOURCE_FILE_NAME, FIRST_SAVED_FILE_NAME, SECOND_SAVED_FILE_NAME).forEach { fileName ->
            fileManager.removeItemAtPath(NSTemporaryDirectory() + fileName, error = null)
        }
    }

    @Test
    fun saveSingleFileFromUrl_whenPhotosSaveSucceeds_removesTempFile() = runTest {
        val manager = RemoteMediaExportManager(saveToPhotos = recordingPhotosSave(isSaved = true))

        val isSaved = manager.saveSingleFileFromUrl(
            fileUrl = checkNotNull(sourceFile.absoluteString),
            fileName = FIRST_SAVED_FILE_NAME,
            mimeType = JPEG_MIME_TYPE,
        )

        assertTrue(isSaved)
        assertEquals(listOf(true), tempFileExistedAtSaveList)
        assertFalse(isInTempDirectory(FIRST_SAVED_FILE_NAME))
    }

    @Test
    fun saveSingleFileFromUrl_whenPhotosSaveFails_removesTempFile() = runTest {
        val manager = RemoteMediaExportManager(saveToPhotos = recordingPhotosSave(isSaved = false))

        val isSaved = manager.saveSingleFileFromUrl(
            fileUrl = checkNotNull(sourceFile.absoluteString),
            fileName = FIRST_SAVED_FILE_NAME,
            mimeType = JPEG_MIME_TYPE,
        )

        assertFalse(isSaved)
        assertEquals(listOf(true), tempFileExistedAtSaveList)
        assertFalse(isInTempDirectory(FIRST_SAVED_FILE_NAME))
    }

    @Test
    fun saveMultipleFilesFromUrls_withOneFailedPhotosSave_removesEveryTempFile() = runTest {
        var saveCount = 0
        val manager = RemoteMediaExportManager(
            saveToPhotos = { fileUrl, _ ->
                tempFileExistedAtSaveList += fileManager.fileExistsAtPath(checkNotNull(fileUrl.path))
                saveCount++ == 0
            },
        )

        val savedCount = manager.saveMultipleFilesFromUrls(
            listOf(FIRST_SAVED_FILE_NAME, SECOND_SAVED_FILE_NAME).map { fileName ->
                RemoteMediaFile(
                    fileUrl = checkNotNull(sourceFile.absoluteString),
                    fileName = fileName,
                    mimeType = JPEG_MIME_TYPE,
                )
            },
        )

        assertEquals(1, savedCount)
        assertEquals(listOf(true, true), tempFileExistedAtSaveList)
        assertFalse(isInTempDirectory(FIRST_SAVED_FILE_NAME))
        assertFalse(isInTempDirectory(SECOND_SAVED_FILE_NAME))
    }

    private fun recordingPhotosSave(isSaved: Boolean): (NSURL, String) -> Boolean = { fileUrl, _ ->
        tempFileExistedAtSaveList += fileManager.fileExistsAtPath(checkNotNull(fileUrl.path))
        isSaved
    }

    private fun isInTempDirectory(fileName: String): Boolean =
        fileManager.fileExistsAtPath(NSTemporaryDirectory() + fileName)

    private companion object {
        const val SOURCE_FILE_NAME = "temp_file_test_source.jpg"
        const val SOURCE_FILE_CONTENT = "photo"
        const val FIRST_SAVED_FILE_NAME = "temp_file_test_first.jpg"
        const val SECOND_SAVED_FILE_NAME = "temp_file_test_second.jpg"
        const val JPEG_MIME_TYPE = "image/jpeg"
    }
}
