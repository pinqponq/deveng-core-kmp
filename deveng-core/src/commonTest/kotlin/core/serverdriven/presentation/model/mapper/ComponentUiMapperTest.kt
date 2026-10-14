package core.serverdriven.presentation.model.mapper

import core.serverdriven.data.model.response.ComponentResponse
import core.serverdriven.presentation.model.ComponentUiModel
import core.serverdriven.presentation.model.TextFieldKeyboardType
import core.serverdriven.presentation.model.TextFieldMaskType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ComponentUiMapperTest {

    private val componentUiMapper = ComponentUiMapper()

    @Test
    fun mapToUiModels_withKnownKeyboardTypeAndFormat_mapsBothIgnoringCase() {
        val textField = mapTextField(keyboardType = "numeric", format = "time")

        assertEquals(TextFieldKeyboardType.NUMERIC, textField.keyboardType)
        assertEquals(TextFieldMaskType.TIME, textField.maskType)
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
    fun mapToUiModels_withSlider_convertsValuesToFloat() {
        val slider = componentUiMapper.mapToUiModels(
            listOf(
                ComponentResponse.SliderResponse(
                    id = SLIDER_ID,
                    title = TITLE,
                    minValue = 1,
                    maxValue = 10,
                    currentValue = 3,
                    defaultValue = 5,
                    factoryValue = 6
                )
            )
        ).single() as ComponentUiModel.Slider

        assertEquals(listOf(1f, 10f, 3f, 5f, 6f), with(slider) {
            listOf(minValue, maxValue, currentValue, defaultValue, factoryValue)
        })
    }

    private fun mapTextField(keyboardType: String?, format: String?): ComponentUiModel.TextField {
        return componentUiMapper.mapToUiModels(
            listOf(
                ComponentResponse.TextFieldResponse(
                    id = TEXT_FIELD_ID,
                    title = TITLE,
                    currentValue = "",
                    defaultValue = "",
                    factoryValue = "",
                    format = format,
                    keyboardType = keyboardType
                )
            )
        ).single() as ComponentUiModel.TextField
    }

    private companion object {
        const val TEXT_FIELD_ID = "start_time_text_field"
        const val SLIDER_ID = "contrast_slider"
        const val TITLE = "Title"
    }
}
