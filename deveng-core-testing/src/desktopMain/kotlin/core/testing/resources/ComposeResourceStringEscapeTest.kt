package core.testing.resources

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.fail

abstract class ComposeResourceStringEscapeTest(
    private val composeResourcesPath: String = DEFAULT_COMPOSE_RESOURCES_PATH
) {

    private val composeResourceStringEscapeScanner = ComposeResourceStringEscapeScanner()

    @Test
    fun scanComposeResourceStrings_withCommittedFiles_findsNoLiteralEscape() {
        val stringFiles = composeResourceStringEscapeScanner.findStringFiles(File(composeResourcesPath))
        assertTrue(stringFiles.isNotEmpty(), "No strings.xml found under $composeResourcesPath")

        val literalEscapeFindings = stringFiles.flatMap(composeResourceStringEscapeScanner::findLiteralEscapes)

        if (literalEscapeFindings.isNotEmpty()) {
            fail(
                "Compose resources show these escapes literally; write ' and \" without a backslash:\n" +
                    literalEscapeFindings.joinToString(separator = "\n")
            )
        }
    }

    private companion object {
        const val DEFAULT_COMPOSE_RESOURCES_PATH = "src/commonMain/composeResources"
    }
}
