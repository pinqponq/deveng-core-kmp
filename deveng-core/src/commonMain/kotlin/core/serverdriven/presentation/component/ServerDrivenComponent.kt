package core.serverdriven.presentation.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import core.presentation.component.CustomDropDownMenu
import core.presentation.component.CustomSlider
import core.presentation.component.LabeledSwitch
import core.presentation.component.Shutter
import core.presentation.component.tabrow.TabRow
import core.presentation.component.textfield.CustomTextField
import core.presentation.theme.LocalComponentTheme
import core.serverdriven.presentation.model.ComponentEvent
import core.serverdriven.presentation.model.ComponentProperty
import core.serverdriven.presentation.model.ComponentUiModel
import core.util.EMPTY

@Composable
internal fun ServerDrivenComponent(
    modifier: Modifier,
    component: ComponentUiModel,
    componentValues: Map<String, Any>,
    componentProperties: Map<String, ComponentProperty>,
    onComponentEvent: (ComponentEvent) -> Unit
) {
    val componentProperty = componentProperties[component.id] ?: ComponentProperty()

    when (component) {
        is ComponentUiModel.Shutter -> Shutter(
            containerModifier = modifier,
            title = component.title,
            description = component.description,
            isShutterExpanded = componentProperty.isExpanded,
            onClickShutter = { onComponentEvent(ComponentEvent.ClickShutter(componentId = component.id)) },
            content = {
                ServerDrivenComponentList(
                    modifier = Modifier.fillMaxWidth(),
                    components = component.components,
                    componentValues = componentValues,
                    componentProperties = componentProperties,
                    containerType = ContainerType.COLUMN,
                    onComponentEvent = onComponentEvent
                )
            }
        )

        is ComponentUiModel.Tabs -> {
            val selectedTabId = componentValues[component.id] as? String
            val selectedTab = component.tabs.firstOrNull { tab -> tab.id == selectedTabId }
                ?: component.tabs.firstOrNull()
                ?: return

            TabRow(
                modifier = modifier,
                tabs = component.tabs,
                selectedTab = selectedTab,
                tabTitle = { tab -> tab.title },
                onClickTabItem = { tab ->
                    onComponentEvent(ComponentEvent.ClickTabItem(componentId = component.id, tabId = tab.id))
                },
                content = { tab ->
                    ServerDrivenComponentList(
                        modifier = Modifier.fillMaxWidth(),
                        components = tab.components,
                        componentValues = componentValues,
                        componentProperties = componentProperties,
                        containerType = ContainerType.COLUMN,
                        onComponentEvent = onComponentEvent
                    )
                }
            )
        }

        is ComponentUiModel.Slider -> {
            val engineValue = componentValues[component.id] as? Float ?: component.currentValue
            var sliderValue by remember(engineValue) { mutableStateOf(engineValue) }

            CustomSlider(
                containerModifier = modifier,
                title = component.title,
                minValue = component.minValue,
                maxValue = component.maxValue,
                value = sliderValue,
                isEnabled = componentProperty.isEditable,
                onValueChange = { value -> sliderValue = value },
                onValueChangeFinished = { value ->
                    onComponentEvent(ComponentEvent.ChangeSliderValue(componentId = component.id, value = value))
                }
            )
        }

        is ComponentUiModel.TextField -> CustomTextField(
            containerModifier = modifier,
            title = component.title,
            value = componentValues[component.id] as? String ?: component.currentValue,
            hint = component.hintMessage ?: String.EMPTY,
            errorMessage = component.errorMessage,
            keyboardType = component.keyboardType.toComposeKeyboardType(),
            enabled = componentProperty.isEditable,
            onValueChange = { value ->
                onComponentEvent(ComponentEvent.EnterTextField(componentId = component.id, value = value))
            }
        )

        is ComponentUiModel.Switch -> LabeledSwitch(
            modifier = modifier,
            label = component.title,
            isChecked = componentValues[component.id] as? Boolean ?: component.currentValue,
            isEnabled = componentProperty.isEditable,
            onSwitchClick = { isChecked ->
                onComponentEvent(ComponentEvent.ClickSwitch(componentId = component.id, isChecked = isChecked))
            }
        )

        is ComponentUiModel.Dropdown -> CustomDropDownMenu(
            modifier = modifier,
            title = component.title,
            items = component.options,
            selectedItem = componentValues[component.id] as? String ?: component.currentValue,
            isEnabled = componentProperty.isEditable,
            onItemSelected = { option ->
                onComponentEvent(ComponentEvent.ClickDropdownOption(componentId = component.id, option = option))
            }
        )

        is ComponentUiModel.Divider -> {
            val serverDrivenScreenTheme = LocalComponentTheme.current.serverDrivenScreen

            HorizontalDivider(
                modifier = modifier,
                color = serverDrivenScreenTheme.dividerColor,
                thickness = serverDrivenScreenTheme.dividerThickness
            )
        }

        is ComponentUiModel.Row -> ServerDrivenComponentList(
            modifier = modifier,
            components = component.components,
            componentValues = componentValues,
            componentProperties = componentProperties,
            containerType = ContainerType.ROW,
            onComponentEvent = onComponentEvent
        )
    }
}
