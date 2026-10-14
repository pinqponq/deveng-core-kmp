package core.serverdriven.presentation.model

sealed class ComponentEvent {
    abstract val componentId: String

    data class ChangeSliderValue(
        override val componentId: String,
        val value: Float
    ) : ComponentEvent()

    data class EnterTextField(
        override val componentId: String,
        val value: String
    ) : ComponentEvent()

    data class ClickTabItem(
        override val componentId: String,
        val tabId: String
    ) : ComponentEvent()

    data class ClickSwitch(
        override val componentId: String,
        val isChecked: Boolean
    ) : ComponentEvent()

    data class ClickDropdownOption(
        override val componentId: String,
        val option: String
    ) : ComponentEvent()

    data class ClickShutter(
        override val componentId: String
    ) : ComponentEvent()
}
