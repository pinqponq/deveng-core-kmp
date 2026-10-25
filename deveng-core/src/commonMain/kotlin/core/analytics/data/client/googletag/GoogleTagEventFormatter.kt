package core.analytics.data.client.googletag

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

internal object GoogleTagEventFormatter {
    const val MAXIMUM_EVENT_NAME_LENGTH = 40
    val EVENT_NAME_PATTERN = Regex("^[a-z][a-z0-9_]{0,${MAXIMUM_EVENT_NAME_LENGTH - 1}}$")

    fun sanitizeEventName(eventName: String): String? = eventName.takeIf { name -> EVENT_NAME_PATTERN.matches(name) }

    /**
     * Returns the parameters as a JSON object. A parameter named in [numericParameterNames] becomes a number
     * when its value is a finite number, and stays a string otherwise.
     */
    fun formatParameters(parameters: Map<String, String>, numericParameterNames: Set<String>): String =
        JsonObject(
            parameters.mapValues { (name, value) ->
                val number = value.toDoubleOrNull()?.takeIf { parsed -> parsed.isFinite() }
                if (name in numericParameterNames && number != null) JsonPrimitive(number) else JsonPrimitive(value)
            }
        ).toString()
}
