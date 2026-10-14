package core.serverdriven.data.model.response

import core.util.CustomLogger
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Picks the component class from the `type` field. A type this library does not know decodes to
 * [ComponentResponse.UnknownResponse], so a layout with a newer component still loads on an older
 * app; the engine skips and logs it. The deprecated type `tab` decodes as `tabs` and is logged once per
 * component. The decoding `Json` needs `ignoreUnknownKeys = true` because the
 * `type` field stays in the object the selected class reads.
 */
internal object ComponentResponseSerializer :
    JsonContentPolymorphicSerializer<ComponentResponse>(ComponentResponse::class) {

    private const val TYPE_KEY = "type"
    private const val ID_KEY = "id"

    override fun selectDeserializer(
        element: JsonElement
    ): DeserializationStrategy<ComponentResponse> {
        val typeName = element.jsonObject[TYPE_KEY]?.jsonPrimitive?.contentOrNull

        return when (typeName?.let(ComponentType::fromSerialName)) {
            ComponentType.SHUTTER -> ComponentResponse.ShutterResponse.serializer()
            ComponentType.TABS -> ComponentResponse.TabsResponse.serializer()
            ComponentType.DEPRECATED_TABS -> {
                logDeprecatedTabsType(element)
                ComponentResponse.TabsResponse.serializer()
            }

            ComponentType.SLIDER -> ComponentResponse.SliderResponse.serializer()
            ComponentType.SWITCH -> ComponentResponse.SwitchResponse.serializer()
            ComponentType.TEXT_FIELD -> ComponentResponse.TextFieldResponse.serializer()
            ComponentType.DROPDOWN -> ComponentResponse.DropdownResponse.serializer()
            ComponentType.DIVIDER -> ComponentResponse.DividerResponse.serializer()
            ComponentType.ROW -> ComponentResponse.RowResponse.serializer()
            ComponentType.LABEL -> ComponentResponse.LabelResponse.serializer()
            ComponentType.SEGMENTED -> ComponentResponse.SegmentedResponse.serializer()
            ComponentType.RADIO -> ComponentResponse.RadioResponse.serializer()
            ComponentType.RATING -> ComponentResponse.RatingResponse.serializer()
            ComponentType.SECTION -> ComponentResponse.SectionResponse.serializer()
            ComponentType.DATE -> ComponentResponse.DateResponse.serializer()
            null -> ComponentResponse.UnknownResponse.serializer()
        }
    }

    private fun logDeprecatedTabsType(element: JsonElement) {
        val componentId = element.jsonObject[ID_KEY]?.jsonPrimitive?.contentOrNull

        CustomLogger.log(
            "Server-driven UI: component '$componentId' uses the deprecated type " +
                "'${ComponentType.DEPRECATED_TABS.serialName}', use '${ComponentType.TABS.serialName}'"
        )
    }
}
