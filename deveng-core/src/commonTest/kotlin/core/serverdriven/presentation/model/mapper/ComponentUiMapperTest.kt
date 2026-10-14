package core.serverdriven.presentation.model.mapper

import core.serverdriven.data.model.response.ComponentResponse
import core.serverdriven.data.model.response.TabResponse
import core.serverdriven.presentation.model.ComponentUiModel
import core.serverdriven.presentation.model.TextFieldKeyboardType
import core.serverdriven.presentation.model.TextFieldMaskType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ComponentUiMapperTest {

    private val componentUiMapper = ComponentUiMapper()

    @Test
    fun mapToUiModels_withShutterRowAndDivider_keepsEveryFieldAndChildOrder() {
        val shutter = componentUiMapper.mapToUiModels(
            listOf(
                ComponentResponse.ShutterResponse(
                    id = SHUTTER_ID,
                    title = TITLE,
                    description = DESCRIPTION,
                    components = listOf(
                        ComponentResponse.DividerResponse(id = DIVIDER_ID),
                        ComponentResponse.RowResponse(id = ROW_ID, components = listOf(switchResponse()))
                    )
                )
            )
        ).single() as ComponentUiModel.Shutter

        assertEquals(listOf(SHUTTER_ID, TITLE, DESCRIPTION), with(shutter) { listOf(id, title, description) })
        val divider = shutter.components[0] as ComponentUiModel.Divider
        val row = shutter.components[1] as ComponentUiModel.Row
        assertEquals(DIVIDER_ID, divider.id)
        assertEquals(ROW_ID, row.id)
        assertEquals(listOf(SWITCH_ID), row.components.map { component -> component.id })
    }

    @Test
    fun mapToUiModels_withTabs_keepsEveryTabField() {
        val tabs = componentUiMapper.mapToUiModels(
            listOf(
                ComponentResponse.TabsResponse(
                    id = TABS_ID,
                    tabs = listOf(
                        TabResponse(id = TAB_ID, title = TITLE, description = DESCRIPTION, components = listOf(switchResponse()))
                    )
                )
            )
        ).single() as ComponentUiModel.Tabs

        val tab = tabs.tabs.single()
        assertEquals(TABS_ID, tabs.id)
        assertEquals(listOf(TAB_ID, TITLE, DESCRIPTION), with(tab) { listOf(id, title, description) })
        assertEquals(listOf(SWITCH_ID), tab.components.map { component -> component.id })
    }

    @Test
    fun mapToUiModels_withSwitch_keepsEveryField() {
        val switch = componentUiMapper.mapToUiModels(listOf(switchResponse())).single() as ComponentUiModel.Switch

        assertEquals(
            listOf<Any?>(SWITCH_ID, TITLE, DESCRIPTION, true),
            with(switch) { listOf(id, title, description, currentValue) }
        )
    }

    @Test
    fun mapToUiModels_withSlider_keepsEveryFieldAsFloat() {
        val slider = componentUiMapper.mapToUiModels(
            listOf(
                ComponentResponse.SliderResponse(
                    id = SLIDER_ID,
                    title = TITLE,
                    description = DESCRIPTION,
                    minValue = 1,
                    maxValue = 10,
                    currentValue = 3,
                    defaultValue = 5,
                    factoryValue = 6
                )
            )
        ).single() as ComponentUiModel.Slider

        assertEquals(
            listOf<Any?>(SLIDER_ID, TITLE, DESCRIPTION, 1f, 10f, 3f, 5f, 6f),
            with(slider) { listOf(id, title, description, minValue, maxValue, currentValue, defaultValue, factoryValue) }
        )
    }

    @Test
    fun mapToUiModels_withDropdown_keepsEveryField() {
        val dropdown = componentUiMapper.mapToUiModels(
            listOf(
                ComponentResponse.DropdownResponse(
                    id = DROPDOWN_ID,
                    title = TITLE,
                    description = DESCRIPTION,
                    currentValue = CURRENT_VALUE,
                    defaultValue = DEFAULT_VALUE,
                    factoryValue = FACTORY_VALUE,
                    options = OPTIONS
                )
            )
        ).single() as ComponentUiModel.Dropdown

        assertEquals(
            listOf<Any?>(DROPDOWN_ID, TITLE, DESCRIPTION, CURRENT_VALUE, DEFAULT_VALUE, FACTORY_VALUE, OPTIONS),
            with(dropdown) { listOf(id, title, description, currentValue, defaultValue, factoryValue, options) }
        )
    }

    @Test
    fun mapToUiModels_withTextField_keepsEveryFieldAndMapsTypesIgnoringCase() {
        val textField = mapTextField(keyboardType = "numeric", format = "time")

        assertEquals(
            listOf<Any?>(
                TEXT_FIELD_ID, TITLE, DESCRIPTION, CURRENT_VALUE, DEFAULT_VALUE, FACTORY_VALUE, ERROR_MESSAGE, HINT_MESSAGE,
                TextFieldMaskType.TIME, TextFieldKeyboardType.NUMERIC
            ),
            with(textField) {
                listOf(
                    id, title, description, currentValue, defaultValue, factoryValue, errorMessage, hintMessage,
                    maskType, keyboardType
                )
            }
        )
    }

    @Test
    fun mapToUiModels_withUnknownKeyboardTypeAndFormat_fallsBackToTextWithoutMask() {
        val textField = mapTextField(keyboardType = "DIAL_PAD", format = "IBAN")

        assertEquals(TextFieldKeyboardType.TEXT, textField.keyboardType)
        assertNull(textField.maskType)
    }

    @Test
    fun mapToUiModels_withoutKeyboardTypeAndFormat_usesTextWithoutMask() {
        val textField = mapTextField(keyboardType = null, format = null)

        assertEquals(TextFieldKeyboardType.TEXT, textField.keyboardType)
        assertNull(textField.maskType)
    }

    @Test
    fun mapToUiModels_withUnknownComponent_dropsOnlyThatComponent() {
        val components = componentUiMapper.mapToUiModels(
            listOf(ComponentResponse.UnknownResponse(id = UNKNOWN_ID, type = UNKNOWN_TYPE), switchResponse())
        )

        assertEquals(listOf(SWITCH_ID), components.map { component -> component.id })
    }

    private fun switchResponse() = ComponentResponse.SwitchResponse(
        id = SWITCH_ID,
        title = TITLE,
        description = DESCRIPTION,
        currentValue = true
    )

    private fun mapTextField(keyboardType: String?, format: String?): ComponentUiModel.TextField {
        return componentUiMapper.mapToUiModels(
            listOf(
                ComponentResponse.TextFieldResponse(
                    id = TEXT_FIELD_ID,
                    title = TITLE,
                    description = DESCRIPTION,
                    currentValue = CURRENT_VALUE,
                    defaultValue = DEFAULT_VALUE,
                    factoryValue = FACTORY_VALUE,
                    errorMessage = ERROR_MESSAGE,
                    hintMessage = HINT_MESSAGE,
                    format = format,
                    keyboardType = keyboardType
                )
            )
        ).single() as ComponentUiModel.TextField
    }

    private companion object {
        const val SHUTTER_ID = "camera_shutter"
        const val DIVIDER_ID = "camera_divider"
        const val ROW_ID = "record_row"
        const val TABS_ID = "mode_tabs"
        const val TAB_ID = "day_tab"
        const val SWITCH_ID = "record_switch"
        const val SLIDER_ID = "contrast_slider"
        const val DROPDOWN_ID = "resolution_dropdown"
        const val TEXT_FIELD_ID = "start_time_text_field"
        const val UNKNOWN_ID = "usage_chart"
        const val UNKNOWN_TYPE = "chart"
        const val TITLE = "Title"
        const val DESCRIPTION = "Description"
        const val CURRENT_VALUE = "current"
        const val DEFAULT_VALUE = "default"
        const val FACTORY_VALUE = "factory"
        const val ERROR_MESSAGE = "Invalid time"
        const val HINT_MESSAGE = "HH:MM"
        val OPTIONS = listOf("720p", "1080p")
    }
}
