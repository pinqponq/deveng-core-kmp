package core.serverdriven.presentation.component

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.unit.dp
import core.presentation.theme.AppTheme
import core.presentation.theme.LocalComponentTheme
import core.serverdriven.presentation.engine.ServerDrivenEngine
import core.serverdriven.presentation.model.ComponentEvent
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class ServerDrivenScreenRecompositionTest {

    @Test
    fun replaceEventHandler_sendsEveryLaterEventToNewHandler() = runDesktopComposeUiTest {
        val engine = ServerDrivenEngine()
        engine.setLayout(json = ServerDrivenScreenTestLayout.JSON)
        runBlocking {
            engine.handleEvent(ComponentEvent.ClickShutter(componentId = ServerDrivenScreenTestLayout.SHUTTER_ID))
            engine.handleEvent(
                ComponentEvent.ClickSwitch(componentId = ServerDrivenScreenTestLayout.SWITCH_ID, isChecked = true)
            )
        }
        val firstHandlerEvents = mutableListOf<ComponentEvent>()
        val secondHandlerEvents = mutableListOf<ComponentEvent>()
        var onComponentEvent by mutableStateOf<(ComponentEvent) -> Unit>({ event -> firstHandlerEvents += event })
        setContent {
            AppTheme(darkTheme = false) {
                ServerDrivenScreen(engine = engine, onComponentEvent = onComponentEvent)
            }
        }

        onComponentEvent = { event -> secondHandlerEvents += event }
        onNode(isToggleable()).performClick()
        onNodeWithText(ServerDrivenScreenTestLayout.DROPDOWN_VALUE).performClick()
        onNodeWithText(ServerDrivenScreenTestLayout.DROPDOWN_OTHER_OPTION).performClick()
        onNode(hasSetTextAction() and hasText(ServerDrivenScreenTestLayout.TEXT_FIELD_VALUE))
            .performTextReplacement(ENTERED_TEXT)
        onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo(current = 85f, range = 0f..100f)))
            .performSemanticsAction(SemanticsActions.SetProgress) { setProgress -> setProgress(SLIDER_TARGET_VALUE) }
        onNodeWithText(ServerDrivenScreenTestLayout.NETWORK_TAB_TITLE).performClick()
        onNodeWithText(ServerDrivenScreenTestLayout.SHUTTER_TITLE).performClick()

        assertTrue(firstHandlerEvents.isEmpty())
        assertEquals(
            listOf(
                ComponentEvent.ClickSwitch(componentId = ServerDrivenScreenTestLayout.SWITCH_ID, isChecked = false),
                ComponentEvent.ClickDropdownOption(
                    componentId = ServerDrivenScreenTestLayout.DROPDOWN_ID,
                    option = ServerDrivenScreenTestLayout.DROPDOWN_OTHER_OPTION
                ),
                ComponentEvent.EnterTextField(componentId = ServerDrivenScreenTestLayout.TEXT_FIELD_ID, value = ENTERED_TEXT),
                ComponentEvent.ChangeSliderValue(
                    componentId = ServerDrivenScreenTestLayout.SLIDER_ID,
                    value = SLIDER_TARGET_VALUE
                ),
                ComponentEvent.ClickTabItem(
                    componentId = ServerDrivenScreenTestLayout.TABS_ID,
                    tabId = ServerDrivenScreenTestLayout.NETWORK_TAB_ID
                ),
                ComponentEvent.ClickShutter(componentId = ServerDrivenScreenTestLayout.SHUTTER_ID)
            ),
            secondHandlerEvents
        )
    }

    @Test
    fun replaceModifier_appliesNewModifier() = runDesktopComposeUiTest {
        val engine = ServerDrivenEngine()
        engine.setLayout(json = ServerDrivenScreenTestLayout.JSON)
        var screenTag by mutableStateOf(FIRST_TAG)
        setContent {
            AppTheme(darkTheme = false) {
                ServerDrivenScreen(engine = engine, modifier = Modifier.testTag(screenTag), onComponentEvent = {})
            }
        }
        onNodeWithTag(FIRST_TAG).assertExists()

        screenTag = SECOND_TAG

        onNodeWithTag(SECOND_TAG).assertExists()
        onNodeWithTag(FIRST_TAG).assertDoesNotExist()
    }

    @Test
    fun divider_takesThemeThickness() = runDesktopComposeUiTest {
        val engine = ServerDrivenEngine()
        engine.setLayout(json = """{ "components": [{ "type": "divider", "id": "divider" }], "dependencies": [] }""")
        var dividerThickness = 0.dp
        setContent {
            AppTheme(darkTheme = false) {
                dividerThickness = LocalComponentTheme.current.serverDrivenScreen.dividerThickness
                ServerDrivenScreen(engine = engine, modifier = Modifier.testTag(FIRST_TAG), onComponentEvent = {})
            }
        }

        onNodeWithTag(FIRST_TAG).assertHeightIsEqualTo(dividerThickness)
    }

    @Test
    fun tabsWithoutTabs_drawNothing() = runDesktopComposeUiTest {
        val engine = ServerDrivenEngine()
        engine.setLayout(json = """{ "components": [{ "type": "tabs", "id": "tabs", "tabs": [] }], "dependencies": [] }""")
        setContent {
            AppTheme(darkTheme = false) {
                ServerDrivenScreen(engine = engine, modifier = Modifier.testTag(FIRST_TAG), onComponentEvent = {})
            }
        }

        onNodeWithTag(FIRST_TAG).assertHeightIsEqualTo(0.dp)
    }

    private companion object {
        const val ENTERED_TEXT = "Parking"
        const val SLIDER_TARGET_VALUE = 40f
        const val FIRST_TAG = "first_screen"
        const val SECOND_TAG = "second_screen"
    }
}
