package core.domain.camera.analysis.onnx

import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertFailsWith

class OnnxModelSessionTest {

    @Test
    fun missingModelFailsSessionInitialization() {
        val missingFile = File("missing-${System.nanoTime()}.onnx")

        assertFailsWith<Throwable> {
            OnnxModelSession.open(missingFile)
        }
    }

    @Test
    fun invalidOnnxContentFailsSessionInitialization() {
        withTemporaryDirectory { directory ->
            val invalidModel = File(directory, "invalid.onnx").apply { writeText("not-an-onnx-model") }

            assertFailsWith<Throwable> {
                OnnxModelSession.open(invalidModel)
            }
        }
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
