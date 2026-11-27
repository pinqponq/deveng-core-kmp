package core.serverdriven.presentation.component

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runDesktopComposeUiTest
import core.serverdriven.presentation.engine.scopedComponentKey
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class ServerDrivenScreenEngineTest {

    @Test
    fun clickShutterHeader_twice_showsThenHidesShutterContent() = runDesktopComposeUiTest {
        engineRobot()
            .assertTextNotShown(ServerDrivenScreenTestLayout.TEXT_FIELD_TITLE)
            .clickShutterHeader()
            .assertTextShown(ServerDrivenScreenTestLayout.TEXT_FIELD_TITLE)
            .clickShutterHeader()
            .assertTextNotShown(ServerDrivenScreenTestLayout.TEXT_FIELD_TITLE)
    }

    @Test
    fun clickNetworkTab_withDependencies_showsItsContentAndAppliesThem() = runDesktopComposeUiTest {
        engineRobot()
            .clickShutterHeader()
            .assertTextShown(ServerDrivenScreenTestLayout.SLIDER_TITLE)
            .clickNetworkTab()
            .assertTextNotShown(ServerDrivenScreenTestLayout.SLIDER_TITLE)
            .assertTextNotShown(ServerDrivenScreenTestLayout.TEXT_FIELD_TITLE)
    }

    @Test
    fun clickSwitch_onLockedDropdown_unlocksItAndShowsSelectedOption() = runDesktopComposeUiTest {
        val robot = engineRobot()
            .clickShutterHeader()
            .clickSwitch()
            .openDropdown()
            .clickDropdownOption(ServerDrivenScreenTestLayout.DROPDOWN_OTHER_OPTION)
            .assertTextNotShown(ServerDrivenScreenTestLayout.DROPDOWN_VALUE)
            .assertTextShown(ServerDrivenScreenTestLayout.DROPDOWN_OTHER_OPTION)

        assertEquals(
            ServerDrivenScreenTestLayout.DROPDOWN_OTHER_OPTION,
            robot.engine.componentValues.value[ServerDrivenScreenTestLayout.DROPDOWN_ID]
        )
    }

    @Test
    fun enterTextField_onEditableField_showsEnteredText() = runDesktopComposeUiTest {
        engineRobot()
            .clickShutterHeader()
            .enterText(ENTERED_TEXT)
            .assertTextFieldShows(ENTERED_TEXT)
    }

    @Test
    fun changeSlider_onEditableSlider_showsNewValue() = runDesktopComposeUiTest {
        val robot = engineRobot()
            .moveSlider(SLIDER_TARGET_VALUE)
            .assertTextShown(SLIDER_TARGET_VALUE.toInt().toString())

        assertEquals(SLIDER_TARGET_VALUE, robot.engine.componentValues.value[ServerDrivenScreenTestLayout.SLIDER_KEY])
    }

    @Test
    fun setLayout_whileShown_sendsEventsWithTheNewLayoutIds() = runDesktopComposeUiTest {
        val robot = engineRobot()
            .clickShutterHeader()
            .setLayout(json = replacedLayoutJson())
            .clickShutterHeader()
            .clickSwitch()
            .openDropdown()
            .clickDropdownOption(ServerDrivenScreenTestLayout.DROPDOWN_OTHER_OPTION)
            .enterText(ENTERED_TEXT)
            .moveSlider(SLIDER_TARGET_VALUE)
            .clickNetworkTab()

        assertEquals(
            mapOf(
                newId(ServerDrivenScreenTestLayout.SWITCH_ID) to true,
                newId(ServerDrivenScreenTestLayout.DROPDOWN_ID) to ServerDrivenScreenTestLayout.DROPDOWN_OTHER_OPTION,
                newId(ServerDrivenScreenTestLayout.TEXT_FIELD_ID) to ENTERED_TEXT,
                scopedComponentKey(
                    scopeKey = newId(ServerDrivenScreenTestLayout.IMAGE_TAB_ID),
                    componentId = newId(ServerDrivenScreenTestLayout.SLIDER_ID)
                ) to SLIDER_TARGET_VALUE,
                newId(ServerDrivenScreenTestLayout.TABS_ID) to newId(ServerDrivenScreenTestLayout.NETWORK_TAB_ID)
            ),
            robot.engine.componentValues.value
        )
    }

    private fun replacedLayoutJson(): String {
        return REPLACED_IDS.fold(ServerDrivenScreenTestLayout.JSON) { json, componentId ->
            json.replace("\"$componentId\"", "\"${newId(componentId)}\"")
        }
    }

    private fun newId(componentId: String) = "$componentId$NEW_LAYOUT_ID_SUFFIX"

    private fun ComposeUiTest.engineRobot(): ServerDrivenScreenRobot {
        return ServerDrivenScreenRobot(composeUiTest = this).setContent(isForwardingToEngine = true)
    }

    private companion object {
        const val ENTERED_TEXT = "Parking"
        const val SLIDER_TARGET_VALUE = 40f
        const val NEW_LAYOUT_ID_SUFFIX = "_v2"
        val REPLACED_IDS = listOf(
            ServerDrivenScreenTestLayout.SHUTTER_ID,
            ServerDrivenScreenTestLayout.SWITCH_ID,
            ServerDrivenScreenTestLayout.DROPDOWN_ID,
            ServerDrivenScreenTestLayout.TEXT_FIELD_ID,
            ServerDrivenScreenTestLayout.SLIDER_ID,
            ServerDrivenScreenTestLayout.TABS_ID,
            ServerDrivenScreenTestLayout.NETWORK_TAB_ID,
            ServerDrivenScreenTestLayout.IMAGE_TAB_ID,
            "record_row",
            "camera_divider"
        )
    }
}
