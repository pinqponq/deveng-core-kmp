package core.testing.resources

import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals

class ComposeResourceStringEscapeScannerTest {

    private val composeResourceStringEscapeScanner = ComposeResourceStringEscapeScanner()

    @Test
    fun findLiteralEscapes_withEscapedApostropheAndQuote_reportsFileKeyAndEscape() {
        val stringFile = createStringFile(
            """
            <resources>
                <string name="premium_feat_partner_body">grâce à l\'abonnement</string>
                <plurals name="gallery_feat_selected_count">
                    <item quantity="one">%1${'$'}d \"photo</item>
                </plurals>
            </resources>
            """.trimIndent()
        )

        val literalEscapeFindings = composeResourceStringEscapeScanner.findLiteralEscapes(stringFile)

        assertEquals(
            listOf(
                "${stringFile.path}: premium_feat_partner_body contains \\'",
                "${stringFile.path}: gallery_feat_selected_count[one] contains \\\"",
            ),
            literalEscapeFindings
        )
    }

    @Test
    fun findLiteralEscapes_withEscapesComposeResolves_reportsNothing() {
        val stringFile = createStringFile(
            """
            <resources>
                <string name="profile_feat_supported_escapes">a\nb\tc\\d \u00E9 l'abonnement "photo"</string>
                <string name="profile_feat_escaped_backslash_before_apostrophe">C:\\'s</string>
            </resources>
            """.trimIndent()
        )

        val literalEscapeFindings = composeResourceStringEscapeScanner.findLiteralEscapes(stringFile)

        assertEquals(emptyList(), literalEscapeFindings)
    }

    @Test
    fun findStringFiles_withNestedLocaleFolders_returnsOnlyStringFilesSortedByPath() {
        val composeResourcesDirectory = createTempDirectory().toFile().apply { deleteOnExit() }
        val frenchStringFile = composeResourcesDirectory.resolve("values-fr/strings.xml").createWithParents()
        val defaultStringFile = composeResourcesDirectory.resolve("values/strings.xml").createWithParents()
        composeResourcesDirectory.resolve("drawable/icon.xml").createWithParents()

        val stringFiles = composeResourceStringEscapeScanner.findStringFiles(composeResourcesDirectory)

        assertEquals(listOf(frenchStringFile, defaultStringFile), stringFiles)
    }

    private fun createStringFile(content: String): File =
        File.createTempFile(TEMPORARY_STRINGS_FILE_PREFIX, TEMPORARY_STRINGS_FILE_SUFFIX).apply {
            deleteOnExit()
            writeText(content)
        }

    private fun File.createWithParents(): File = apply {
        parentFile.mkdirs()
        writeText(EMPTY_RESOURCES)
        deleteOnExit()
    }

    private companion object {
        const val TEMPORARY_STRINGS_FILE_PREFIX = "strings"
        const val TEMPORARY_STRINGS_FILE_SUFFIX = ".xml"
        const val EMPTY_RESOURCES = "<resources/>"
    }
}
