package core.serverdriven.presentation.component

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runDesktopComposeUiTest
import core.serverdriven.presentation.model.ComponentEvent
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class ServerDrivenScreenTest {

    @Test
    fun clickShutterHeader_sendsClickShutterEvent() = runDesktopComposeUiTest {
        recordingRobot(isShutterExpanded = false)
            .clickShutterHeader()
            .assertSentEvents(listOf(shutterClick()))
    }

    @Test
    fun clickSwitch_sendsNewCheckedState() = runDesktopComposeUiTest {
        recordingRobot(isShutterExpanded = true)
            .clickSwitch()
            .assertSentEvents(
                listOf(ComponentEvent.ClickSwitch(componentId = ServerDrivenScreenTestLayout.SWITCH_ID, isChecked = true))
            )
    }

    @Test
    fun clickSwitch_whenNotEditable_sendsNothing() = runDesktopComposeUiTest {
        recordingRobot(isShutterExpanded = true, preparingEvents = listOf(networkTabClick()))
            .clickSwitch()
            .assertSentEvents(emptyList())
    }

    @Test
    fun clickTab_sendsSelectedTabId() = runDesktopComposeUiTest {
        recordingRobot(isShutterExpanded = false)
            .clickNetworkTab()
            .assertSentEvents(listOf(networkTabClick()))
    }

    @Test
    fun selectTab_withVisibilityDependency_hidesTargetComponent() = runDesktopComposeUiTest {
        recordingRobot(isShutterExpanded = true, preparingEvents = listOf(networkTabClick()))
            .assertTextNotShown(ServerDrivenScreenTestLayout.TEXT_FIELD_TITLE)
            .assertTextShown(ServerDrivenScreenTestLayout.SHUTTER_TITLE)
    }

    @Test
    fun enterTextField_sendsEnteredText() = runDesktopComposeUiTest {
        recordingRobot(isShutterExpanded = true)
            .enterText(ENTERED_TEXT)
            .assertSentEvents(
                listOf(
                    ComponentEvent.EnterTextField(
                        componentId = ServerDrivenScreenTestLayout.TEXT_FIELD_ID,
                        value = ENTERED_TEXT
                    )
                )
            )
    }

    @Test
    fun changeSlider_sendsFinishedValue() = runDesktopComposeUiTest {
        recordingRobot(isShutterExpanded = false)
            .moveSlider(SLIDER_TARGET_VALUE)
            .assertSentEvents(
                listOf(
                    ComponentEvent.ChangeSliderValue(
                        componentId = ServerDrivenScreenTestLayout.SLIDER_ID,
                        value = SLIDER_TARGET_VALUE
                    )
                )
            )
            .assertTextShown(SLIDER_TARGET_VALUE.toInt().toString())
    }

    @Test
    fun clickDropdownOption_whenEditable_sendsOption() = runDesktopComposeUiTest {
        recordingRobot(
            isShutterExpanded = true,
            preparingEvents = listOf(
                ComponentEvent.ClickSwitch(componentId = ServerDrivenScreenTestLayout.SWITCH_ID, isChecked = true)
            )
        )
            .openDropdown()
            .clickDropdownOption(ServerDrivenScreenTestLayout.DROPDOWN_OTHER_OPTION)
            .assertSentEvents(
                listOf(
                    ComponentEvent.ClickDropdownOption(
                        componentId = ServerDrivenScreenTestLayout.DROPDOWN_ID,
                        option = ServerDrivenScreenTestLayout.DROPDOWN_OTHER_OPTION
                    )
                )
            )
    }

    @Test
    fun clickDropdown_whenNotEditable_doesNotOpenOptions() = runDesktopComposeUiTest {
        recordingRobot(isShutterExpanded = true)
            .openDropdown()
            .assertTextNotShown(ServerDrivenScreenTestLayout.DROPDOWN_OTHER_OPTION)
            .assertSentEvents(emptyList())
    }

    private fun shutterClick() = ComponentEvent.ClickShutter(componentId = ServerDrivenScreenTestLayout.SHUTTER_ID)

    private fun networkTabClick() = ComponentEvent.ClickTabItem(
        componentId = ServerDrivenScreenTestLayout.TABS_ID,
        tabId = ServerDrivenScreenTestLayout.NETWORK_TAB_ID
    )

    private suspend fun ComposeUiTest.recordingRobot(
        isShutterExpanded: Boolean,
        preparingEvents: List<ComponentEvent> = emptyList()
    ): ServerDrivenScreenRobot {
        val shutterEvents = if (isShutterExpanded) listOf(shutterClick()) else emptyList()

        return ServerDrivenScreenRobot(composeUiTest = this)
            .prepareEngine(shutterEvents + preparingEvents)
            .setContent(isForwardingToEngine = false)
    }

    private companion object {
        const val ENTERED_TEXT = "Parking"
        const val SLIDER_TARGET_VALUE = 40f
    }
}
