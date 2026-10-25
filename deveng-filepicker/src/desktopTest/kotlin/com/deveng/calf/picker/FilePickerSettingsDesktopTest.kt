package com.deveng.calf.picker

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FilePickerSettingsDesktopTest {

    @Test
    fun createSettings_withoutParentWindow_hasNoParentWindow() {
        val settings = FilePickerSettings(title = TITLE, initialDirectory = DIRECTORY, parentWindow = null)

        assertNull(settings.parentWindow)
        assertEquals(TITLE, settings.title)
        assertEquals(DIRECTORY, settings.initialDirectory)
        assertEquals(ImageRepresentationMode.Compatible, settings.imageRepresentationMode)
    }

    @Test
    fun createSettings_withImageMode_keepsTheMode() {
        val settings = FilePickerSettings(imageRepresentationMode = ImageRepresentationMode.Current, parentWindow = null)

        assertEquals(ImageRepresentationMode.Current, settings.imageRepresentationMode)
    }

    @Test
    fun readParentWindow_ofCommonSettings_isNull() {
        assertNull(FilePickerSettingsImpl(title = TITLE).parentWindow)
    }

    private companion object {
        const val TITLE = "Pick"
        const val DIRECTORY = "/tmp"
    }
}
