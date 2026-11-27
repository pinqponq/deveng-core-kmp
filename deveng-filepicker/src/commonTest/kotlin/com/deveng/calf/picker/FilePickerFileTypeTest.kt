package com.deveng.calf.picker

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FilePickerFileTypeTest {

    @Test
    fun readValue_ofImageVideo_listsImageThenVideo() {
        assertEquals(listOf("image/*", "video/*"), FilePickerFileType.ImageVideo.value.toList())
    }

    @Test
    fun readValue_ofSingleKinds_listsOneContentType() {
        assertEquals(listOf("image/*"), FilePickerFileType.Image.value.toList())
        assertEquals(listOf("video/*"), FilePickerFileType.Video.value.toList())
        assertEquals(listOf("application/pdf"), FilePickerFileType.Pdf.value.toList())
        assertEquals(listOf("*/*"), FilePickerFileType.All.value.toList())
    }

    @Test
    fun readValue_ofCustomType_listsTheGivenContentTypes() {
        val type = FilePickerFileType.Custom(listOf("audio/mpeg", "audio/wav"))

        assertEquals(listOf("audio/mpeg", "audio/wav"), type.value.toList())
    }

    @Test
    fun readValue_ofExtensionType_listsTheGivenExtensions() {
        val type = FilePickerFileType.Extension(listOf("png", "jpg"))

        assertEquals(listOf("png", "jpg"), type.value.toList())
    }

    @Test
    fun readMaxItems_ofMultipleWithoutArgument_isUnlimited() {
        assertNull(FilePickerSelectionMode.Multiple.maxItems)
    }

    @Test
    fun readMaxItems_ofMultipleWithLimit_keepsTheLimit() {
        assertEquals(MAX_ITEMS, FilePickerSelectionMode.Multiple(maxItems = MAX_ITEMS).maxItems)
    }

    private companion object {
        const val MAX_ITEMS = 5
    }
}
