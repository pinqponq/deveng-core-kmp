package core.serverdriven.presentation.engine

import core.serverdriven.data.model.response.ComponentResponse
import core.serverdriven.data.model.response.DependencyResponse
import core.serverdriven.data.model.response.LayoutResponse
import core.serverdriven.presentation.model.ComponentEvent
import core.serverdriven.presentation.model.ComponentUiModel
import core.util.CustomLogger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ServerDrivenEngineLoggingTest {

    private val originalOutput = System.out
    private val capturedOutput = ByteArrayOutputStream()

    @BeforeTest
    fun captureLogs() {
        CustomLogger.isLoggingEnabled = true
        System.setOut(PrintStream(capturedOutput, true))
    }

    @AfterTest
    fun restoreOutput() {
        System.setOut(originalOutput)
        CustomLogger.isLoggingEnabled = false
    }

    @Test
    fun setLayout_withUnknownValues_logsEachSkippedOrDefaultedValue() {
        val engine = ServerDrivenEngine()

        engine.setLayout(
            layout = LayoutResponse(
                components = listOf(
                    ComponentResponse.UnknownResponse(id = CHART_ID, type = CHART_TYPE),
                    ComponentResponse.TextFieldResponse(
                        id = TEXT_FIELD_ID,
                        title = TITLE,
                        currentValue = "",
                        defaultValue = "",
                        factoryValue = "",
                        format = UNKNOWN_FORMAT,
                        keyboardType = UNKNOWN_KEYBOARD_TYPE
                    )
                ),
                dependencies = listOf(
                    DependencyResponse(
                        sourceId = TEXT_FIELD_ID,
                        targetId = TEXT_FIELD_ID,
                        operator = UNKNOWN_OPERATOR,
                        triggerValue = "",
                        effectType = "VISIBILITY",
                        effectValue = false
                    )
                )
            )
        )

        assertEquals(
            listOf(CHART_TYPE, UNKNOWN_FORMAT, UNKNOWN_KEYBOARD_TYPE, UNKNOWN_OPERATOR),
            loggedLines().map { line -> REPORTED_VALUES.first { value -> line.contains("'$value'") } }
        )
    }

    @Test
    fun setLayout_withKnownValues_logsNothing() {
        val engine = ServerDrivenEngine()

        engine.setLayout(
            layout = LayoutResponse(
                components = listOf(
                    ComponentResponse.TextFieldResponse(
                        id = TEXT_FIELD_ID,
                        title = TITLE,
                        currentValue = "",
                        defaultValue = "",
                        factoryValue = "",
                        format = "TIME",
                        keyboardType = "NUMERIC"
                    )
                )
            )
        )

        assertEquals(emptyList(), loggedLines())
    }

    @Test
    fun enterTextField_onNotEditableField_logsIgnoredValue() = runTest {
        val engine = ServerDrivenEngine()
        engine.setLayout(
            layout = LayoutResponse(
                components = listOf(
                    ComponentResponse.SwitchResponse(id = SWITCH_ID, title = TITLE, currentValue = true),
                    ComponentResponse.TextFieldResponse(
                        id = TEXT_FIELD_ID,
                        title = TITLE,
                        currentValue = "",
                        defaultValue = "",
                        factoryValue = ""
                    )
                ),
                dependencies = listOf(
                    DependencyResponse(
                        sourceId = SWITCH_ID,
                        targetId = TEXT_FIELD_ID,
                        operator = "EQUALS",
                        triggerValue = "true",
                        effectType = "EDITABLE",
                        effectValue = false
                    )
                )
            )
        )

        engine.handleEvent(ComponentEvent.EnterTextField(componentId = TEXT_FIELD_ID, value = TITLE))

        assertEquals(1, loggedLines().count { line -> line.contains("'$TEXT_FIELD_ID'") })
    }

    @Test
    fun setLayout_withDependencyOnMissingId_logsThatDependency() {
        val engine = ServerDrivenEngine()

        engine.setLayout(
            layout = LayoutResponse(
                components = listOf(switchResponse()),
                dependencies = listOf(missingTargetDependency())
            )
        )

        assertEquals(1, loggedLines().count { line -> line.contains("'$MISSING_ID'") })
    }

    @Test
    fun clickShutter_withLoadedDependencyOnMissingId_logsThatDependency() = runTest {
        val engine = ServerDrivenEngine(
            shutterContentLoader = {
                LayoutResponse(dependencies = listOf(missingTargetDependency()))
            }
        )

        engine.setLayout(
            layout = LayoutResponse(
                components = listOf(
                    ComponentResponse.ShutterResponse(
                        id = SHUTTER_ID,
                        title = TITLE,
                        components = listOf(switchResponse())
                    )
                )
            )
        )

        engine.handleEvent(ComponentEvent.ClickShutter(componentId = SHUTTER_ID))

        assertEquals(1, loggedLines().count { line -> line.contains("'$MISSING_ID'") })
    }

    @Test
    fun setLayout_withDocumentExampleUsingDeprecatedTabType_logsOnlyTheTypeOnce() {
        val engine = ServerDrivenEngine()

        engine.setLayout(json = DayNightTestLayout.json(tabsType = DEPRECATED_TABS_TYPE))

        val loggedLine = loggedLines().single()
        assertTrue(loggedLine.contains("'${DayNightTestLayout.TABS_ID}'"))
        assertTrue(loggedLine.contains("'$DEPRECATED_TABS_TYPE'"))
    }

    @Test
    fun clickShutter_whenLayoutReplacedDuringLoad_logsAndKeepsNewLayout() = runTest {
        val loadedContent = CompletableDeferred<LayoutResponse>()
        val engine = ServerDrivenEngine(shutterContentLoader = { loadedContent.await() })
        engine.setLayout(
            layout = LayoutResponse(
                components = listOf(ComponentResponse.ShutterResponse(id = SHUTTER_ID, title = TITLE))
            )
        )
        val shutterLoad = launch { engine.handleEvent(ComponentEvent.ClickShutter(componentId = SHUTTER_ID)) }
        testScheduler.runCurrent()

        engine.setLayout(layout = LayoutResponse(components = listOf(switchResponse())))
        loadedContent.complete(LayoutResponse(components = listOf(switchResponse().copy(id = TEXT_FIELD_ID))))
        shutterLoad.join()

        assertEquals(listOf(SWITCH_ID), engine.components.value.map { component -> component.id })
        assertEquals(1, loggedLines().count { line -> line.contains("'$SHUTTER_ID'") })
    }

    @Test
    fun clickShutter_whenLayoutWithSameShutterReplacedDuringLoad_dropsOldContent() = runTest {
        val loadedContent = CompletableDeferred<LayoutResponse>()
        val engine = ServerDrivenEngine(shutterContentLoader = { loadedContent.await() })
        val shutterLayout = LayoutResponse(
            components = listOf(ComponentResponse.ShutterResponse(id = SHUTTER_ID, title = TITLE))
        )
        engine.setLayout(layout = shutterLayout)
        val shutterLoad = launch { engine.handleEvent(ComponentEvent.ClickShutter(componentId = SHUTTER_ID)) }
        testScheduler.runCurrent()

        engine.setLayout(layout = shutterLayout)
        loadedContent.complete(LayoutResponse(components = listOf(switchResponse())))
        shutterLoad.join()

        assertEquals(listOf(SHUTTER_ID), engine.components.value.map { component -> component.id })
        assertEquals(emptyList(), (engine.components.value.single() as ComponentUiModel.Shutter).components)
        assertEquals(1, loggedLines().count { line -> line.contains("'$SHUTTER_ID'") })
    }

    @Test
    fun clickShutter_whenLoaderFailsAfterLayoutReplaced_rethrowsLoaderException() = runTest {
        val loadedContent = CompletableDeferred<LayoutResponse>()
        val engine = ServerDrivenEngine(shutterContentLoader = { loadedContent.await() })
        engine.setLayout(
            layout = LayoutResponse(
                components = listOf(ComponentResponse.ShutterResponse(id = SHUTTER_ID, title = TITLE))
            )
        )
        val shutterLoad = async {
            runCatching { engine.handleEvent(ComponentEvent.ClickShutter(componentId = SHUTTER_ID)) }
        }
        testScheduler.runCurrent()

        engine.setLayout(layout = LayoutResponse(components = listOf(switchResponse())))
        loadedContent.completeExceptionally(IllegalStateException(LOADER_FAILURE_MESSAGE))

        assertEquals(LOADER_FAILURE_MESSAGE, shutterLoad.await().exceptionOrNull()?.message)
        assertEquals(listOf(SWITCH_ID), engine.components.value.map { component -> component.id })
    }

    private fun switchResponse() = ComponentResponse.SwitchResponse(id = SWITCH_ID, title = TITLE, currentValue = true)

    private fun missingTargetDependency() = DependencyResponse(
        sourceId = SWITCH_ID,
        targetId = MISSING_ID,
        operator = "EQUALS",
        triggerValue = "true",
        effectType = "VISIBILITY",
        effectValue = false
    )

    private fun loggedLines(): List<String> =
        capturedOutput.toString().lines().filter { line -> line.isNotBlank() }

    private companion object {
        const val CHART_ID = "usage_chart"
        const val CHART_TYPE = "chart"
        const val TEXT_FIELD_ID = "start_time_text_field"
        const val SWITCH_ID = "record_switch"
        const val MISSING_ID = "missing_component"
        const val SHUTTER_ID = "camera_shutter"
        const val TITLE = "Title"
        const val DEPRECATED_TABS_TYPE = "tab"
        const val LOADER_FAILURE_MESSAGE = "shutter content unavailable"
        const val UNKNOWN_FORMAT = "IBAN"
        const val UNKNOWN_KEYBOARD_TYPE = "DIAL_PAD"
        const val UNKNOWN_OPERATOR = "CONTAINS"
        val REPORTED_VALUES = listOf(CHART_TYPE, UNKNOWN_FORMAT, UNKNOWN_KEYBOARD_TYPE, UNKNOWN_OPERATOR)
    }
}
