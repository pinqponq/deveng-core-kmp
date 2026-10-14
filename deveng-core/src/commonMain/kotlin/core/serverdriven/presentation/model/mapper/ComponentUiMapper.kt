package core.serverdriven.presentation.model.mapper

import core.serverdriven.data.model.response.ComponentResponse
import core.serverdriven.data.model.response.TabResponse
import core.serverdriven.presentation.model.ComponentUiModel
import core.serverdriven.presentation.model.SkippedElementKind
import core.serverdriven.presentation.model.SkippedLayoutElement
import core.serverdriven.presentation.model.TabUiModel
import core.serverdriven.presentation.model.TextFieldKeyboardType
import core.serverdriven.presentation.model.TextFieldMaskType
import core.util.CustomLogger
import core.util.EMPTY
import core.util.datetime.TargetDates
import kotlinx.datetime.LocalDate

internal class ComponentUiMapper(
    private val onElementSkip: (SkippedLayoutElement) -> Unit = {}
) {

    fun mapToUiModels(
        responses: List<ComponentResponse>
    ): List<ComponentUiModel> {
        return responses.mapNotNull { response -> mapToUiModel(response) }
    }

    private fun mapToUiModel(
        response: ComponentResponse
    ): ComponentUiModel? {
        return when (response) {
            is ComponentResponse.ShutterResponse -> ComponentUiModel.Shutter(
                id = response.id,
                title = response.title,
                description = response.description,
                components = mapToUiModels(response.components)
            )

            is ComponentResponse.TabsResponse -> ComponentUiModel.Tabs(
                id = response.id,
                tabs = response.tabs.map { tabResponse -> mapToTabUiModel(tabResponse) }
            )

            is ComponentResponse.SliderResponse -> ComponentUiModel.Slider(
                id = response.id,
                title = response.title,
                description = response.description,
                minValue = response.minValue.toFloat(),
                maxValue = response.maxValue.toFloat(),
                currentValue = response.currentValue.toFloat(),
                defaultValue = response.defaultValue.toFloat(),
                factoryValue = response.factoryValue.toFloat()
            )

            is ComponentResponse.SwitchResponse -> ComponentUiModel.Switch(
                id = response.id,
                title = response.title,
                description = response.description,
                currentValue = response.currentValue
            )

            is ComponentResponse.TextFieldResponse -> ComponentUiModel.TextField(
                id = response.id,
                title = response.title,
                description = response.description,
                currentValue = response.currentValue,
                defaultValue = response.defaultValue,
                factoryValue = response.factoryValue,
                errorMessage = response.errorMessage,
                hintMessage = response.hintMessage ?: String.EMPTY,
                maskType = mapToMaskType(response),
                keyboardType = mapToKeyboardType(response)
            )

            is ComponentResponse.DropdownResponse -> ComponentUiModel.Dropdown(
                id = response.id,
                title = response.title,
                description = response.description,
                currentValue = response.currentValue,
                defaultValue = response.defaultValue,
                factoryValue = response.factoryValue,
                options = response.options
            )

            is ComponentResponse.DividerResponse -> ComponentUiModel.Divider(
                id = response.id
            )

            is ComponentResponse.RowResponse -> ComponentUiModel.Row(
                id = response.id,
                components = mapToUiModels(response.components)
            )

            is ComponentResponse.LabelResponse -> ComponentUiModel.Label(
                id = response.id,
                text = response.text
            )

            is ComponentResponse.SegmentedResponse -> ComponentUiModel.Segmented(
                id = response.id,
                title = response.title,
                description = response.description,
                currentValue = response.currentValue,
                defaultValue = response.defaultValue,
                factoryValue = response.factoryValue,
                options = response.options
            )

            is ComponentResponse.RadioResponse -> ComponentUiModel.Radio(
                id = response.id,
                title = response.title,
                description = response.description,
                currentValue = response.currentValue,
                defaultValue = response.defaultValue,
                factoryValue = response.factoryValue,
                options = response.options
            )

            is ComponentResponse.RatingResponse -> ComponentUiModel.Rating(
                id = response.id,
                title = response.title,
                description = response.description,
                maxValue = response.maxValue,
                currentValue = response.currentValue,
                defaultValue = response.defaultValue,
                factoryValue = response.factoryValue
            )

            is ComponentResponse.SectionResponse -> ComponentUiModel.Section(
                id = response.id,
                title = response.title,
                description = response.description,
                components = mapToUiModels(response.components)
            )

            is ComponentResponse.DateResponse -> ComponentUiModel.Date(
                id = response.id,
                title = response.title,
                description = response.description,
                currentValue = mapToDateValue(response),
                defaultValue = response.defaultValue,
                factoryValue = response.factoryValue,
                targetDates = mapToTargetDates(response)
            )

            is ComponentResponse.UnknownResponse -> {
                CustomLogger.log(
                    "Server-driven UI: skipped component '${response.id}' with unknown type '${response.type}'"
                )

                onElementSkip(
                    SkippedLayoutElement(
                        kind = SkippedElementKind.COMPONENT_TYPE,
                        name = response.type,
                        componentId = response.id
                    )
                )

                null
            }
        }
    }

    private fun mapToTabUiModel(
        response: TabResponse
    ): TabUiModel {
        return TabUiModel(
            id = response.id,
            title = response.title,
            description = response.description,
            components = mapToUiModels(response.components)
        )
    }

    private fun mapToMaskType(
        response: ComponentResponse.TextFieldResponse
    ): TextFieldMaskType? {
        val format = response.format ?: return null
        val maskType = TextFieldMaskType.fromValue(format)

        if (maskType == null) {
            CustomLogger.log(
                "Server-driven UI: text field '${response.id}' has unknown format '$format', no mask applied"
            )
        }

        return maskType
    }

    private fun mapToKeyboardType(
        response: ComponentResponse.TextFieldResponse
    ): TextFieldKeyboardType {
        val keyboardTypeName = response.keyboardType ?: return TextFieldKeyboardType.TEXT
        val keyboardType = TextFieldKeyboardType.fromValue(keyboardTypeName)

        if (keyboardType == null) {
            CustomLogger.log(
                "Server-driven UI: text field '${response.id}' has unknown keyboard type '$keyboardTypeName', using TEXT"
            )
        }

        return keyboardType ?: TextFieldKeyboardType.TEXT
    }

    private fun mapToDateValue(
        response: ComponentResponse.DateResponse
    ): String {
        val dateValue = response.currentValue
        val isDateValueValid = dateValue.isEmpty() || runCatching { LocalDate.parse(dateValue) }.isSuccess

        if (!isDateValueValid) {
            CustomLogger.log(
                "Server-driven UI: date '${response.id}' has a value '$dateValue' that is not an ISO-8601 date, " +
                    "no date selected"
            )
        }

        return if (isDateValueValid) dateValue else String.EMPTY
    }

    private fun mapToTargetDates(
        response: ComponentResponse.DateResponse
    ): TargetDates {
        val targetDatesName = response.targetDates ?: return TargetDates.PAST
        val targetDates = TargetDates.entries.firstOrNull { targetDates ->
            targetDates.name.equals(targetDatesName, ignoreCase = true)
        }

        if (targetDates == null) {
            CustomLogger.log(
                "Server-driven UI: date '${response.id}' has unknown target dates '$targetDatesName', using PAST"
            )
        }

        return targetDates ?: TargetDates.PAST
    }
}
