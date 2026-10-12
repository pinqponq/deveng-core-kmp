package core.testing.resources

import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ComposeResourceStringEscapeTestTest {

    @Test
    fun scanComposeResourceStrings_withResolvedEscapesOnly_passes() {
        val composeResourcesDirectory = createComposeResourcesDirectory(stringValue = "l'abonnement\\n")

        createStringEscapeTest(composeResourcesDirectory).scanComposeResourceStrings_withCommittedFiles_findsNoLiteralEscape()
    }

    @Test
    fun scanComposeResourceStrings_withLiteralEscape_failsNamingKeyAndEscape() {
        val composeResourcesDirectory = createComposeResourcesDirectory(stringValue = "l\\'abonnement")

        val failure = assertFailsWith<AssertionError> {
            createStringEscapeTest(composeResourcesDirectory).scanComposeResourceStrings_withCommittedFiles_findsNoLiteralEscape()
        }

        assertTrue("$STRING_KEY contains \\'" in failure.message.orEmpty())
    }

    @Test
    fun scanComposeResourceStrings_withoutStringFiles_failsNamingFolder() {
        val emptyDirectory = createTempDirectory().toFile().apply { deleteOnExit() }

        val failure = assertFailsWith<AssertionError> {
            createStringEscapeTest(emptyDirectory).scanComposeResourceStrings_withCommittedFiles_findsNoLiteralEscape()
        }

        assertTrue(emptyDirectory.path in failure.message.orEmpty())
    }

    // An anonymous subclass keeps the inherited @Test out of this class's own test run.
    private fun createStringEscapeTest(composeResourcesDirectory: File): ComposeResourceStringEscapeTest =
        object : ComposeResourceStringEscapeTest(composeResourcesPath = composeResourcesDirectory.path) {}

    private fun createComposeResourcesDirectory(stringValue: String): File {
        val composeResourcesDirectory = createTempDirectory().toFile().apply { deleteOnExit() }
        composeResourcesDirectory.resolve("values").mkdirs()
        composeResourcesDirectory.resolve("values/strings.xml").apply {
            writeText("""<resources><string name="$STRING_KEY">$stringValue</string></resources>""")
            deleteOnExit()
        }
        return composeResourcesDirectory
    }

    private companion object {
        const val STRING_KEY = "profile_feat_partner_body"
    }
}
