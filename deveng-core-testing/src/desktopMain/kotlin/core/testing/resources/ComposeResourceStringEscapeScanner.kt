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
        val stringFindings = findElements(document.getElementsByTagName(STRING_TAG)).flatMap { stringElement ->
            findLiteralEscapes(
                stringFile = stringFile,
                resourceKey = stringElement.getAttribute(NAME_ATTRIBUTE).orEmpty(),
                text = stringElement.textContent.orEmpty()
            )
        }
        val pluralFindings = findElements(document.getElementsByTagName(PLURALS_TAG)).flatMap { pluralsElement ->
            findPluralLiteralEscapes(stringFile = stringFile, pluralsElement = pluralsElement)
        }

        return stringFindings + pluralFindings
    }

    private fun findPluralLiteralEscapes(stringFile: File, pluralsElement: Element): List<String> {
        val pluralsKey = pluralsElement.getAttribute(NAME_ATTRIBUTE).orEmpty()

        return findElements(pluralsElement.getElementsByTagName(PLURAL_ITEM_TAG)).flatMap { itemElement ->
            findLiteralEscapes(
                stringFile = stringFile,
                resourceKey = "$pluralsKey[${itemElement.getAttribute(QUANTITY_ATTRIBUTE).orEmpty()}]",
                text = itemElement.textContent.orEmpty()
            )
        }
    }

    private fun findLiteralEscapes(stringFile: File, resourceKey: String, text: String): List<String> =
        LITERAL_ESCAPE_CANDIDATE_REGEX.findAll(text)
            .map { matchResult -> matchResult.value }
            .filterNot(::isEscapeResolvedByCompose)
            .map { literalEscape -> "${stringFile.path}: $resourceKey contains $literalEscape" }
            .toList()

    private fun isEscapeResolvedByCompose(escape: String): Boolean =
        COMPOSE_RESOLVED_ESCAPE_REGEX.matches(escape)

    private fun findElements(nodeList: NodeList?): List<Element> {
        if (nodeList == null) return emptyList()

        return (0 until nodeList.length).mapNotNull { index -> nodeList.item(index) as? Element }
    }

    private companion object {
        const val STRINGS_FILE_NAME = "strings.xml"
        const val STRING_TAG = "string"
        const val PLURALS_TAG = "plurals"
        const val PLURAL_ITEM_TAG = "item"
        const val NAME_ATTRIBUTE = "name"
        const val QUANTITY_ATTRIBUTE = "quantity"

        val LITERAL_ESCAPE_CANDIDATE_REGEX = Regex("""\\(u[0-9a-fA-F]{4}|[\s\S]?)""")

        // Mirrors handleSpecialCharacters in the Compose resources Gradle plugin (1.10.0):
        // only \uXXXX, \n, \t and \\ are converted; every other backslash stays in the text.
        val COMPOSE_RESOLVED_ESCAPE_REGEX = Regex("""\\(u[0-9a-fA-F]{4}|n|t|\\)""")
    }
}
