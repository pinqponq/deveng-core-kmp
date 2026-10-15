package core.serverdriven.presentation.component

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.unit.dp
import core.presentation.theme.ServerDrivenScreenTheme
import core.serverdriven.presentation.model.ComponentEvent
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class ServerDrivenScreenRecompositionTest {

    @Test
    fun replaceEventHandler_whileShown_sendsEveryLaterEventToNewHandler() = runDesktopComposeUiTest {
        val robot = ServerDrivenScreenRobot(composeUiTest = this)
            .prepareEngine(
                listOf(
                    ComponentEvent.ClickShutter(componentId = ServerDrivenScreenTestLayout.SHUTTER_ID),
                    ComponentEvent.ClickSwitch(componentId = ServerDrivenScreenTestLayout.SWITCH_ID, isChecked = true)
                )
            )
            .setContent(isForwardingToEngine = false)
            .replaceEventHandler()
            .clickSwitch()
            .openDropdown()
            .clickDropdownOption(ServerDrivenScreenTestLayout.DROPDOWN_OTHER_OPTION)
            .enterText(ENTERED_TEXT)
            .moveSlider(SLIDER_TARGET_VALUE)
            .clickNetworkTab()
            .clickShutterHeader()
            .assertSentEvents(emptyList())

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
            robot.replacedHandlerEvents
        )
    }

    @Test
    fun replaceModifier_whileShown_appliesNewModifier() = runDesktopComposeUiTest {
        ServerDrivenScreenRobot(composeUiTest = this)
            .setContent(isForwardingToEngine = false)
            .assertScreenTag(ServerDrivenScreenRobot.SCREEN_TAG)
            .replaceScreenTag(REPLACED_TAG)
            .assertScreenTag(REPLACED_TAG)
            .assertNoScreenTag(ServerDrivenScreenRobot.SCREEN_TAG)
    }

    @Test
    fun divider_withDefaultTheme_takesThemeThickness() = runDesktopComposeUiTest {
        ServerDrivenScreenRobot(composeUiTest = this)
            .setLayout(json = """{ "components": [{ "type": "divider", "id": "divider" }], "dependencies": [] }""")
            .setContent(isForwardingToEngine = false)
            .assertScreenHeight(ServerDrivenScreenTheme().dividerThickness)
    }

    @Test
    fun tabs_withoutAnyTab_drawNothing() = runDesktopComposeUiTest {
        ServerDrivenScreenRobot(composeUiTest = this)
            .setLayout(json = """{ "components": [{ "type": "tabs", "id": "tabs", "tabs": [] }], "dependencies": [] }""")
            .setContent(isForwardingToEngine = false)
            .assertScreenHeight(0.dp)
    }

    private companion object {
        const val ENTERED_TEXT = "Parking"
        const val SLIDER_TARGET_VALUE = 40f
        const val REPLACED_TAG = "replaced_screen"
    }
}
