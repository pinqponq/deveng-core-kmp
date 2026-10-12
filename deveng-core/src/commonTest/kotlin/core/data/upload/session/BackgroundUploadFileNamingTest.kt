package core.data.upload.session

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BackgroundUploadFileNamingTest {

    private val fileNaming = BackgroundUploadFileNaming(fileNamePrefix = PREFIX)

    @Test
    fun fileNameOf_wrapsUploadIdInPrefixAndSuffix() {
        assertEquals("bg_widget_$UPLOAD_ID.multipart", fileNaming.fileNameOf(UPLOAD_ID))
    }

    @Test
    fun uploadIdOf_readsUploadIdBackFromFileName() {
        assertEquals(UPLOAD_ID, fileNaming.uploadIdOf(fileNaming.fileNameOf(UPLOAD_ID)))
    }

    @Test
    fun uploadIdOf_ignoresFilesOfOtherOwners() {
        assertNull(fileNaming.uploadIdOf("bg_upload_$UPLOAD_ID.multipart"))
        assertNull(fileNaming.uploadIdOf("bg_widget_$UPLOAD_ID.jpg"))
        assertNull(fileNaming.uploadIdOf("bg_widget_.multipart"))
    }

    private companion object {
        const val PREFIX = "bg_widget_"
        const val UPLOAD_ID = "upload-1"
    }
}
