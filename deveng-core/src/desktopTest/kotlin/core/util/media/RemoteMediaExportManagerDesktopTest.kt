package core.util.media

import kotlin.test.Test
import kotlin.test.assertIs
import kotlinx.coroutines.test.runTest

class RemoteMediaExportManagerDesktopTest {

    @Test
    fun saveMediaToGallery_onDesktop_returnsUnsupportedFailure() = runTest {
        val result = RemoteMediaExportManager().saveMediaToGallery(
            RemoteMediaSaveRequest(fileUrl = FILE_URL, fileName = FILE_NAME, mimeType = JPEG_MIME_TYPE),
        )

        val failure = assertIs<RemoteMediaSaveResult.Failed>(result)
        assertIs<UnsupportedOperationException>(failure.exception)
    }

    private companion object {
        const val FILE_URL = "https://media.example.com/photo.jpg"
        const val FILE_NAME = "photo.jpg"
        const val JPEG_MIME_TYPE = "image/jpeg"
    }
}
