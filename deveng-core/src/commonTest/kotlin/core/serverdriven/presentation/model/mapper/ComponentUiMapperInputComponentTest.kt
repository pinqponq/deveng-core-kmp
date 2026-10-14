package core.serverdriven.presentation.model.mapper

import core.serverdriven.data.model.response.ComponentResponse
import core.serverdriven.presentation.model.ComponentUiModel
import core.util.datetime.TargetDates
import kotlin.test.Test
import kotlin.test.assertEquals

class ComponentUiMapperInputComponentTest {

    private val componentUiMapper = ComponentUiMapper()

    @Test
    fun mapToUiModels_withLabel_keepsText() {
        val label = componentUiMapper.mapToUiModels(
            listOf(ComponentResponse.LabelResponse(id = LABEL_ID, text = TEXT))
        ).single()

        assertEquals(ComponentUiModel.Label(id = LABEL_ID, text = TEXT), label)
    }

    @Test
    fun mapToUiModels_withSegmentedAndRadio_keepsEveryFieldAndOptionOrder() {
        val components = componentUiMapper.mapToUiModels(
            listOf(
                ComponentResponse.SegmentedResponse(
                    id = SEGMENTED_ID,
                    title = TITLE,
                    description = DESCRIPTION,
                    currentValue = CURRENT_VALUE,
                    defaultValue = DEFAULT_VALUE,
                    factoryValue = FACTORY_VALUE,
                    options = OPTIONS
                ),
                ComponentResponse.RadioResponse(
                    id = RADIO_ID,
                    title = TITLE,
                    description = DESCRIPTION,
                    currentValue = CURRENT_VALUE,
                    defaultValue = DEFAULT_VALUE,
                    factoryValue = FACTORY_VALUE,
                    options = OPTIONS
                )
            )
        )

        assertEquals(
            listOf(
                ComponentUiModel.Segmented(
                    id = SEGMENTED_ID,
                    title = TITLE,
                    description = DESCRIPTION,
                    currentValue = CURRENT_VALUE,
                    defaultValue = DEFAULT_VALUE,
                    factoryValue = FACTORY_VALUE,
                    options = OPTIONS
                ),
                ComponentUiModel.Radio(
                    id = RADIO_ID,
                    title = TITLE,
                    description = DESCRIPTION,
                    currentValue = CURRENT_VALUE,
                    defaultValue = DEFAULT_VALUE,
                    factoryValue = FACTORY_VALUE,
                    options = OPTIONS
                )
            ),
            components
        )
    }

    @Test
    fun mapToUiModels_withRating_keepsEveryField() {
        val rating = componentUiMapper.mapToUiModels(
            listOf(
                ComponentResponse.RatingResponse(
                    id = RATING_ID,
                    title = TITLE,
                    description = DESCRIPTION,
                    maxValue = 10,
                    currentValue = 7f,
                    defaultValue = 5f,
                    factoryValue = 3f
                )
            )
        ).single()

        assertEquals(
            ComponentUiModel.Rating(
                id = RATING_ID,
                title = TITLE,
                description = DESCRIPTION,
                maxValue = 10,
                currentValue = 7f,
                defaultValue = 5f,
                factoryValue = 3f
            ),
            rating
        )
    }

    @Test
    fun mapToUiModels_withSection_keepsFieldsAndChildOrder() {
        val section = componentUiMapper.mapToUiModels(
            listOf(
                ComponentResponse.SectionResponse(
                    id = SECTION_ID,
                    title = TITLE,
                    description = DESCRIPTION,
                    components = listOf(
                        ComponentResponse.LabelResponse(id = LABEL_ID, text = TEXT),
                        ComponentResponse.UnknownResponse(id = UNKNOWN_ID, type = UNKNOWN_TYPE),
                        ComponentResponse.DividerResponse(id = DIVIDER_ID)
                    )
                )
            )
        ).single()

        assertEquals(
            ComponentUiModel.Section(
                id = SECTION_ID,
                title = TITLE,
                description = DESCRIPTION,
                components = listOf(
                    ComponentUiModel.Label(id = LABEL_ID, text = TEXT),
                    ComponentUiModel.Divider(id = DIVIDER_ID)
                )
            ),
            section
        )
    }

    @Test
    fun mapToUiModels_withDateAndKnownTargetDates_keepsEveryField() {
        val date = mapDate(currentValue = DATE_VALUE, targetDates = "future")

        assertEquals(
            ComponentUiModel.Date(
                id = DATE_ID,
                title = TITLE,
                description = DESCRIPTION,
                currentValue = DATE_VALUE,
                defaultValue = DEFAULT_VALUE,
                factoryValue = FACTORY_VALUE,
                targetDates = TargetDates.FUTURE
            ),
            date
        )
    }

    @Test
    fun mapToUiModels_withDateWithoutTargetDates_usesPast() {
        assertEquals(TargetDates.PAST, mapDate(currentValue = DATE_VALUE, targetDates = null).targetDates)
    }

    @Test
    fun mapToUiModels_withUnknownTargetDates_usesPast() {
        assertEquals(TargetDates.PAST, mapDate(currentValue = DATE_VALUE, targetDates = "anytime").targetDates)
    }

    @Test
    fun mapToUiModels_withEmptyDateValue_keepsNoDate() {
        assertEquals("", mapDate(currentValue = "", targetDates = null).currentValue)
    }

    @Test
    fun mapToUiModels_withDateValueThatIsNotIso_selectsNoDate() {
        assertEquals("", mapDate(currentValue = "15.10.2026", targetDates = null).currentValue)
    }

    private fun mapDate(currentValue: String, targetDates: String?): ComponentUiModel.Date {
        return componentUiMapper.mapToUiModels(
            listOf(
                ComponentResponse.DateResponse(
                    id = DATE_ID,
                    title = TITLE,
                    description = DESCRIPTION,
                    currentValue = currentValue,
                    defaultValue = DEFAULT_VALUE,
                    factoryValue = FACTORY_VALUE,
                    targetDates = targetDates
                )
            )
        ).single() as ComponentUiModel.Date
    }

    private companion object {
        const val LABEL_ID = "intro_label"
        const val SEGMENTED_ID = "mode_segmented"
        const val RADIO_ID = "quality_radio"
        const val RATING_ID = "service_rating"
        const val SECTION_ID = "schedule_section"
        const val DATE_ID = "start_date"
        const val DIVIDER_ID = "schedule_divider"
        const val UNKNOWN_ID = "usage_chart"
        const val UNKNOWN_TYPE = "chart"
        const val TEXT = "Settings apply after a restart"
        const val TITLE = "Title"
        const val DESCRIPTION = "Description"
        const val CURRENT_VALUE = "current"
        const val DEFAULT_VALUE = "default"
        const val FACTORY_VALUE = "factory"
        const val DATE_VALUE = "2026-10-15"
        val OPTIONS = listOf("Low", "High")
    }
}
