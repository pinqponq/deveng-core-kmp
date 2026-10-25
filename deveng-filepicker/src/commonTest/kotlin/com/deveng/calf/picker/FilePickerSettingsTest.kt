package com.deveng.calf.picker

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FilePickerSettingsTest {

    @Test
    fun createDefaultSettings_withoutArguments_hasNoSizeLimitAndCompatibleImages() {
        val settings = defaultFilePickerSettings()

        assertNull(settings.title)
        assertNull(settings.initialDirectory)
        assertNull(settings.maxFileSizeBytes)
        assertEquals(ImageRepresentationMode.Compatible, settings.imageRepresentationMode)
    }

    @Test
    fun createSettings_withMaxFileSize_keepsTheLimit() {
        val settings = FilePickerSettings(maxFileSizeBytes = MAX_FILE_SIZE_BYTES)

        assertEquals(MAX_FILE_SIZE_BYTES, settings.maxFileSizeBytes)
    }

    @Test
    fun createSettings_withAllArguments_keepsEveryValue() {
        val settings = FilePickerSettings(
            title = TITLE,
            initialDirectory = INITIAL_DIRECTORY,
            imageRepresentationMode = ImageRepresentationMode.Current,
            maxFileSizeBytes = MAX_FILE_SIZE_BYTES,
        )

        assertEquals(TITLE, settings.title)
        assertEquals(INITIAL_DIRECTORY, settings.initialDirectory)
        assertEquals(ImageRepresentationMode.Current, settings.imageRepresentationMode)
        assertEquals(MAX_FILE_SIZE_BYTES, settings.maxFileSizeBytes)
    }

    @Test
    fun compareSettings_withSameValues_areEqual() {
        assertEquals(
            FilePickerSettings(title = TITLE, maxFileSizeBytes = MAX_FILE_SIZE_BYTES),
            FilePickerSettings(title = TITLE, maxFileSizeBytes = MAX_FILE_SIZE_BYTES),
        )
    }

    private companion object {
        const val TITLE = "Pick a photo"
        const val INITIAL_DIRECTORY = "/pictures"
        const val MAX_FILE_SIZE_BYTES = 260L * 1024 * 1024
    }
}
