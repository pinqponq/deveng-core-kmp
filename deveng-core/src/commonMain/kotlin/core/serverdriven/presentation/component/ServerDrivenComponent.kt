package core.serverdriven.presentation.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import core.presentation.component.CustomDropDownMenu
import core.presentation.component.CustomSlider
import core.presentation.component.LabeledSwitch
import core.presentation.component.RoundedSurface
import core.presentation.component.Shutter
import core.presentation.component.chips.ChipsMenu
import core.presentation.component.datepicker.CustomDatePicker
import core.presentation.component.optionitemlist.OptionItemList
import core.presentation.component.ratingrow.RatingRow
import core.presentation.component.tabrow.TabRow
import core.presentation.component.textfield.CustomTextField
import core.presentation.theme.LocalComponentTheme
import core.serverdriven.presentation.engine.scopedComponentKey
import core.serverdriven.presentation.model.ComponentEvent
import core.serverdriven.presentation.model.ComponentProperty
import core.serverdriven.presentation.model.ComponentUiModel
import kotlinx.datetime.LocalDate
import kotlin.math.roundToInt

@Composable
internal fun ServerDrivenComponent(
    modifier: Modifier,
    component: ComponentUiModel,
    componentValues: Map<String, Any>,
    componentProperties: Map<String, ComponentProperty>,
    scopeKey: String?,
    onComponentEvent: (ComponentEvent) -> Unit
) {
    val componentKey = scopedComponentKey(scopeKey, component.id)
    val componentProperty = componentProperties[componentKey] ?: ComponentProperty()

    when (component) {
        is ComponentUiModel.Shutter -> Shutter(
            containerModifier = modifier,
            title = component.title,
            description = component.description,
            isShutterExpanded = componentProperty.isExpanded,
            onClickShutter = { onComponentEvent(ComponentEvent.ClickShutter(componentId = componentKey)) },
            content = {
                ServerDrivenComponentList(
                    modifier = Modifier.fillMaxWidth(),
                    components = component.components,
                    componentValues = componentValues,
                    componentProperties = componentProperties,
                    scopeKey = scopeKey,
                    containerType = ContainerType.COLUMN,
                    onComponentEvent = onComponentEvent
                )
            }
        )

        is ComponentUiModel.Tabs -> {
            val selectedTabId = componentValues[componentKey] as? String ?: return
            val selectedTab = component.tabs.first { tab -> tab.id == selectedTabId }

            TabRow(
                modifier = modifier,
                tabs = component.tabs,
                selectedTab = selectedTab,
                tabTitle = { tab -> tab.title },
                onClickTabItem = { tab ->
                    onComponentEvent(ComponentEvent.ClickTabItem(componentId = componentKey, tabId = tab.id))
                },
                content = { tab ->
                    ServerDrivenComponentList(
                        modifier = Modifier.fillMaxWidth(),
                        components = tab.components,
                        componentValues = componentValues,
                        componentProperties = componentProperties,
                        scopeKey = scopedComponentKey(scopeKey, tab.id),
                        containerType = ContainerType.COLUMN,
                        onComponentEvent = onComponentEvent
                    )
                }
            )
        }

        is ComponentUiModel.Slider -> CustomSlider(
            containerModifier = modifier,
            title = component.title,
            minValue = component.minValue,
            maxValue = component.maxValue,
            value = componentValues.getValue(componentKey) as Float,
            isEnabled = componentProperty.isEditable,
            onValueChange = {},
            onValueChangeFinished = { value ->
                onComponentEvent(ComponentEvent.ChangeSliderValue(componentId = componentKey, value = value))
            }
        )

        is ComponentUiModel.TextField -> CustomTextField(
            containerModifier = modifier,
            title = component.title,
            value = componentValues.getValue(componentKey) as String,
            hint = component.hintMessage,
            errorMessage = component.errorMessage,
            keyboardType = component.keyboardType.toComposeKeyboardType(),
            enabled = componentProperty.isEditable,
            onValueChange = { value ->
                onComponentEvent(ComponentEvent.EnterTextField(componentId = componentKey, value = value))
            }
        )

        is ComponentUiModel.Switch -> LabeledSwitch(
            modifier = modifier,
            label = component.title,
            isChecked = componentValues.getValue(componentKey) as Boolean,
            isEnabled = componentProperty.isEditable,
            onSwitchClick = { isChecked ->
                onComponentEvent(ComponentEvent.ClickSwitch(componentId = componentKey, isChecked = isChecked))
            }
        )

        is ComponentUiModel.Dropdown -> CustomDropDownMenu(
            modifier = modifier,
            title = component.title,
            items = component.options,
            selectedItem = componentValues.getValue(componentKey) as String,
            isEnabled = componentProperty.isEditable,
            onItemSelected = { option ->
                onComponentEvent(ComponentEvent.ClickDropdownOption(componentId = componentKey, option = option))
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

        is ComponentUiModel.Label -> Text(
            modifier = modifier,
            text = component.text,
            style = LocalComponentTheme.current.serverDrivenScreen.labelTextStyle
        )

        is ComponentUiModel.Segmented -> ServerDrivenTitledContent(
            modifier = modifier,
            title = component.title,
            description = component.description
        ) {
            ChipsMenu(
                optionList = component.options,
                optionText = { option -> option },
                selectedOption = componentValues.getValue(componentKey) as String,
                onOptionClick = { option ->
                    if (componentProperty.isEditable) {
                        onComponentEvent(ComponentEvent.ClickDropdownOption(componentId = componentKey, option = option))
                    }
                }
            )
        }

        is ComponentUiModel.Radio -> ServerDrivenTitledContent(
            modifier = modifier,
            title = component.title,
            description = component.description
        ) {
            OptionItemList(
                optionList = component.options,
                optionText = { option -> option },
                optionId = { option -> option },
                isCheckIconsVisible = true,
                selectedOption = componentValues.getValue(componentKey) as String,
                onOptionItemClick = { option ->
                    if (componentProperty.isEditable) {
                        onComponentEvent(ComponentEvent.ClickDropdownOption(componentId = componentKey, option = option))
                    }
                }
            )
        }

        is ComponentUiModel.Rating -> {
            val rating = componentValues.getValue(componentKey) as Float

            ServerDrivenTitledContent(
                modifier = modifier,
                title = component.title,
                description = component.description
            ) {
                key(rating) {
                    RatingRow(
                        maxRating = component.maxValue,
                        initialRating = rating.roundToInt(),
                        isEditable = componentProperty.isEditable,
                        onRatingChanged = { selectedRating ->
                            onComponentEvent(
                                ComponentEvent.ChangeSliderValue(
                                    componentId = componentKey,
                                    value = selectedRating.toFloat()
                                )
                            )
                        }
                    )
                }
            }
        }

        is ComponentUiModel.Section -> {
            val serverDrivenScreenTheme = LocalComponentTheme.current.serverDrivenScreen

            RoundedSurface(modifier = modifier) {
                ServerDrivenTitledContent(
                    modifier = Modifier.padding(serverDrivenScreenTheme.sectionContentPadding),
                    title = component.title,
                    description = component.description
                ) {
                    ServerDrivenComponentList(
                        modifier = Modifier.fillMaxWidth(),
                        components = component.components,
                        componentValues = componentValues,
                        componentProperties = componentProperties,
                        scopeKey = scopeKey,
                        containerType = ContainerType.COLUMN,
                        onComponentEvent = onComponentEvent
                    )
                }
            }
        }

        is ComponentUiModel.Date -> {
            val dateValue = componentValues.getValue(componentKey) as String
            val selectedDate = runCatching { LocalDate.parse(dateValue) }.getOrNull()

            CustomDatePicker(
                modifier = modifier,
                isEnabled = componentProperty.isEditable,
                selectedDate = selectedDate,
                onDateChange = { date ->
                    onComponentEvent(ComponentEvent.EnterTextField(componentId = componentKey, value = date.toString()))
                },
                targetDates = component.targetDates,
                title = component.title,
                selectedDateText = selectedDate?.toString()
            )
        }

        is ComponentUiModel.Row -> ServerDrivenComponentList(
            modifier = modifier,
            components = component.components,
            componentValues = componentValues,
            componentProperties = componentProperties,
            scopeKey = scopeKey,
            containerType = ContainerType.ROW,
            onComponentEvent = onComponentEvent
        )
    }
}
