package com.deveng.calf.picker.platform

import com.deveng.calf.picker.FilePickerFileType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PlatformFilePickerTest {

    @Test
    fun resolveExtensions_ofAll_hasNoFilter() {
        assertNull(resolveExtensions(FilePickerFileType.All))
    }

    @Test
    fun resolveExtensions_ofFolder_hasNoFilter() {
        assertNull(resolveExtensions(FilePickerFileType.Folder))
    }

    @Test
    fun resolveExtensions_ofExtensionType_returnsTheGivenExtensions() {
        assertEquals(listOf("pdf", "txt"), resolveExtensions(FilePickerFileType.Extension(listOf("pdf", "txt"))))
    }

    @Test
    fun resolveExtensions_ofPdf_returnsPdf() {
        assertEquals(listOf("pdf"), resolveExtensions(FilePickerFileType.Pdf))
    }

    @Test
    fun resolveExtensions_ofImage_expandsWildcardToImageExtensions() {
        val extensions = resolveExtensions(FilePickerFileType.Image).orEmpty()

        assertTrue("png" in extensions)
        assertTrue("jpg" in extensions)
    }

    @Test
    fun resolveExtensions_ofImageVideo_hasNoDuplicates() {
        val extensions = resolveExtensions(FilePickerFileType.ImageVideo).orEmpty()

        assertEquals(extensions.distinct(), extensions)
        assertTrue("png" in extensions && "mp4" in extensions)
    }

    @Test
    fun resolveExtensions_ofUnknownMime_returnsNull() {
        assertNull(resolveExtensions(FilePickerFileType.Custom(listOf("application/x-unknown-thing"))))
    }
}
