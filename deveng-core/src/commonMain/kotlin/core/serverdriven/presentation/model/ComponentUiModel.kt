package core.serverdriven.presentation.model

import core.util.datetime.TargetDates
import kotlinx.datetime.LocalDate

sealed class ComponentUiModel {
    abstract val id: String

    data class Shutter(
        override val id: String,
        val title: String,
        val description: String?,
        val components: List<ComponentUiModel>
    ) : ComponentUiModel()

    data class Tabs(
        override val id: String,
        val tabs: List<TabUiModel>
    ) : ComponentUiModel()

    data class Slider(
        override val id: String,
        val title: String,
        val description: String?,
        val minValue: Float,
        val maxValue: Float,
        val currentValue: Float,
        val defaultValue: Float,
        val factoryValue: Float
    ) : ComponentUiModel()

    data class Switch(
        override val id: String,
        val title: String,
        val description: String?,
        val currentValue: Boolean
    ) : ComponentUiModel()

    data class TextField(
        override val id: String,
        val title: String,
        val description: String?,
        val currentValue: String,
        val defaultValue: String,
        val factoryValue: String,
        val errorMessage: String?,
        val hintMessage: String,
        val maskType: TextFieldMaskType?,
        val keyboardType: TextFieldKeyboardType
    ) : ComponentUiModel()

    data class Dropdown(
        override val id: String,
        val title: String,
        val description: String?,
        val currentValue: String,
        val defaultValue: String,
        val factoryValue: String,
        val options: List<String>
    ) : ComponentUiModel()

    data class Divider(
        override val id: String
    ) : ComponentUiModel()

    data class Row(
        override val id: String,
        val components: List<ComponentUiModel>
    ) : ComponentUiModel()

    data class Label(
        override val id: String,
        val text: String
    ) : ComponentUiModel()

    data class Segmented(
        override val id: String,
        val title: String,
        val description: String?,
        val currentValue: String,
        val defaultValue: String,
        val factoryValue: String,
        val options: List<String>
    ) : ComponentUiModel()

    data class Radio(
        override val id: String,
        val title: String,
        val description: String?,
        val currentValue: String,
        val defaultValue: String,
        val factoryValue: String,
        val options: List<String>
    ) : ComponentUiModel()

    data class Rating(
        override val id: String,
        val title: String,
        val description: String?,
        val maxValue: Int,
        val currentValue: Float,
        val defaultValue: Float,
        val factoryValue: Float
    ) : ComponentUiModel()

    data class Section(
        override val id: String,
        val title: String,
        val description: String?,
        val components: List<ComponentUiModel>
    ) : ComponentUiModel()

    data class Date(
        override val id: String,
        val title: String,
        val description: String?,
        val currentValue: LocalDate?,
        val defaultValue: LocalDate?,
        val factoryValue: LocalDate?,
        val targetDates: TargetDates
    ) : ComponentUiModel()
}
