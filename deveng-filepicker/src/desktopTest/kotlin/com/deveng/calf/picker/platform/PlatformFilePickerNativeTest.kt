package com.deveng.calf.picker.platform

import com.deveng.calf.picker.FilePickerFileType
import com.deveng.calf.picker.FilePickerSelectionMode
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking

class PlatformFilePickerNativeTest {

    private val fake = FakeNativeDialogs()
    private lateinit var original: NativeDialogs
    private lateinit var dir: File

    @BeforeTest
    fun setUp() {
        original = PlatformFilePicker.native
        PlatformFilePicker.native = fake
        dir = createTempDirectory("filepicker-test").toFile()
    }

    @AfterTest
    fun tearDown() {
        PlatformFilePicker.native = original
        dir.deleteRecursively()
    }

    @Test
    fun warmup_initialisesTheNativeLibrary() {
        PlatformFilePicker.warmup()

        assertEquals(listOf("init"), fake.calls)
    }

    @Test
    fun createFilePickerHandle_passesConfigurationAndReturnsHandle() {
        val handle = PlatformFilePicker.createFilePickerHandle(
            initialDirectory = "/home",
            type = FilePickerFileType.Pdf,
            selectionMode = FilePickerSelectionMode.Single,
            title = "Pick",
        )

        assertEquals(7L, handle)
        assertEquals("Pick", fake.lastTitle)
        assertEquals("/home", fake.lastInitialDirectory)
        assertContentEquals(arrayOf("pdf"), fake.lastExtensions)
        assertEquals(false, fake.lastMultiple)
        assertEquals(0L, fake.lastParentWindow)
    }

    @Test
    fun createFilePickerHandle_withMultipleSelection_allowsMultiple() {
        PlatformFilePicker.createFilePickerHandle(
            initialDirectory = null,
            type = FilePickerFileType.All,
            selectionMode = FilePickerSelectionMode.Multiple(3),
            title = null,
        )

        assertEquals(true, fake.lastMultiple)
        assertNull(fake.lastExtensions)
    }

    @Test
    fun createDirectoryPickerHandle_passesConfigurationAndReturnsHandle() {
        val handle = PlatformFilePicker.createDirectoryPickerHandle(initialDirectory = "/tmp", title = "Folder")

        assertEquals(7L, handle)
        assertEquals("Folder", fake.lastTitle)
        assertEquals("/tmp", fake.lastInitialDirectory)
        assertEquals(0L, fake.lastParentWindow)
    }

    @Test
    fun createSaveDialogHandle_joinsBaseNameAndExtension() {
        val handle = PlatformFilePicker.createSaveDialogHandle(
            initialDirectory = "/docs",
            baseName = "report",
            extension = "pdf",
            title = "Save",
        )

        assertEquals(7L, handle)
        assertEquals("report.pdf", fake.lastDefaultName)
        assertEquals("pdf", fake.lastSaveExtension)
        assertEquals("Save", fake.lastTitle)
        assertEquals("/docs", fake.lastInitialDirectory)
        assertEquals(0L, fake.lastParentWindow)
    }

    @Test
    fun showFilePicker_mapsPathsToFiles() = runBlocking {
        fake.filePaths = arrayOf("/a/one.txt", "/a/two.txt")

        val files = PlatformFilePicker.showFilePicker(9L)

        assertEquals(listOf(File("/a/one.txt"), File("/a/two.txt")), files)
        assertEquals(listOf("showFile:9"), fake.calls)
    }

    @Test
    fun showFilePicker_whenCancelled_returnsEmptyList() = runBlocking {
        assertTrue(PlatformFilePicker.showFilePicker(9L).isEmpty())
    }

    @Test
    fun showDirectoryPicker_returnsSelectedDirectory() = runBlocking {
        fake.directoryPath = "/a/dir"

        assertEquals(File("/a/dir"), PlatformFilePicker.showDirectoryPicker(9L))
        assertEquals(listOf("showDirectory:9"), fake.calls)
    }

    @Test
    fun showDirectoryPicker_whenCancelled_returnsNull() = runBlocking {
        assertNull(PlatformFilePicker.showDirectoryPicker(9L))
    }

    @Test
    fun showSaveDialog_withBytes_writesThemToSelectedFile() = runBlocking {
        val target = File(dir, "out.bin")
        fake.savePath = target.path

        val saved = PlatformFilePicker.showSaveDialog(9L, byteArrayOf(1, 2, 3))

        assertEquals(target, saved)
        assertContentEquals(byteArrayOf(1, 2, 3), target.readBytes())
    }

    @Test
    fun showSaveDialog_withoutBytes_createsAnEmptyFile() = runBlocking {
        val target = File(dir, "empty.bin")
        fake.savePath = target.path

        val saved = PlatformFilePicker.showSaveDialog(9L, null)

        assertEquals(target, saved)
        assertTrue(target.exists())
        assertEquals(0L, target.length())
    }

    @Test
    fun showSaveDialog_whenCancelled_returnsNullAndWritesNothing() = runBlocking {
        assertNull(PlatformFilePicker.showSaveDialog(9L, byteArrayOf(1)))
        assertTrue(dir.listFiles().orEmpty().isEmpty())
    }

    @Test
    fun showSaveDialogWithFile_copiesSourceOverExistingFile() = runBlocking {
        val source = File(dir, "source.txt").apply { writeText("new") }
        val target = File(dir, "target.txt").apply { writeText("old content") }
        fake.savePath = target.path

        val saved = PlatformFilePicker.showSaveDialogWithFile(9L, source)

        assertEquals(target, saved)
        assertEquals("new", target.readText())
        assertTrue(source.exists())
    }

    @Test
    fun showSaveDialogWithFile_whenCancelled_returnsNull() = runBlocking {
        val source = File(dir, "source.txt").apply { writeText("x") }

        assertNull(PlatformFilePicker.showSaveDialogWithFile(9L, source))
        assertFalse(File(dir, "target.txt").exists())
    }

    @Test
    fun destroyDialog_releasesTheHandle() {
        PlatformFilePicker.destroyDialog(9L)

        assertEquals(listOf("destroy:9"), fake.calls)
    }
}
