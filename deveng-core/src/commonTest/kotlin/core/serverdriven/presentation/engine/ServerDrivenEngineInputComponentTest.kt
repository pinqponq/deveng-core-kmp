package core.serverdriven.presentation.engine

import core.serverdriven.data.model.response.ComponentResponse
import core.serverdriven.data.model.response.LayoutResponse
import core.serverdriven.presentation.model.ComponentEvent
import core.serverdriven.presentation.model.ComponentProperty
import core.serverdriven.presentation.model.ComponentUiModel
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ServerDrivenEngineInputComponentTest {

    @Test
    fun setLayout_withEveryInputComponentType_collectsValuesOnlyForValueComponents() {
        val engine = ServerDrivenEngine()

        engine.setLayout(json = LAYOUT_JSON)

        assertEquals(
            mapOf(
                MODE_SEGMENTED_ID to DAY_MODE,
                START_DATE_ID to START_DATE,
                QUALITY_RADIO_ID to HIGH_QUALITY,
                SERVICE_RATING_ID to INITIAL_RATING.toFloat()
            ),
            engine.componentValues.value
        )
        assertEquals(
            listOf(INTRO_LABEL_ID, SCHEDULE_SECTION_ID, QUALITY_RADIO_ID, SERVICE_RATING_ID, THANKS_LABEL_ID),
            engine.components.value.map { component -> component.id }
        )
    }

    @Test
    fun setLayout_withEveryInputComponentType_givesEveryComponentInsideSectionAProperty() {
        val engine = ServerDrivenEngine()

        engine.setLayout(json = LAYOUT_JSON)

        assertEquals(
            setOf(
                INTRO_LABEL_ID,
                SCHEDULE_SECTION_ID,
                MODE_SEGMENTED_ID,
                START_DATE_ID,
                ADVANCED_SHUTTER_ID,
                QUALITY_RADIO_ID,
                SERVICE_RATING_ID,
                THANKS_LABEL_ID
            ),
            engine.componentProperties.value.keys
        )
    }

    @Test
    fun clickDropdownOption_onSegmentedWithVisibilityDependency_dropsTargetNestedInSection() = runTest {
        val engine = ServerDrivenEngine()
        engine.setLayout(json = LAYOUT_JSON)

        engine.handleEvent(ComponentEvent.ClickDropdownOption(componentId = MODE_SEGMENTED_ID, option = NIGHT_MODE))

        assertEquals(NIGHT_MODE, engine.componentValues.value[MODE_SEGMENTED_ID])
        assertEquals(listOf(MODE_SEGMENTED_ID, ADVANCED_SHUTTER_ID), visibleSectionChildIds(engine))
    }

    @Test
    fun setLayout_withSegmentedNotMatchingTrigger_keepsSectionChildrenVisible() {
        val engine = ServerDrivenEngine()

        engine.setLayout(json = LAYOUT_JSON)

        assertEquals(listOf(MODE_SEGMENTED_ID, START_DATE_ID, ADVANCED_SHUTTER_ID), visibleSectionChildIds(engine))
    }

    @Test
    fun changeSliderValue_onRatingAboveTrigger_showsGreaterThanTarget() = runTest {
        val engine = ServerDrivenEngine()
        engine.setLayout(json = LAYOUT_JSON)

        assertFalse(isVisible(engine, THANKS_LABEL_ID))

        engine.handleEvent(ComponentEvent.ChangeSliderValue(componentId = SERVICE_RATING_ID, value = 4f))

        assertEquals(4f, engine.componentValues.value[SERVICE_RATING_ID])
        assertTrue(isVisible(engine, THANKS_LABEL_ID))
    }

    @Test
    fun clickDropdownOption_onRadioWithEditableDependency_locksRatingAndIgnoresItsValue() = runTest {
        val engine = ServerDrivenEngine()
        engine.setLayout(json = LAYOUT_JSON)

        engine.handleEvent(ComponentEvent.ClickDropdownOption(componentId = QUALITY_RADIO_ID, option = LOW_QUALITY))
        engine.handleEvent(ComponentEvent.ChangeSliderValue(componentId = SERVICE_RATING_ID, value = 5f))

        assertEquals(LOW_QUALITY, engine.componentValues.value[QUALITY_RADIO_ID])
        assertFalse(engine.componentProperties.value.getValue(SERVICE_RATING_ID).isEditable)
        assertEquals(INITIAL_RATING.toFloat(), engine.componentValues.value[SERVICE_RATING_ID])
    }

    @Test
    fun enterTextField_onDateWithEqualsDependency_hidesTarget() = runTest {
        val engine = ServerDrivenEngine()
        engine.setLayout(json = LAYOUT_JSON)

        engine.handleEvent(ComponentEvent.EnterTextField(componentId = START_DATE_ID, value = LAST_DATE))

        assertEquals(LAST_DATE, engine.componentValues.value[START_DATE_ID])
        assertFalse(isVisible(engine, INTRO_LABEL_ID))
    }

    @Test
    fun clickShutter_nestedInSection_appendsLoadedContentInsideSection() = runTest {
        val engine = ServerDrivenEngine(
            shutterContentLoader = {
                LayoutResponse(components = listOf(ComponentResponse.LabelResponse(id = LOADED_LABEL_ID, text = TEXT)))
            }
        )
        engine.setLayout(json = LAYOUT_JSON)

        engine.handleEvent(ComponentEvent.ClickShutter(componentId = ADVANCED_SHUTTER_ID))

        val shutter = section(engine).components.last() as ComponentUiModel.Shutter
        assertEquals(listOf(LOADED_LABEL_ID), shutter.components.map { component -> component.id })
        assertEquals(ComponentProperty(isExpanded = true), engine.componentProperties.value[ADVANCED_SHUTTER_ID])
        assertEquals(ComponentProperty(), engine.componentProperties.value[LOADED_LABEL_ID])
    }

    @Test
    fun setLayout_withDateWithoutValue_collectsNoValueForIt() {
        val engine = ServerDrivenEngine()

        engine.setLayout(
            layout = LayoutResponse(
                components = listOf(
                    ComponentResponse.DateResponse(
                        id = START_DATE_ID,
                        title = TITLE,
                        currentValue = "",
                        defaultValue = "",
                        factoryValue = ""
                    )
                )
            )
        )

        assertEquals(emptyMap(), engine.componentValues.value)
    }

    @Test
    fun setLayout_withSectionChildIdUsedOutsideSection_fails() {
        val engine = ServerDrivenEngine()

        assertFailsWith<IllegalArgumentException> {
            engine.setLayout(
                layout = LayoutResponse(
                    components = listOf(
                        ComponentResponse.LabelResponse(id = INTRO_LABEL_ID, text = TEXT),
                        ComponentResponse.SectionResponse(
                            id = SCHEDULE_SECTION_ID,
                            title = TITLE,
                            components = listOf(ComponentResponse.LabelResponse(id = INTRO_LABEL_ID, text = TEXT))
                        )
                    )
                )
            )
        }
    }

    @Test
    fun setLayout_withSectionInTab_keysSectionChildrenByTab() {
        val engine = ServerDrivenEngine()

        engine.setLayout(
            json = """
                {"components":[{"type":"tabs","id":"$TABS_ID","tabs":[{"id":"$DAY_TAB_ID","title":"Day","components":[
                  {"type":"section","id":"$SCHEDULE_SECTION_ID","title":"$TITLE","components":[$SEGMENTED_JSON]}
                ]}]}]}
            """
        )

        assertEquals(
            mapOf(TABS_ID to DAY_TAB_ID, "$DAY_TAB_ID/$MODE_SEGMENTED_ID" to DAY_MODE),
            engine.componentValues.value
        )
    }

    private fun section(engine: ServerDrivenEngine): ComponentUiModel.Section =
        engine.components.value.filterIsInstance<ComponentUiModel.Section>().single()

    private fun visibleSectionChildIds(engine: ServerDrivenEngine): List<String> =
        engine.visibleComponents.value
            .filterIsInstance<ComponentUiModel.Section>()
            .single()
            .components
            .map { component -> component.id }

    private fun isVisible(engine: ServerDrivenEngine, componentId: String): Boolean =
        engine.componentProperties.value.getValue(componentId).isVisible

    private companion object {
        const val INTRO_LABEL_ID = "intro_label"
        const val THANKS_LABEL_ID = "thanks_label"
        const val LOADED_LABEL_ID = "loaded_label"
        const val SCHEDULE_SECTION_ID = "schedule_section"
        const val MODE_SEGMENTED_ID = "mode_segmented"
        const val START_DATE_ID = "start_date"
        const val ADVANCED_SHUTTER_ID = "advanced_shutter"
        const val QUALITY_RADIO_ID = "quality_radio"
        const val SERVICE_RATING_ID = "service_rating"
        const val TABS_ID = "mode_tabs"
        const val DAY_TAB_ID = "day_tab"
        const val TITLE = "Title"
        const val TEXT = "Settings apply after a restart"
        const val DAY_MODE = "Day"
        const val NIGHT_MODE = "Night"
        const val HIGH_QUALITY = "High"
        const val LOW_QUALITY = "Low"
        const val START_DATE = "2026-10-15"
        const val LAST_DATE = "2026-12-31"
        const val INITIAL_RATING = 3

        const val SEGMENTED_JSON = """
            {
              "type": "segmented", "id": "$MODE_SEGMENTED_ID", "title": "Mode",
              "currentValue": "$DAY_MODE", "defaultValue": "$DAY_MODE", "factoryValue": "$DAY_MODE",
              "options": ["$DAY_MODE", "$NIGHT_MODE"]
            }
        """

        const val LAYOUT_JSON = """
            {
              "components": [
                { "type": "label", "id": "$INTRO_LABEL_ID", "text": "$TEXT" },
                {
                  "type": "section", "id": "$SCHEDULE_SECTION_ID", "title": "Schedule",
                  "components": [
                    $SEGMENTED_JSON,
                    {
                      "type": "date", "id": "$START_DATE_ID", "title": "Start",
                      "currentValue": "$START_DATE", "defaultValue": "", "factoryValue": ""
                    },
                    { "type": "shutter", "id": "$ADVANCED_SHUTTER_ID", "title": "Advanced" }
                  ]
                },
                {
                  "type": "radio", "id": "$QUALITY_RADIO_ID", "title": "Quality",
                  "currentValue": "$HIGH_QUALITY", "defaultValue": "$HIGH_QUALITY", "factoryValue": "$LOW_QUALITY",
                  "options": ["$LOW_QUALITY", "$HIGH_QUALITY"]
                },
                {
                  "type": "rating", "id": "$SERVICE_RATING_ID", "title": "Service",
                  "currentValue": $INITIAL_RATING, "defaultValue": 0, "factoryValue": 0
                },
                { "type": "label", "id": "$THANKS_LABEL_ID", "text": "Thanks" }
              ],
              "dependencies": [
                {
                  "sourceId": "$MODE_SEGMENTED_ID", "targetId": "$START_DATE_ID", "operator": "EQUALS",
                  "triggerValue": "$NIGHT_MODE", "effectType": "VISIBILITY", "effectValue": false
                },
                {
                  "sourceId": "$SERVICE_RATING_ID", "targetId": "$THANKS_LABEL_ID", "operator": "GREATER_THAN",
                  "triggerValue": "3", "effectType": "VISIBILITY", "effectValue": true
                },
                {
                  "sourceId": "$QUALITY_RADIO_ID", "targetId": "$SERVICE_RATING_ID", "operator": "EQUALS",
                  "triggerValue": "$LOW_QUALITY", "effectType": "EDITABLE", "effectValue": false
                },
                {
                  "sourceId": "$START_DATE_ID", "targetId": "$INTRO_LABEL_ID", "operator": "EQUALS",
                  "triggerValue": "$LAST_DATE", "effectType": "VISIBILITY", "effectValue": false
                }
              ]
            }
        """
    }
}
