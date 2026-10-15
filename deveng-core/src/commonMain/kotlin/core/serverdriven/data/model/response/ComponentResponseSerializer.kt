package core.serverdriven.data.model.response

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Picks the component class from the `type` field. A type this library does not know decodes to
 * [ComponentResponse.UnknownResponse], so a layout with a newer component still loads on an older
 * app; the engine skips and logs it. The decoding `Json` needs `ignoreUnknownKeys = true` because the
 * `type` field stays in the object the selected class reads.
 */
internal object ComponentResponseSerializer :
    JsonContentPolymorphicSerializer<ComponentResponse>(ComponentResponse::class) {

    private const val TYPE_KEY = "type"

    override fun selectDeserializer(
        element: JsonElement
    ): DeserializationStrategy<ComponentResponse> {
        val typeName = element.jsonObject[TYPE_KEY]?.jsonPrimitive?.contentOrNull

        return when (typeName?.let(ComponentType::fromSerialName)) {
            ComponentType.SHUTTER -> ComponentResponse.ShutterResponse.serializer()
            ComponentType.TABS -> ComponentResponse.TabsResponse.serializer()
            ComponentType.SLIDER -> ComponentResponse.SliderResponse.serializer()
            ComponentType.SWITCH -> ComponentResponse.SwitchResponse.serializer()
            ComponentType.TEXT_FIELD -> ComponentResponse.TextFieldResponse.serializer()
            ComponentType.DROPDOWN -> ComponentResponse.DropdownResponse.serializer()
            ComponentType.DIVIDER -> ComponentResponse.DividerResponse.serializer()
            ComponentType.ROW -> ComponentResponse.RowResponse.serializer()
            null -> ComponentResponse.UnknownResponse.serializer()
        }
    }
}
