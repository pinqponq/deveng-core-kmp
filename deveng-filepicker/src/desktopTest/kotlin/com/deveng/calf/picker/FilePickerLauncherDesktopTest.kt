package com.deveng.calf.picker

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import com.deveng.calf.io.KmpFile
import com.deveng.calf.picker.platform.FakeNativeDialogs
import com.deveng.calf.picker.platform.NativeDialogs
import com.deveng.calf.picker.platform.PlatformFilePicker
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class, com.deveng.calf.core.ExperimentalCalfApi::class)
class FilePickerLauncherDesktopTest {

    private val fake = FakeNativeDialogs()
    private lateinit var original: NativeDialogs
    private lateinit var dir: File

    @BeforeTest
    fun setUp() {
        original = PlatformFilePicker.native
        PlatformFilePicker.native = fake
        dir = createTempDirectory("launcher-test").toFile()
    }

    @AfterTest
    fun tearDown() {
        PlatformFilePicker.native = original
        dir.deleteRecursively()
    }

    @Test
    fun launch_withFileType_returnsSelectedFiles() = runComposeUiTest {
        fake.filePaths = arrayOf("/a/one.txt", "/a/two.txt")
        var result: List<KmpFile>? = null
        var launcher: FilePickerLauncher? = null
        setContent {
            launcher = rememberFilePickerLauncher(
                type = FilePickerFileType.Pdf,
                selectionMode = FilePickerSelectionMode.Multiple,
                settings = FilePickerSettings(title = "Pick", initialDirectory = "/home", parentWindow = null),
                onPickerDismissed = {},
                onResult = { result = it },
            )
        }
        waitUntil { fake.calls.contains("createFile") }

        launcher!!.launch()
        waitUntil { result != null }

        assertEquals(listOf("/a/one.txt", "/a/two.txt"), result!!.map { (it.file).path })
        assertEquals("Pick", fake.lastTitle)
        assertEquals("/home", fake.lastInitialDirectory)
        assertEquals(true, fake.lastMultiple)
        assertContentEquals(arrayOf("pdf"), fake.lastExtensions)
    }

    @Test
    fun launch_withMaxItems_limitsTheResult() = runComposeUiTest {
        fake.filePaths = arrayOf("/a/1", "/a/2", "/a/3")
        var result: List<KmpFile>? = null
        var launcher: FilePickerLauncher? = null
        setContent {
            launcher = rememberFilePickerLauncher(
                type = FilePickerFileType.All,
                selectionMode = FilePickerSelectionMode.Multiple(2),
                onResult = { result = it },
            )
        }
        waitUntil { fake.calls.contains("createFile") }

        launcher!!.launch()
        waitUntil { result != null }

        assertEquals(2, result!!.size)
    }

    @Test
    fun launch_withoutTitle_usesDefaultFileTitle() = runComposeUiTest {
        setContent {
            rememberFilePickerLauncher(type = FilePickerFileType.All, onResult = {})
        }
        waitUntil { fake.calls.contains("createFile") }

        assertEquals("Select a file", fake.lastTitle)
    }

    @Test
    fun launch_withFolderType_returnsSelectedDirectory() = runComposeUiTest {
        fake.directoryPath = "/a/dir"
        var result: List<KmpFile>? = null
        var launcher: FilePickerLauncher? = null
        setContent {
            launcher = rememberFilePickerLauncher(
                type = FilePickerFileType.Folder,
                onResult = { result = it },
            )
        }
        waitUntil { fake.calls.contains("createDirectory") }

        launcher!!.launch()
        waitUntil { result != null }

        assertEquals("Select a folder", fake.lastTitle)
        assertEquals(listOf("/a/dir"), result!!.map { it.file.path })
    }

    @Test
    fun launch_withFolderTypeAndCancel_returnsEmptyList() = runComposeUiTest {
        var result: List<KmpFile>? = null
        var launcher: FilePickerLauncher? = null
        setContent {
            launcher = rememberFilePickerLauncher(
                type = FilePickerFileType.Folder,
                onResult = { result = it },
            )
        }
        waitUntil { fake.calls.contains("createDirectory") }

        launcher!!.launch()
        waitUntil { result != null }

        assertTrue(result!!.isEmpty())
    }

    @Test
    fun launch_beforeHandleExists_doesNothing() = runComposeUiTest {
        var result: List<KmpFile>? = null
        var launcher: FilePickerLauncher? = null
        fake.nextHandle = 0L
        setContent {
            launcher = rememberFilePickerLauncher(type = FilePickerFileType.All, onResult = { result = it })
        }
        waitUntil { fake.calls.contains("createFile") }

        launcher!!.launch()
        waitForIdle()

        assertNull(result)
        assertTrue(fake.calls.none { it.startsWith("showFile") })
    }

    @Test
    fun leavingComposition_destroysTheHandle() = runComposeUiTest {
        var shown = true
        val state = androidx.compose.runtime.mutableStateOf(true)
        setContent {
            if (state.value) {
                rememberFilePickerLauncher(type = FilePickerFileType.All, onResult = {})
            }
        }
        waitUntil { fake.calls.contains("createFile") }

        state.value = false
        waitForIdle()
        shown = false

        assertTrue(fake.calls.contains("destroy:7"), "calls=${fake.calls} shown=$shown")
    }

    @Test
    fun saver_withBytes_writesFileAndReportsIt() = runComposeUiTest {
        val target = File(dir, "saved.bin")
        fake.savePath = target.path
        var result: KmpFile? = null
        var launcher: FileSaverLauncher? = null
        setContent {
            launcher = rememberFileSaverLauncher(
                settings = FilePickerSettings(title = "Save", initialDirectory = "/docs", parentWindow = null),
                onResult = { result = it },
            )
        }

        launcher!!.launch(bytes = byteArrayOf(4, 5), baseName = "saved", extension = "bin")
        waitUntil { result != null }

        assertContentEquals(byteArrayOf(4, 5), target.readBytes())
        assertEquals("saved.bin", fake.lastDefaultName)
        assertEquals("/docs", fake.lastInitialDirectory)
        assertTrue(fake.calls.contains("destroy:7"))
    }

    @Test
    fun saver_withExplicitDirectory_overridesSettingsDirectory() = runComposeUiTest {
        var launcher: FileSaverLauncher? = null
        setContent {
            launcher = rememberFileSaverLauncher(
                settings = FilePickerSettings(initialDirectory = "/docs", parentWindow = null),
                onResult = {},
            )
        }

        launcher!!.launch(bytes = null, baseName = "x", extension = "txt", initialDirectory = "/other")
        waitUntil { fake.calls.contains("destroy:7") }

        assertEquals("/other", fake.lastInitialDirectory)
    }

    @Test
    fun saver_whenCancelled_reportsNull() = runComposeUiTest {
        var called = false
        var result: KmpFile? = KmpFile(File("sentinel"))
        var launcher: FileSaverLauncher? = null
        setContent {
            launcher = rememberFileSaverLauncher(onResult = { called = true; result = it })
        }

        launcher!!.launch(bytes = byteArrayOf(1), baseName = "x", extension = "txt")
        waitUntil { called }

        assertNull(result)
        assertTrue(fake.calls.contains("destroy:7"))
    }

    @Test
    fun saver_withFile_copiesSourceToSelectedPath() = runComposeUiTest {
        val source = File(dir, "source.txt").apply { writeText("payload") }
        val target = File(dir, "copy.txt")
        fake.savePath = target.path
        var result: KmpFile? = null
        var launcher: FileSaverLauncher? = null
        setContent {
            launcher = rememberFileSaverLauncher(onResult = { result = it })
        }

        launcher!!.launch(source, baseName = "copy", extension = "txt")
        waitUntil { result != null }

        assertEquals("payload", target.readText())
        assertEquals(target, result!!.file)
    }

    @Test
    fun saver_withFileAndSettings_usesSettingsTitleAndDirectory() = runComposeUiTest {
        val source = File(dir, "source.txt").apply { writeText("payload") }
        fake.savePath = File(dir, "copy.txt").path
        var result: KmpFile? = null
        var launcher: FileSaverLauncher? = null
        setContent {
            launcher = rememberFileSaverLauncher(
                settings = FilePickerSettings(title = "Save copy", initialDirectory = "/docs", parentWindow = null),
                onResult = { result = it },
            )
        }

        launcher!!.launch(source, baseName = "copy", extension = "txt")
        waitUntil { result != null }

        assertEquals("Save copy", fake.lastTitle)
        assertEquals("/docs", fake.lastInitialDirectory)
        assertEquals("copy.txt", fake.lastDefaultName)
        assertTrue(fake.calls.contains("destroy:7"))
    }

    @Test
    fun saver_withFileAndExplicitDirectory_overridesSettingsDirectory() = runComposeUiTest {
        val source = File(dir, "source.txt").apply { writeText("payload") }
        var called = false
        var launcher: FileSaverLauncher? = null
        setContent {
            launcher = rememberFileSaverLauncher(
                settings = FilePickerSettings(initialDirectory = "/docs", parentWindow = null),
                onResult = { called = true },
            )
        }

        launcher!!.launch(source, baseName = "copy", extension = "txt", initialDirectory = "/other")
        waitUntil { called }

        assertEquals("/other", fake.lastInitialDirectory)
    }

    @Test
    fun saver_withFileWhenCancelled_reportsNullAndDestroysTheHandle() = runComposeUiTest {
        val source = File(dir, "source.txt").apply { writeText("payload") }
        var called = false
        var result: KmpFile? = KmpFile(File("sentinel"))
        var launcher: FileSaverLauncher? = null
        setContent {
            launcher = rememberFileSaverLauncher(onResult = { called = true; result = it })
        }

        launcher!!.launch(source, baseName = "copy", extension = "txt")
        waitUntil { called }

        assertNull(result)
        assertTrue(fake.calls.contains("destroy:7"))
    }

    @Test
    fun saver_withBytes_usesSettingsTitle() = runComposeUiTest {
        var called = false
        var launcher: FileSaverLauncher? = null
        setContent {
            launcher = rememberFileSaverLauncher(
                settings = FilePickerSettings(title = "Save bytes", parentWindow = null),
                onResult = { called = true },
            )
        }

        launcher!!.launch(bytes = byteArrayOf(1), baseName = "x", extension = "txt")
        waitUntil { called }

        assertEquals("Save bytes", fake.lastTitle)
    }
}
