package core.presentation.component.json

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

/**
 * Formats a raw JSON string into a human-readable, pretty-printed structure.
 *
 * This function attempts to parse the input string as a JSON element and re-encodes it
 * with indentation and line breaks. It also performs a normalization step to handle
 * double-escaped characters (often found in log outputs).
 *
 * If parsing fails due to invalid JSON format, the original input string is returned
 * without modification.
 *
 * @param unFormattedJson The raw JSON string to be formatted. It can contain escaped characters like `\n` or `\"`.
 * @return A formatted (pretty-printed) JSON string, or the original [unFormattedJson] if parsing fails.
 */
@OptIn(ExperimentalSerializationApi::class)
fun formatJson(unFormattedJson: String): String {
    return try {
        val json = Json {
            prettyPrint = true
            // Defines the indentation string (two spaces) for each level of nesting.
            prettyPrintIndent = "  "
            isLenient = true
        }

        val normalized = unFormattedJson
            .replace("\\n", "\n")
            .replace("\\\"", "\"")

        val jsonElement = Json.parseToJsonElement(normalized)
        json.encodeToString(JsonElement.serializer(), jsonElement)

    } catch (e: Exception) {
        unFormattedJson
    }
}