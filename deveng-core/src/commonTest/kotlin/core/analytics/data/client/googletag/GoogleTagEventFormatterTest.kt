package core.analytics.data.client.googletag

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GoogleTagEventFormatterTest {

    @Test
    fun sanitizeEventName_withSnakeCaseName_returnsTheName() {
        assertEquals("signup_completed", GoogleTagEventFormatter.sanitizeEventName("signup_completed"))
    }

    @Test
    fun sanitizeEventName_withNameOfMaximumLength_returnsTheName() {
        val name = "a".repeat(GoogleTagEventFormatter.MAXIMUM_EVENT_NAME_LENGTH)

        assertEquals(name, GoogleTagEventFormatter.sanitizeEventName(name))
    }

    @Test
    fun sanitizeEventName_withNameLongerThanMaximum_returnsNull() {
        val name = "a".repeat(GoogleTagEventFormatter.MAXIMUM_EVENT_NAME_LENGTH + 1)

        assertNull(GoogleTagEventFormatter.sanitizeEventName(name))
    }

    @Test
    fun sanitizeEventName_withInvalidName_returnsNull() {
        listOf("", "SIGNUP", "1_signup", "_signup", "sign up", "sign-up", "signup\n").forEach { name ->
            assertNull(GoogleTagEventFormatter.sanitizeEventName(name), "'$name' must be rejected")
        }
    }

    @Test
    fun formatParameters_withNumericParameterName_writesANumber() {
        val parameters = parse(GoogleTagEventFormatter.formatParameters(mapOf("value" to "9.5"), NUMERIC_NAMES))

        assertEquals(9.5, parameters.getValue("value").asDouble())
    }

    @Test
    fun formatParameters_withNonNumericValueForNumericName_keepsTheString() {
        val parameters = parse(GoogleTagEventFormatter.formatParameters(mapOf("value" to "free"), NUMERIC_NAMES))

        assertEquals("free", (parameters.getValue("value") as JsonPrimitive).content)
        assertTrue((parameters.getValue("value") as JsonPrimitive).isString)
    }

    @Test
    fun formatParameters_withNotFiniteValueForNumericName_keepsTheString() {
        val parameters = parse(GoogleTagEventFormatter.formatParameters(mapOf("value" to "NaN", "tax" to "Infinity"), NUMERIC_NAMES))

        assertTrue((parameters.getValue("value") as JsonPrimitive).isString)
        assertTrue((parameters.getValue("tax") as JsonPrimitive).isString)
    }

    @Test
    fun formatParameters_withNumericValueForOtherName_keepsTheString() {
        val parameters = parse(GoogleTagEventFormatter.formatParameters(mapOf("item_count" to "3"), NUMERIC_NAMES))

        assertTrue((parameters.getValue("item_count") as JsonPrimitive).isString)
    }

    @Test
    fun formatParameters_withNoParameters_writesAnEmptyObject() {
        assertEquals("{}", GoogleTagEventFormatter.formatParameters(emptyMap(), NUMERIC_NAMES))
    }

    private fun parse(json: String): JsonObject = Json.parseToJsonElement(json) as JsonObject

    private fun kotlinx.serialization.json.JsonElement.asDouble(): Double? = (this as JsonPrimitive).doubleOrNull

    private companion object {
        val NUMERIC_NAMES = setOf("value", "tax")
    }
}
