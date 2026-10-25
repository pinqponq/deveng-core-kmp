package com.deveng.calf.picker

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
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

    @Test
    fun compareSettings_withSameValues_areEqualWithSameHashCode() {
        val first = DesktopFilePickerSettings(title = TITLE, initialDirectory = DIRECTORY)
        val second = DesktopFilePickerSettings(title = TITLE, initialDirectory = DIRECTORY)

        assertEquals(first, second)
        assertEquals(first.hashCode(), second.hashCode())
    }

    @Test
    fun compareSettings_withOneDifferentValue_areNotEqual() {
        val base = DesktopFilePickerSettings(title = TITLE, initialDirectory = DIRECTORY)

        assertNotEquals(base, base.copy(title = "Other"))
        assertNotEquals(base, base.copy(initialDirectory = "/other"))
        assertNotEquals(base, base.copy(imageRepresentationMode = ImageRepresentationMode.Current))
        assertNotEquals(base.hashCode(), base.copy(title = "Other").hashCode())
        assertNotEquals(base.hashCode(), base.copy(initialDirectory = "/other").hashCode())
        assertNotEquals(base.hashCode(), base.copy(imageRepresentationMode = ImageRepresentationMode.Current).hashCode())
    }

    @Test
    fun printSettings_listsEveryValue() {
        val text = DesktopFilePickerSettings(title = TITLE, initialDirectory = DIRECTORY).toString()

        assertTrue(TITLE in text && DIRECTORY in text && "Compatible" in text)
    }

    @Test
    fun readParentWindow_ofLocalDefault_isNull() = runComposeUiTest {
        var window: Any? = "unset"
        setContent { window = LocalFilePickerParentWindow.current }

        assertNull(window)
    }

    @Test
    fun rememberSettings_withDesktopArguments_keepsEveryValue() = runComposeUiTest {
        var settings: FilePickerSettings? = null
        setContent {
            settings = rememberFilePickerSettings(
                title = TITLE,
                initialDirectory = DIRECTORY,
                imageRepresentationMode = ImageRepresentationMode.Current,
                parentWindow = null,
            )
        }

        assertEquals(TITLE, settings!!.title)
        assertEquals(DIRECTORY, settings!!.initialDirectory)
        assertEquals(ImageRepresentationMode.Current, settings!!.imageRepresentationMode)
        assertNull(settings!!.parentWindow)
    }

    @Test
    fun rememberSettings_withCommonArguments_keepsEveryValue() = runComposeUiTest {
        var settings: FilePickerSettings? = null
        setContent {
            settings = rememberFilePickerSettings(
                title = TITLE,
                initialDirectory = DIRECTORY,
                imageRepresentationMode = ImageRepresentationMode.Current,
                maxFileSizeBytes = 5L,
            )
        }

        assertEquals(TITLE, settings!!.title)
        assertEquals(DIRECTORY, settings!!.initialDirectory)
        assertEquals(ImageRepresentationMode.Current, settings!!.imageRepresentationMode)
        assertEquals(5L, settings!!.maxFileSizeBytes)
    }

    @Test
    fun readDefaults_ofMinimalSettings_areEmpty() {
        val settings = object : FilePickerSettings {
            override val title: String? = null
            override val initialDirectory: String? = null
        }

        assertNull(settings.title)
        assertNull(settings.initialDirectory)
        assertNull(settings.maxFileSizeBytes)
        assertEquals(ImageRepresentationMode.Compatible, settings.imageRepresentationMode)
    }

    private companion object {
        const val TITLE = "Pick"
        const val DIRECTORY = "/tmp"
    }
}
