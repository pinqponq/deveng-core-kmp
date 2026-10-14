package core.serverdriven.presentation.component

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import core.presentation.theme.AppTheme
import core.serverdriven.presentation.engine.ServerDrivenEngine
import core.serverdriven.presentation.model.ComponentEvent
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class ServerDrivenScreenTest {

    @Test
    fun clickShutterHeader_sendsClickShutterEvent() = runDesktopComposeUiTest {
        val sentEvents = setServerDrivenScreen(isShutterExpanded = false)

        onNodeWithText(ServerDrivenScreenTestLayout.SHUTTER_TITLE).performClick()

        assertEquals(
            listOf(ComponentEvent.ClickShutter(componentId = ServerDrivenScreenTestLayout.SHUTTER_ID)),
            sentEvents
        )
    }

    @Test
    fun clickSwitch_sendsNewCheckedState() = runDesktopComposeUiTest {
        val sentEvents = setServerDrivenScreen(isShutterExpanded = true)

        onNode(isToggleable()).performClick()

        assertEquals(
            listOf(ComponentEvent.ClickSwitch(componentId = ServerDrivenScreenTestLayout.SWITCH_ID, isChecked = true)),
            sentEvents
        )
    }

    @Test
    fun clickSwitch_whenNotEditable_sendsNothing() = runDesktopComposeUiTest {
        val sentEvents = setServerDrivenScreen(
            isShutterExpanded = true,
            preparingEvents = listOf(networkTabClick())
        )

        onNode(isToggleable()).performClick()

        assertTrue(sentEvents.isEmpty())
    }

    @Test
    fun clickTab_sendsSelectedTabId() = runDesktopComposeUiTest {
        val sentEvents = setServerDrivenScreen(isShutterExpanded = false)

        onNodeWithText(ServerDrivenScreenTestLayout.NETWORK_TAB_TITLE).performClick()

        assertEquals(listOf(networkTabClick()), sentEvents)
    }

    @Test
    fun selectTab_withVisibilityDependency_hidesTargetComponent() = runDesktopComposeUiTest {
        setServerDrivenScreen(isShutterExpanded = true, preparingEvents = listOf(networkTabClick()))

        onNodeWithText(ServerDrivenScreenTestLayout.TEXT_FIELD_TITLE).assertDoesNotExist()
        onNodeWithText(ServerDrivenScreenTestLayout.SHUTTER_TITLE).assertExists()
    }

    @Test
    fun enterTextField_sendsEnteredText() = runDesktopComposeUiTest {
        val sentEvents = setServerDrivenScreen(isShutterExpanded = true)

        onNode(hasSetTextAction() and hasText(ServerDrivenScreenTestLayout.TEXT_FIELD_VALUE))
            .performTextReplacement(ENTERED_TEXT)

        assertEquals(
            listOf(
                ComponentEvent.EnterTextField(
                    componentId = ServerDrivenScreenTestLayout.TEXT_FIELD_ID,
                    value = ENTERED_TEXT
                )
            ),
            sentEvents
        )
    }

    @Test
    fun changeSlider_sendsFinishedValue() = runDesktopComposeUiTest {
        val sentEvents = setServerDrivenScreen(isShutterExpanded = false)

        onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo(current = 85f, range = 0f..100f)))
            .performSemanticsAction(SemanticsActions.SetProgress) { setProgress -> setProgress(SLIDER_TARGET_VALUE) }

        assertEquals(
            listOf(
                ComponentEvent.ChangeSliderValue(
                    componentId = ServerDrivenScreenTestLayout.SLIDER_ID,
                    value = SLIDER_TARGET_VALUE
                )
            ),
            sentEvents
        )
    }

    @Test
    fun clickDropdownOption_whenEditable_sendsOption() = runDesktopComposeUiTest {
        val sentEvents = setServerDrivenScreen(
            isShutterExpanded = true,
            preparingEvents = listOf(
                ComponentEvent.ClickSwitch(componentId = ServerDrivenScreenTestLayout.SWITCH_ID, isChecked = true)
            )
        )

        onNodeWithText(ServerDrivenScreenTestLayout.DROPDOWN_VALUE).performClick()
        onNodeWithText(ServerDrivenScreenTestLayout.DROPDOWN_OTHER_OPTION).performClick()

        assertEquals(
            listOf(
                ComponentEvent.ClickDropdownOption(
                    componentId = ServerDrivenScreenTestLayout.DROPDOWN_ID,
                    option = ServerDrivenScreenTestLayout.DROPDOWN_OTHER_OPTION
                )
            ),
            sentEvents
        )
    }

    @Test
    fun clickDropdown_whenNotEditable_doesNotOpenOptions() = runDesktopComposeUiTest {
        val sentEvents = setServerDrivenScreen(isShutterExpanded = true)

        onNodeWithText(ServerDrivenScreenTestLayout.DROPDOWN_VALUE).performClick()

        onNodeWithText(ServerDrivenScreenTestLayout.DROPDOWN_OTHER_OPTION).assertDoesNotExist()
        assertTrue(sentEvents.isEmpty())
    }

    private fun networkTabClick() = ComponentEvent.ClickTabItem(
        componentId = ServerDrivenScreenTestLayout.TABS_ID,
        tabId = ServerDrivenScreenTestLayout.NETWORK_TAB_ID
    )

    private fun ComposeUiTest.setServerDrivenScreen(
        isShutterExpanded: Boolean,
        preparingEvents: List<ComponentEvent> = emptyList()
    ): List<ComponentEvent> {
        val engine = ServerDrivenEngine()
        engine.setLayout(json = ServerDrivenScreenTestLayout.JSON)
        val shutterEvents = if (isShutterExpanded) {
            listOf(ComponentEvent.ClickShutter(componentId = ServerDrivenScreenTestLayout.SHUTTER_ID))
        } else {
            emptyList()
        }
        runBlocking { (shutterEvents + preparingEvents).forEach { event -> engine.handleEvent(event) } }
        val sentEvents = mutableListOf<ComponentEvent>()

        setContent {
            AppTheme(darkTheme = false) {
                ServerDrivenScreen(
                    engine = engine,
                    onComponentEvent = { event -> sentEvents += event }
                )
            }
        }

        return sentEvents
    }

    private companion object {
        const val ENTERED_TEXT = "Parking"
        const val SLIDER_TARGET_VALUE = 40f
    }
}
