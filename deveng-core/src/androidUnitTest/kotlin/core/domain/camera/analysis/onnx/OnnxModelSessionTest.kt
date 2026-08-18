package core.domain.camera.analysis.onnx

import java.io.File
import java.nio.file.Files
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class OnnxModelSessionTest {

    @Test
    fun missingModelIsRejectedBeforeSessionInitialization() {
        val missingFile = File("missing-${System.nanoTime()}.onnx")

        val error = assertFailsWith<OnnxModelException> {
            validateModelFile(missingFile)
        }

        assertEquals(OnnxModelErrorCode.MODEL_NOT_FOUND, error.code)
    }

    @Test
    fun directoryIsRejectedAsModelFile() {
        withTemporaryDirectory { directory ->
            val error = assertFailsWith<OnnxModelException> {
                validateModelFile(directory)
            }

            assertEquals(OnnxModelErrorCode.MODEL_NOT_READABLE, error.code)
        }
    }

    @Test
    fun emptyModelFileIsRejectedBeforeNativeInitialization() {
        withTemporaryDirectory { directory ->
            val emptyModel = File(directory, "empty.onnx").apply { createNewFile() }

            val error = assertFailsWith<OnnxModelException> {
                validateModelFile(emptyModel)
            }

            assertEquals(OnnxModelErrorCode.MODEL_INITIALIZATION_FAILED, error.code)
        }
    }

    @Test
    fun validModelPathIsNormalizedToAnAbsoluteFile() {
        withTemporaryDirectory { directory ->
            val modelFile = File(directory, "model.onnx").apply { writeText("not-empty") }

            val validatedFile = validateModelFile(modelFile)

            assertTrue(validatedFile.isAbsolute)
            assertEquals(modelFile.absoluteFile, validatedFile)
        }
    }

    @Test
    fun invalidOnnxContentIsReportedAsInitializationFailure() {
        withTemporaryDirectory { directory ->
            val invalidModel = File(directory, "invalid.onnx").apply { writeText("not-an-onnx-model") }

            val error = assertFailsWith<OnnxModelException> {
                OnnxModelSession.open(invalidModel)
            }

            assertEquals(OnnxModelErrorCode.MODEL_INITIALIZATION_FAILED, error.code)
        }
    }

    @Test
    fun closeGuardReleasesNativeResourceOnlyOnce() {
        val closeCount = AtomicInteger(0)
        val closeGuard = CloseOnce { closeCount.incrementAndGet() }

        closeGuard.close()
        closeGuard.close()

        assertTrue(closeGuard.isClosed)
        assertEquals(1, closeCount.get())
    }

    private fun withTemporaryDirectory(block: (File) -> Unit) {
        val directory = Files.createTempDirectory("onnx-model-session-test").toFile()
        try {
            block(directory)
        } finally {
            directory.deleteRecursively()
        }
    }
}
