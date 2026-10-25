package com.deveng.calf.io

import com.deveng.calf.core.PlatformContext
import kotlinx.coroutines.test.runTest
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class KmpFileDesktopTest {

    private val context = PlatformContext.INSTANCE

    @Test
    fun convertFile_toKmpFile_wrapsTheSameFile() {
        val file = File("any.txt")

        assertSame(file, file.toKmpFile().file)
    }

    @Test
    fun readMetadata_ofExistingFile_returnsNamePathAndSize() {
        val file = createTempFile(CONTENT)
        val kmpFile = file.toKmpFile()

        assertTrue(kmpFile.exists(context))
        assertEquals(file.name, kmpFile.getName(context))
        assertEquals(file.path, kmpFile.getPath(context))
        assertEquals(CONTENT.size.toLong(), kmpFile.getSize(context))
        assertFalse(kmpFile.isDirectory(context))
        assertTrue(kmpFile.isFile(context))
    }

    @Test
    fun checkExists_ofMissingFile_isFalse() {
        val missing = File(Files.createTempDirectory("kmpfile").toFile(), "missing.bin")

        assertFalse(missing.toKmpFile().exists(context))
    }

    @Test
    fun checkDirectory_ofFolder_isDirectoryAndNotFile() {
        val kmpFile = Files.createTempDirectory("kmpfile").toFile().toKmpFile()

        assertTrue(kmpFile.isDirectory(context))
        assertFalse(kmpFile.isFile(context))
    }

    @Test
    fun readByteArray_ofFile_returnsContent() = runTest {
        val kmpFile = createTempFile(CONTENT).toKmpFile()

        assertContentEquals(CONTENT, kmpFile.readByteArray(context))
    }

    @Test
    fun useContextFreeOverloads_ofFile_matchContextOverloads() = runTest {
        val file = createTempFile(CONTENT)
        val kmpFile = file.toKmpFile()

        assertTrue(kmpFile.exists())
        assertEquals(file.name, kmpFile.getName())
        assertEquals(file.path, kmpFile.getPath())
        assertEquals(CONTENT.size.toLong(), kmpFile.getSize())
        assertFalse(kmpFile.isDirectory())
        assertTrue(kmpFile.isFile())
        assertContentEquals(CONTENT, kmpFile.readByteArray())
    }

    private fun createTempFile(content: ByteArray): File =
        Files.createTempFile("kmpfile", ".bin").toFile().also {
            it.deleteOnExit()
            it.writeBytes(content)
        }

    private companion object {
        val CONTENT = byteArrayOf(1, 2, 3, 4, 5)
    }
}
