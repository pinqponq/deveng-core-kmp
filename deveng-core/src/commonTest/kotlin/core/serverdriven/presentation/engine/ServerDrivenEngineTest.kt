package core.serverdriven.presentation.engine

import core.serverdriven.data.model.response.ComponentResponse
import core.serverdriven.data.model.response.DependencyResponse
import core.serverdriven.data.model.response.LayoutResponse
import core.serverdriven.data.model.response.TabResponse
import core.serverdriven.presentation.model.ComponentEvent
import core.serverdriven.presentation.model.ComponentUiModel
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ServerDrivenEngineTest {

    @Test
    fun setLayout_withJsonOfEveryComponentType_buildsTreeAndInitialValues() {
        val engine = ServerDrivenEngine()

        engine.setLayout(json = LAYOUT_JSON)

        val componentTypes = engine.components.value.map { component -> component::class }
        assertEquals(
            listOf(
                ComponentUiModel.Shutter::class,
                ComponentUiModel.Divider::class,
                ComponentUiModel.Tabs::class
            ),
            componentTypes
        )
        assertEquals(
            mapOf(
                NAME_TEXT_FIELD_ID to "Gate",
                RECORD_SWITCH_ID to true,
                RESOLUTION_DROPDOWN_ID to "1080p",
                MAIN_TABS_ID to IMAGE_TAB_ID,
                BRIGHTNESS_SLIDER_ID to 85f
            ),
            engine.componentValues.value
        )
    }

    @Test
    fun setLayout_withUnknownComponentType_skipsOnlyThatComponent() {
        val engine = ServerDrivenEngine()

        engine.setLayout(json = """{"components":[{"type":"chart","id":"$CHART_ID"},$SWITCH_JSON]}""")

        assertEquals(listOf(RECORD_SWITCH_ID), engine.components.value.map { component -> component.id })
    }

    @Test
    fun setLayout_withUnknownOperator_skipsThatDependency() {
        val engine = ServerDrivenEngine()

        engine.setLayout(
            layout = switchAndSliderLayout(
                dependency = visibilityDependency(operator = "CONTAINS", triggerValue = "true", effectValue = false)
            )
        )

        assertTrue(isVisible(engine, BRIGHTNESS_SLIDER_ID))
    }

    @Test
    fun setLayout_withUnknownEffect_skipsThatDependency() {
        val engine = ServerDrivenEngine()

        engine.setLayout(
            layout = switchAndSliderLayout(
                dependency = visibilityDependency(triggerValue = "true", effectValue = false)
                    .copy(effectType = "COLOR")
            )
        )

        assertTrue(isVisible(engine, BRIGHTNESS_SLIDER_ID))
        assertTrue(isEditable(engine, BRIGHTNESS_SLIDER_ID))
    }

    @Test
    fun setLayout_withMatchingInitialValue_appliesDependencyBeforeAnyEvent() {
        val engine = ServerDrivenEngine()

        engine.setLayout(
            layout = switchAndSliderLayout(
                dependency = visibilityDependency(triggerValue = "true", effectValue = false)
            )
        )

        assertFalse(isVisible(engine, BRIGHTNESS_SLIDER_ID))
    }

    @Test
    fun setLayout_withDropdownSourceInitialValue_appliesEditableEffect() {
        val engine = ServerDrivenEngine()

        engine.setLayout(
            layout = LayoutResponse(
                components = listOf(dropdownResponse(), switchResponse()),
                dependencies = listOf(
                    DependencyResponse(
                        sourceId = RESOLUTION_DROPDOWN_ID,
                        targetId = RECORD_SWITCH_ID,
                        operator = "EQUALS",
                        triggerValue = "1080p",
                        effectType = "EDITABLE",
                        effectValue = false
                    )
                )
            )
        )

        assertFalse(isEditable(engine, RECORD_SWITCH_ID))
    }

    @Test
    fun setLayout_withDuplicateIdsInTwoTabs_failsNamingTheId() {
        val engine = ServerDrivenEngine()
        val layout = LayoutResponse(
            components = listOf(
                ComponentResponse.TabsResponse(
                    id = MAIN_TABS_ID,
                    tabs = listOf(
                        TabResponse(id = IMAGE_TAB_ID, title = TAB_TITLE, components = listOf(sliderResponse())),
                        TabResponse(id = NETWORK_TAB_ID, title = TAB_TITLE, components = listOf(sliderResponse()))
                    )
                )
            )
        )

        val exception = assertFailsWith<IllegalArgumentException> { engine.setLayout(layout = layout) }

        assertTrue(exception.message.orEmpty().contains(BRIGHTNESS_SLIDER_ID))
    }

    @Test
    fun clickSwitch_withEqualsMatch_hidesTarget() = runTest {
        val engine = ServerDrivenEngine()
        engine.setLayout(
            layout = switchAndSliderLayout(
                dependency = visibilityDependency(triggerValue = "false", effectValue = false)
            )
        )

        engine.handleEvent(ComponentEvent.ClickSwitch(componentId = RECORD_SWITCH_ID, isChecked = false))

        assertFalse(isVisible(engine, BRIGHTNESS_SLIDER_ID))
        assertEquals(false, engine.componentValues.value[RECORD_SWITCH_ID])
    }

    @Test
    fun clickSwitch_withEqualsNoLongerMatching_showsTargetAgain() = runTest {
        val engine = ServerDrivenEngine()
        engine.setLayout(
            layout = switchAndSliderLayout(
                dependency = visibilityDependency(triggerValue = "true", effectValue = false)
            )
        )

        engine.handleEvent(ComponentEvent.ClickSwitch(componentId = RECORD_SWITCH_ID, isChecked = false))

        assertTrue(isVisible(engine, BRIGHTNESS_SLIDER_ID))
    }

    @Test
    fun changeSliderValue_aboveGreaterThanTrigger_makesTargetNotEditable() = runTest {
        val engine = ServerDrivenEngine()
        engine.setLayout(layout = sliderSourceLayout(operator = "GREATER_THAN", triggerValue = "90"))
        assertTrue(isEditable(engine, RECORD_SWITCH_ID))

        engine.handleEvent(ComponentEvent.ChangeSliderValue(componentId = BRIGHTNESS_SLIDER_ID, value = 91f))

        assertFalse(isEditable(engine, RECORD_SWITCH_ID))
    }

    @Test
    fun changeSliderValue_equalToGreaterThanTrigger_keepsTargetEditable() = runTest {
        val engine = ServerDrivenEngine()
        engine.setLayout(layout = sliderSourceLayout(operator = "GREATER_THAN", triggerValue = "90"))

        engine.handleEvent(ComponentEvent.ChangeSliderValue(componentId = BRIGHTNESS_SLIDER_ID, value = 90f))

        assertTrue(isEditable(engine, RECORD_SWITCH_ID))
    }

    @Test
    fun changeSliderValue_belowLessThanTrigger_makesTargetNotEditable() = runTest {
        val engine = ServerDrivenEngine()
        engine.setLayout(layout = sliderSourceLayout(operator = "LESS_THAN", triggerValue = "20"))
        assertTrue(isEditable(engine, RECORD_SWITCH_ID))

        engine.handleEvent(ComponentEvent.ChangeSliderValue(componentId = BRIGHTNESS_SLIDER_ID, value = 19f))

        assertFalse(isEditable(engine, RECORD_SWITCH_ID))
    }

    @Test
    fun changeSliderValue_equalToLessThanTrigger_keepsTargetEditable() = runTest {
        val engine = ServerDrivenEngine()
        engine.setLayout(layout = sliderSourceLayout(operator = "LESS_THAN", triggerValue = "20"))

        engine.handleEvent(ComponentEvent.ChangeSliderValue(componentId = BRIGHTNESS_SLIDER_ID, value = 20f))

        assertTrue(isEditable(engine, RECORD_SWITCH_ID))
    }

    @Test
    fun changeSliderValue_withNumericEqualsTrigger_matchesWholeNumberText() = runTest {
        val engine = ServerDrivenEngine()
        engine.setLayout(layout = sliderSourceLayout(operator = "EQUALS", triggerValue = "50"))

        engine.handleEvent(ComponentEvent.ChangeSliderValue(componentId = BRIGHTNESS_SLIDER_ID, value = 50f))

        assertFalse(isEditable(engine, RECORD_SWITCH_ID))
    }

    @Test
    fun changeSliderValue_withNonNumericComparisonTrigger_neverMatches() = runTest {
        val engine = ServerDrivenEngine()
        engine.setLayout(layout = sliderSourceLayout(operator = "GREATER_THAN", triggerValue = "high"))

        engine.handleEvent(ComponentEvent.ChangeSliderValue(componentId = BRIGHTNESS_SLIDER_ID, value = 99f))

        assertTrue(isEditable(engine, RECORD_SWITCH_ID))
    }

    @Test
    fun clickDropdownOption_onNotEditableDropdown_keepsValue() = runTest {
        val engine = ServerDrivenEngine()
        engine.setLayout(
            layout = LayoutResponse(
                components = listOf(switchResponse(), dropdownResponse()),
                dependencies = listOf(
                    DependencyResponse(
                        sourceId = RECORD_SWITCH_ID,
                        targetId = RESOLUTION_DROPDOWN_ID,
                        operator = "EQUALS",
                        triggerValue = "true",
                        effectType = "EDITABLE",
                        effectValue = false
                    )
                )
            )
        )

        engine.handleEvent(ComponentEvent.ClickDropdownOption(componentId = RESOLUTION_DROPDOWN_ID, option = "720p"))

        assertEquals("1080p", engine.componentValues.value[RESOLUTION_DROPDOWN_ID])
    }

    @Test
    fun enterTextField_onEditableField_storesValue() = runTest {
        val engine = ServerDrivenEngine()
        engine.setLayout(json = LAYOUT_JSON)

        engine.handleEvent(ComponentEvent.EnterTextField(componentId = NAME_TEXT_FIELD_ID, value = "Parking"))

        assertEquals("Parking", engine.componentValues.value[NAME_TEXT_FIELD_ID])
    }

    @Test
    fun clickTabItem_withTabOfComponent_selectsTabAndTriggersDependency() = runTest {
        val engine = ServerDrivenEngine()
        engine.setLayout(json = LAYOUT_JSON)
        assertTrue(isVisible(engine, RECORD_SWITCH_ID))

        engine.handleEvent(ComponentEvent.ClickTabItem(componentId = MAIN_TABS_ID, tabId = NETWORK_TAB_ID))

        assertEquals(NETWORK_TAB_ID, engine.componentValues.value[MAIN_TABS_ID])
        assertFalse(isVisible(engine, RECORD_SWITCH_ID))
    }

    @Test
    fun clickTabItem_withTabOfAnotherComponent_fails() = runTest {
        val engine = ServerDrivenEngine()
        engine.setLayout(json = LAYOUT_JSON)

        assertFailsWith<IllegalArgumentException> {
            engine.handleEvent(ComponentEvent.ClickTabItem(componentId = MAIN_TABS_ID, tabId = CHART_ID))
        }
    }

    @Test
    fun clickShutter_withoutLoader_togglesExpanded() = runTest {
        val engine = ServerDrivenEngine()
        engine.setLayout(json = LAYOUT_JSON)
        assertFalse(isExpanded(engine, CAMERA_SHUTTER_ID))

        engine.handleEvent(ComponentEvent.ClickShutter(componentId = CAMERA_SHUTTER_ID))
        assertTrue(isExpanded(engine, CAMERA_SHUTTER_ID))

        engine.handleEvent(ComponentEvent.ClickShutter(componentId = CAMERA_SHUTTER_ID))
        assertFalse(isExpanded(engine, CAMERA_SHUTTER_ID))
    }

    @Test
    fun clickShutter_onComponentThatIsNotShutter_fails() = runTest {
        val engine = ServerDrivenEngine()
        engine.setLayout(json = LAYOUT_JSON)

        assertFailsWith<IllegalArgumentException> {
            engine.handleEvent(ComponentEvent.ClickShutter(componentId = RECORD_SWITCH_ID))
        }
    }

    @Test
    fun clickShutter_firstExpand_appendsLoadedContentAndAppliesItsDependencies() = runTest {
        val loader = RecordingShutterContentLoader(content = lazyShutterContent())
        val engine = ServerDrivenEngine(shutterContentLoader = loader)
        engine.setLayout(json = LAYOUT_JSON)

        engine.handleEvent(ComponentEvent.ClickShutter(componentId = CAMERA_SHUTTER_ID))

        val shutter = engine.components.value.first() as ComponentUiModel.Shutter
        assertEquals(
            listOf(NAME_TEXT_FIELD_ID, ROW_ID, LAZY_SWITCH_ID),
            shutter.components.map { component -> component.id }
        )
        assertEquals(listOf(CAMERA_SHUTTER_ID), loader.requestedShutterIds)
        assertEquals(true, engine.componentValues.value[LAZY_SWITCH_ID])
        assertFalse(isEditable(engine, NAME_TEXT_FIELD_ID))
    }

    @Test
    fun clickShutter_collapseAndExpandAgain_loadsContentOnce() = runTest {
        val loader = RecordingShutterContentLoader(content = lazyShutterContent())
        val engine = ServerDrivenEngine(shutterContentLoader = loader)
        engine.setLayout(json = LAYOUT_JSON)

        repeat(times = 3) { engine.handleEvent(ComponentEvent.ClickShutter(componentId = CAMERA_SHUTTER_ID)) }

        assertEquals(listOf(CAMERA_SHUTTER_ID), loader.requestedShutterIds)
        assertTrue(isExpanded(engine, CAMERA_SHUTTER_ID))
    }

    @Test
    fun clickShutter_whenLoaderFails_collapsesAndRetriesOnNextExpand() = runTest {
        val loader = RecordingShutterContentLoader(content = lazyShutterContent(), failureCount = 1)
        val engine = ServerDrivenEngine(shutterContentLoader = loader)
        engine.setLayout(json = LAYOUT_JSON)

        assertFailsWith<IllegalStateException> {
            engine.handleEvent(ComponentEvent.ClickShutter(componentId = CAMERA_SHUTTER_ID))
        }
        assertFalse(isExpanded(engine, CAMERA_SHUTTER_ID))
        engine.handleEvent(ComponentEvent.ClickShutter(componentId = CAMERA_SHUTTER_ID))

        assertEquals(listOf(CAMERA_SHUTTER_ID, CAMERA_SHUTTER_ID), loader.requestedShutterIds)
        assertEquals(true, engine.componentValues.value[LAZY_SWITCH_ID])
    }

    @Test
    fun clickShutter_withLoadedIdAlreadyInLayout_fails() = runTest {
        val loader = RecordingShutterContentLoader(content = LayoutResponse(components = listOf(switchResponse())))
        val engine = ServerDrivenEngine(shutterContentLoader = loader)
        engine.setLayout(json = LAYOUT_JSON)

        assertFailsWith<IllegalArgumentException> {
            engine.handleEvent(ComponentEvent.ClickShutter(componentId = CAMERA_SHUTTER_ID))
        }
    }

    @Test
    fun clickShutter_nestedInTab_appendsLoadedContent() = runTest {
        val loader = RecordingShutterContentLoader(content = LayoutResponse(components = listOf(switchResponse())))
        val engine = ServerDrivenEngine(shutterContentLoader = loader)
        engine.setLayout(
            layout = LayoutResponse(
                components = listOf(
                    ComponentResponse.TabsResponse(
                        id = MAIN_TABS_ID,
                        tabs = listOf(
                            TabResponse(
                                id = IMAGE_TAB_ID,
                                title = TAB_TITLE,
                                components = listOf(
                                    ComponentResponse.RowResponse(
                                        id = ROW_ID,
                                        components = listOf(
                                            ComponentResponse.ShutterResponse(id = CAMERA_SHUTTER_ID, title = TAB_TITLE)
                                        )
                                    )
                                )
                            )
                        )
                    )
                )
            )
        )

        engine.handleEvent(ComponentEvent.ClickShutter(componentId = CAMERA_SHUTTER_ID))

        val tabs = engine.components.value.single() as ComponentUiModel.Tabs
        val row = tabs.tabs.single().components.single() as ComponentUiModel.Row
        val shutter = row.components.single() as ComponentUiModel.Shutter
        assertEquals(listOf(RECORD_SWITCH_ID), shutter.components.map { component -> component.id })
    }

    @Test
    fun setLayout_afterShutterWasLoaded_loadsShutterContentAgain() = runTest {
        val loader = RecordingShutterContentLoader(content = lazyShutterContent())
        val engine = ServerDrivenEngine(shutterContentLoader = loader)
        engine.setLayout(json = LAYOUT_JSON)
        engine.handleEvent(ComponentEvent.ClickShutter(componentId = CAMERA_SHUTTER_ID))

        engine.setLayout(json = LAYOUT_JSON)
        engine.handleEvent(ComponentEvent.ClickShutter(componentId = CAMERA_SHUTTER_ID))

        assertEquals(listOf(CAMERA_SHUTTER_ID, CAMERA_SHUTTER_ID), loader.requestedShutterIds)
    }

    private fun isVisible(engine: ServerDrivenEngine, componentId: String): Boolean =
        engine.componentProperties.value.getValue(componentId).isVisible

    private fun isEditable(engine: ServerDrivenEngine, componentId: String): Boolean =
        engine.componentProperties.value.getValue(componentId).isEditable

    private fun isExpanded(engine: ServerDrivenEngine, componentId: String): Boolean =
        engine.componentProperties.value.getValue(componentId).isExpanded

    private fun switchAndSliderLayout(dependency: DependencyResponse) = LayoutResponse(
        components = listOf(switchResponse(), sliderResponse()),
        dependencies = listOf(dependency)
    )

    private fun sliderSourceLayout(operator: String, triggerValue: String) = LayoutResponse(
        components = listOf(sliderResponse(currentValue = 50), switchResponse()),
        dependencies = listOf(
            DependencyResponse(
                sourceId = BRIGHTNESS_SLIDER_ID,
                targetId = RECORD_SWITCH_ID,
                operator = operator,
                triggerValue = triggerValue,
                effectType = "EDITABLE",
                effectValue = false
            )
        )
    )

    private fun visibilityDependency(
        operator: String = "EQUALS",
        triggerValue: String,
        effectValue: Boolean
    ) = DependencyResponse(
        sourceId = RECORD_SWITCH_ID,
        targetId = BRIGHTNESS_SLIDER_ID,
        operator = operator,
        triggerValue = triggerValue,
        effectType = "VISIBILITY",
        effectValue = effectValue
    )

    private fun lazyShutterContent() = LayoutResponse(
        components = listOf(
            ComponentResponse.SwitchResponse(id = LAZY_SWITCH_ID, title = TAB_TITLE, currentValue = true)
        ),
        dependencies = listOf(
            DependencyResponse(
                sourceId = LAZY_SWITCH_ID,
                targetId = NAME_TEXT_FIELD_ID,
                operator = "EQUALS",
                triggerValue = "true",
                effectType = "EDITABLE",
                effectValue = false
            )
        )
    )

    private fun switchResponse() = ComponentResponse.SwitchResponse(
        id = RECORD_SWITCH_ID,
        title = TAB_TITLE,
        currentValue = true
    )

    private fun sliderResponse(currentValue: Int = 85) = ComponentResponse.SliderResponse(
        id = BRIGHTNESS_SLIDER_ID,
        title = TAB_TITLE,
        minValue = 0,
        maxValue = 100,
        currentValue = currentValue,
        defaultValue = 50,
        factoryValue = 50
    )

    private fun dropdownResponse() = ComponentResponse.DropdownResponse(
        id = RESOLUTION_DROPDOWN_ID,
        title = TAB_TITLE,
        currentValue = "1080p",
        defaultValue = "1080p",
        factoryValue = "720p",
        options = listOf("720p", "1080p")
    )

    private class RecordingShutterContentLoader(
        private val content: LayoutResponse,
        private var failureCount: Int = 0
    ) : ShutterContentLoader {
        val requestedShutterIds = mutableListOf<String>()

        override suspend fun loadShutterContent(shutterId: String): LayoutResponse {
            requestedShutterIds += shutterId
            if (failureCount > 0) {
                failureCount--
                throw IllegalStateException(LOADER_FAILURE_MESSAGE)
            }
            return content
        }
    }

    private companion object {
        const val CAMERA_SHUTTER_ID = "camera_shutter"
        const val NAME_TEXT_FIELD_ID = "camera_name_text_field"
        const val ROW_ID = "record_row"
        const val RECORD_SWITCH_ID = "record_switch"
        const val RESOLUTION_DROPDOWN_ID = "resolution_dropdown"
        const val MAIN_TABS_ID = "main_tabs"
        const val IMAGE_TAB_ID = "image_tab"
        const val NETWORK_TAB_ID = "network_tab"
        const val BRIGHTNESS_SLIDER_ID = "brightness_slider"
        const val LAZY_SWITCH_ID = "lazy_switch"
        const val CHART_ID = "usage_chart"
        const val TAB_TITLE = "Title"
        const val LOADER_FAILURE_MESSAGE = "shutter content unavailable"

        const val SWITCH_JSON =
            """{"type":"switch","id":"$RECORD_SWITCH_ID","title":"Record","currentValue":true}"""

        const val LAYOUT_JSON = """
            {
              "components": [
                {
                  "type": "shutter", "id": "$CAMERA_SHUTTER_ID", "title": "Camera", "description": "Basics",
                  "components": [
                    {
                      "type": "textfield", "id": "$NAME_TEXT_FIELD_ID", "title": "Name", "currentValue": "Gate",
                      "defaultValue": "", "factoryValue": "", "hintMessage": "Name", "keyboardType": "TEXT"
                    },
                    {
                      "type": "row", "id": "$ROW_ID",
                      "components": [
                        $SWITCH_JSON,
                        {
                          "type": "dropdown", "id": "$RESOLUTION_DROPDOWN_ID", "title": "Resolution",
                          "currentValue": "1080p", "defaultValue": "1080p", "factoryValue": "720p",
                          "options": ["720p", "1080p"]
                        }
                      ]
                    }
                  ]
                },
                { "type": "divider", "id": "camera_divider" },
                {
                  "type": "tabs", "id": "$MAIN_TABS_ID",
                  "tabs": [
                    {
                      "type": "tab", "id": "$IMAGE_TAB_ID", "title": "Image",
                      "components": [
                        {
                          "type": "slider", "id": "$BRIGHTNESS_SLIDER_ID", "title": "Brightness",
                          "minValue": 0, "maxValue": 100, "currentValue": 85, "defaultValue": 50, "factoryValue": 50
                        }
                      ]
                    },
                    { "type": "tab", "id": "$NETWORK_TAB_ID", "title": "Network", "components": [] }
                  ]
                }
              ],
              "dependencies": [
                {
                  "sourceId": "$MAIN_TABS_ID", "targetId": "$RECORD_SWITCH_ID", "operator": "EQUALS",
                  "triggerValue": "$NETWORK_TAB_ID", "effectType": "VISIBILITY", "effectValue": false
                }
              ]
            }
        """
    }
}
