package core.serverdriven.data.model.response

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class ComponentResponseSerializerTest {

    private val layoutJson = Json { ignoreUnknownKeys = true }

    @Test
    fun decode_withEveryKnownType_picksMatchingClass() {
        val types = listOf("shutter", "tabs", "slider", "switch", "textfield", "dropdown", "divider", "row")
        val json = types.joinToString(prefix = "[", postfix = "]") { type -> componentJson(type) }

        val components = layoutJson.decodeFromString<List<ComponentResponse>>(json)

        assertEquals(
            listOf(
                ComponentResponse.ShutterResponse::class,
                ComponentResponse.TabsResponse::class,
                ComponentResponse.SliderResponse::class,
                ComponentResponse.SwitchResponse::class,
                ComponentResponse.TextFieldResponse::class,
                ComponentResponse.DropdownResponse::class,
                ComponentResponse.DividerResponse::class,
                ComponentResponse.RowResponse::class
            ),
            components.map { component -> component::class }
        )
    }

    @Test
    fun decode_withUnknownOrMissingType_returnsUnknownResponse() {
        val components = layoutJson.decodeFromString<List<ComponentResponse>>(
            """[{"type":"tab","id":"$COMPONENT_ID"},{"id":"$COMPONENT_ID"}]"""
        )

        assertEquals(
            listOf(
                ComponentResponse.UnknownResponse(id = COMPONENT_ID, type = "tab"),
                ComponentResponse.UnknownResponse(id = COMPONENT_ID, type = null)
            ),
            components
        )

        val unknownComponent = components.first() as ComponentResponse.UnknownResponse
        assertEquals(listOf(COMPONENT_ID, "tab"), listOf(unknownComponent.id, unknownComponent.type))
    }

    private fun componentJson(type: String): String {
        return """
            {
              "type": "$type", "id": "$COMPONENT_ID", "title": "Title", "minValue": 0, "maxValue": 1,
              "currentValue": ${if (type == "slider") "0" else if (type == "switch") "true" else "\"\""},
              "defaultValue": ${if (type == "slider") "0" else "\"\""},
              "factoryValue": ${if (type == "slider") "0" else "\"\""}
            }
        """
    }

    private companion object {
        const val COMPONENT_ID = "component"
    }
}
