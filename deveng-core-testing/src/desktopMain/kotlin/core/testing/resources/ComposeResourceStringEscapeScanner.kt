package core.testing.resources

import org.w3c.dom.Element
import org.w3c.dom.NodeList
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class ComposeResourceStringEscapeScanner {

    fun findStringFiles(composeResourcesDirectory: File): List<File> =
        composeResourcesDirectory
            .walkTopDown()
            .filter { file -> file.name == STRINGS_FILE_NAME }
            .sortedBy { file -> file.path }
            .toList()

    fun findLiteralEscapes(stringFile: File): List<String> {
        val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(stringFile)
        val stringElements = document.getElementsByTagName(STRING_TAG).toElementList()
        val pluralItemElements = document.getElementsByTagName(PLURAL_ITEM_TAG).toElementList()

        return (stringElements + pluralItemElements).flatMap { element ->
            val resourceKey = createResourceKey(element)
            LITERAL_ESCAPE_CANDIDATE_REGEX.findAll(element.textContent)
                .map { matchResult -> matchResult.value }
                .filterNot(::isEscapeResolvedByCompose)
                .map { literalEscape -> "${stringFile.path}: $resourceKey contains $literalEscape" }
                .toList()
        }
    }

    private fun createResourceKey(element: Element): String {
        if (element.tagName == STRING_TAG) return element.getAttribute(NAME_ATTRIBUTE)

        val pluralsElement = element.parentNode as Element
        return "${pluralsElement.getAttribute(NAME_ATTRIBUTE)}[${element.getAttribute(QUANTITY_ATTRIBUTE)}]"
    }

    private fun isEscapeResolvedByCompose(escape: String): Boolean =
        COMPOSE_RESOLVED_ESCAPE_REGEX.matches(escape)

    private fun NodeList.toElementList(): List<Element> =
        (0 until length).map { index -> item(index) as Element }

    private companion object {
        const val STRINGS_FILE_NAME = "strings.xml"
        const val STRING_TAG = "string"
        const val PLURAL_ITEM_TAG = "item"
        const val NAME_ATTRIBUTE = "name"
        const val QUANTITY_ATTRIBUTE = "quantity"

        val LITERAL_ESCAPE_CANDIDATE_REGEX = Regex("""\\(u[0-9a-fA-F]{4}|[\s\S]?)""")

        // Mirrors handleSpecialCharacters in the Compose resources Gradle plugin (1.10.0):
        // only \uXXXX, \n, \t and \\ are converted; every other backslash stays in the text.
        val COMPOSE_RESOLVED_ESCAPE_REGEX = Regex("""\\(u[0-9a-fA-F]{4}|n|t|\\)""")
    }
}
