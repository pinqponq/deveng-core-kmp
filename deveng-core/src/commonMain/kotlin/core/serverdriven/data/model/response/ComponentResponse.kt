package core.serverdriven.data.model.response

import kotlinx.serialization.Serializable

@Serializable(with = ComponentResponseSerializer::class)
sealed class ComponentResponse {
    abstract val id: String

    @Serializable
    data class ShutterResponse(
        override val id: String,
        val title: String,
        val description: String? = null,
        val components: List<ComponentResponse> = emptyList()
    ) : ComponentResponse()

    @Serializable
    data class TabsResponse(
        override val id: String,
        val tabs: List<TabResponse> = emptyList()
    ) : ComponentResponse()

    @Serializable
    data class SliderResponse(
        override val id: String,
        val title: String,
        val description: String? = null,
        val minValue: Int,
        val maxValue: Int,
        val currentValue: Int,
        val defaultValue: Int,
        val factoryValue: Int
    ) : ComponentResponse()

    @Serializable
    data class SwitchResponse(
        override val id: String,
        val title: String,
        val description: String? = null,
        val currentValue: Boolean
    ) : ComponentResponse()

    @Serializable
    data class TextFieldResponse(
        override val id: String,
        val title: String,
        val description: String? = null,
        val currentValue: String,
        val defaultValue: String,
        val factoryValue: String,
        val errorMessage: String? = null,
        val hintMessage: String? = null,
        val format: String? = null,
        val keyboardType: String? = null
    ) : ComponentResponse()

    @Serializable
    data class DropdownResponse(
        override val id: String,
        val title: String,
        val description: String? = null,
        val currentValue: String,
        val defaultValue: String,
        val factoryValue: String,
        val options: List<String> = emptyList()
    ) : ComponentResponse()

    @Serializable
    data class DividerResponse(
        override val id: String
    ) : ComponentResponse()

    @Serializable
    data class RowResponse(
        override val id: String,
        val components: List<ComponentResponse> = emptyList()
    ) : ComponentResponse()

    @Serializable
    data class LabelResponse(
        override val id: String,
        val text: String
    ) : ComponentResponse()

    @Serializable
    data class SegmentedResponse(
        override val id: String,
        val title: String,
        val description: String? = null,
        val currentValue: String,
        val defaultValue: String,
        val factoryValue: String,
        val options: List<String> = emptyList()
    ) : ComponentResponse()

    @Serializable
    data class RadioResponse(
        override val id: String,
        val title: String,
        val description: String? = null,
        val currentValue: String,
        val defaultValue: String,
        val factoryValue: String,
        val options: List<String> = emptyList()
    ) : ComponentResponse()

    @Serializable
    data class RatingResponse(
        override val id: String,
        val title: String,
        val description: String? = null,
        val maxValue: Int = DEFAULT_MAX_RATING,
        val currentValue: Float,
        val defaultValue: Float,
        val factoryValue: Float
    ) : ComponentResponse()

    @Serializable
    data class SectionResponse(
        override val id: String,
        val title: String,
        val description: String? = null,
        val components: List<ComponentResponse> = emptyList()
    ) : ComponentResponse()

    @Serializable
    data class DateResponse(
        override val id: String,
        val title: String,
        val description: String? = null,
        val currentValue: String,
        val defaultValue: String,
        val factoryValue: String,
        val targetDates: String? = null
    ) : ComponentResponse()

    @Serializable
    data class UnknownResponse(
        override val id: String,
        val type: String? = null
    ) : ComponentResponse()
}

private const val DEFAULT_MAX_RATING = 5
