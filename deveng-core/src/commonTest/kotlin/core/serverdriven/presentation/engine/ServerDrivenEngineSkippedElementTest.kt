package core.serverdriven.presentation.engine

import core.serverdriven.data.model.response.ComponentResponse
import core.serverdriven.data.model.response.DependencyResponse
import core.serverdriven.data.model.response.LayoutResponse
import core.serverdriven.data.model.response.TabResponse
import core.serverdriven.presentation.model.ComponentEvent
import core.serverdriven.presentation.model.SkippedElementKind
import core.serverdriven.presentation.model.SkippedLayoutElement
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ServerDrivenEngineSkippedElementTest {

    private val skippedElements = mutableListOf<SkippedLayoutElement>()

    @Test
    fun setLayout_withUnknownComponentType_reportsTypeAndId() {
        val engine = ServerDrivenEngine(onLayoutElementSkip = { skippedElement -> skippedElements += skippedElement })

        engine.setLayout(json = """{"components":[{"type":"$CHART_TYPE","id":"$CHART_ID"},$SWITCH_JSON]}""")

        assertEquals(listOf(chartElement()), skippedElements)
    }

    @Test
    fun setLayout_withComponentWithoutType_reportsItWithoutName() {
        val engine = ServerDrivenEngine(onLayoutElementSkip = { skippedElement -> skippedElements += skippedElement })

        engine.setLayout(json = """{"components":[{"id":"$CHART_ID"}]}""")

        assertEquals(
            listOf(SkippedLayoutElement(kind = SkippedElementKind.COMPONENT_TYPE, name = null, componentId = CHART_ID)),
            skippedElements
        )
    }

    @Test
    fun setLayout_withUnknownOperator_reportsOperatorAndTargetId() {
        val engine = ServerDrivenEngine(onLayoutElementSkip = { skippedElement -> skippedElements += skippedElement })

        engine.setLayout(layout = switchLayout(dependency(operator = UNKNOWN_OPERATOR, effectType = KNOWN_EFFECT)))

        assertEquals(listOf(operatorElement()), skippedElements)
    }

    @Test
    fun setLayout_withUnknownEffect_reportsEffectAndTargetId() {
        val engine = ServerDrivenEngine(onLayoutElementSkip = { skippedElement -> skippedElements += skippedElement })

        engine.setLayout(layout = switchLayout(dependency(operator = KNOWN_OPERATOR, effectType = UNKNOWN_EFFECT)))

        assertEquals(listOf(effectElement()), skippedElements)
    }

    @Test
    fun setLayout_withUnknownOperatorAndEffect_reportsBoth() {
        val engine = ServerDrivenEngine(onLayoutElementSkip = { skippedElement -> skippedElements += skippedElement })

        engine.setLayout(layout = switchLayout(dependency(operator = UNKNOWN_OPERATOR, effectType = UNKNOWN_EFFECT)))

        assertEquals(listOf(operatorElement(), effectElement()), skippedElements)
    }

    @Test
    fun setLayout_withSameUnknownElementsRepeated_reportsEachOnce() {
        val engine = ServerDrivenEngine(onLayoutElementSkip = { skippedElement -> skippedElements += skippedElement })
        val unknownOperatorDependency = dependency(operator = UNKNOWN_OPERATOR, effectType = KNOWN_EFFECT)

        engine.setLayout(
            layout = LayoutResponse(
                components = listOf(
                    ComponentResponse.TabsResponse(
                        id = TABS_ID,
                        tabs = listOf(
                            TabResponse(id = DAY_TAB_ID, title = TITLE, components = listOf(chartResponse())),
                            TabResponse(id = NIGHT_TAB_ID, title = TITLE, components = listOf(chartResponse()))
                        )
                    ),
                    switchResponse()
                ),
                dependencies = listOf(unknownOperatorDependency, unknownOperatorDependency)
            )
        )

        assertEquals(listOf(chartElement(), operatorElement()), skippedElements)
    }

    @Test
    fun setLayout_again_reportsSkippedElementsOfNewLayout() {
        val engine = ServerDrivenEngine(onLayoutElementSkip = { skippedElement -> skippedElements += skippedElement })
        val layout = LayoutResponse(components = listOf(chartResponse()))

        engine.setLayout(layout = layout)
        engine.setLayout(layout = layout)

        assertEquals(listOf(chartElement(), chartElement()), skippedElements)
    }

    @Test
    fun clickShutter_withUnknownElementsInLoadedContent_reportsOnlyNewOnes() = runTest {
        val engine = ServerDrivenEngine(
            shutterContentLoader = {
                LayoutResponse(
                    components = listOf(chartResponse(), ComponentResponse.UnknownResponse(id = MAP_ID, type = MAP_TYPE))
                )
            },
            onLayoutElementSkip = { skippedElement -> skippedElements += skippedElement }
        )
        engine.setLayout(
            layout = LayoutResponse(
                components = listOf(
                    chartResponse(),
                    ComponentResponse.ShutterResponse(id = SHUTTER_ID, title = TITLE)
                )
            )
        )

        engine.handleEvent(ComponentEvent.ClickShutter(componentId = SHUTTER_ID))

        assertEquals(
            listOf(
                chartElement(),
                SkippedLayoutElement(kind = SkippedElementKind.COMPONENT_TYPE, name = MAP_TYPE, componentId = MAP_ID)
            ),
            skippedElements
        )
    }

    @Test
    fun setLayout_withKnownElementsOnly_reportsNothing() {
        val engine = ServerDrivenEngine(onLayoutElementSkip = { skippedElement -> skippedElements += skippedElement })

        engine.setLayout(layout = switchLayout(dependency(operator = KNOWN_OPERATOR, effectType = KNOWN_EFFECT)))

        assertEquals(emptyList(), skippedElements)
    }

    private fun switchLayout(dependency: DependencyResponse) = LayoutResponse(
        components = listOf(switchResponse()),
        dependencies = listOf(dependency)
    )

    private fun switchResponse() = ComponentResponse.SwitchResponse(id = SWITCH_ID, title = TITLE, currentValue = true)

    private fun chartResponse() = ComponentResponse.UnknownResponse(id = CHART_ID, type = CHART_TYPE)

    private fun dependency(
        operator: String,
        effectType: String
    ) = DependencyResponse(
        sourceId = SWITCH_ID,
        targetId = TARGET_ID,
        operator = operator,
        triggerValue = "true",
        effectType = effectType,
        effectValue = false
    )

    private fun chartElement() =
        SkippedLayoutElement(kind = SkippedElementKind.COMPONENT_TYPE, name = CHART_TYPE, componentId = CHART_ID)

    private fun operatorElement() =
        SkippedLayoutElement(kind = SkippedElementKind.OPERATOR, name = UNKNOWN_OPERATOR, componentId = TARGET_ID)

    private fun effectElement() =
        SkippedLayoutElement(kind = SkippedElementKind.EFFECT, name = UNKNOWN_EFFECT, componentId = TARGET_ID)

    private companion object {
        const val SWITCH_ID = "record_switch"
        const val TARGET_ID = "resolution_dropdown"
        const val CHART_ID = "usage_chart"
        const val CHART_TYPE = "chart"
        const val MAP_ID = "coverage_map"
        const val MAP_TYPE = "map"
        const val SHUTTER_ID = "network_shutter"
        const val TABS_ID = "profile_tabs"
        const val DAY_TAB_ID = "day_tab"
        const val NIGHT_TAB_ID = "night_tab"
        const val TITLE = "Title"
        const val KNOWN_OPERATOR = "EQUALS"
        const val KNOWN_EFFECT = "VISIBILITY"
        const val UNKNOWN_OPERATOR = "CONTAINS"
        const val UNKNOWN_EFFECT = "HIGHLIGHT"
        const val SWITCH_JSON = """{"type":"switch","id":"$SWITCH_ID","title":"Record","currentValue":true}"""
    }
}
