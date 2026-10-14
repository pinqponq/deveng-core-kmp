package core.serverdriven.presentation.component

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runDesktopComposeUiTest
import core.serverdriven.presentation.model.ComponentEvent
import java.util.TimeZone
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class ServerDrivenScreenInputComponentTest {

    private val originalTimeZone = TimeZone.getDefault()

    @BeforeTest
    fun useTimeZoneEastOfUtc() {
        TimeZone.setDefault(TimeZone.getTimeZone(TIME_ZONE_EAST_OF_UTC))
    }

    @AfterTest
    fun restoreTimeZone() {
        TimeZone.setDefault(originalTimeZone)
    }

    @Test
    fun setLayout_withLabelInSection_showsSectionHeaderAndLabelText() = runDesktopComposeUiTest {
        recordingRobot(isLocked = false)
            .assertTextShown(ServerDrivenInputTestLayout.SECTION_TITLE)
            .assertTextShown(ServerDrivenInputTestLayout.LABEL_TEXT)
    }

    @Test
    fun clickSegmentedOption_whenEditable_sendsOption() = runDesktopComposeUiTest {
        recordingRobot(isLocked = false)
            .clickText(ServerDrivenInputTestLayout.SEGMENTED_OTHER_OPTION)
            .assertSentEvents(
                listOf(
                    ComponentEvent.ClickDropdownOption(
                        componentId = ServerDrivenInputTestLayout.SEGMENTED_ID,
                        option = ServerDrivenInputTestLayout.SEGMENTED_OTHER_OPTION
                    )
                )
            )
    }

    @Test
    fun clickSegmentedOption_whenNotEditable_sendsNothing() = runDesktopComposeUiTest {
        recordingRobot(isLocked = true)
            .clickText(ServerDrivenInputTestLayout.SEGMENTED_OTHER_OPTION)
            .assertSentEvents(emptyList())
    }

    @Test
    fun clickRadioOption_whenEditable_sendsOption() = runDesktopComposeUiTest {
        recordingRobot(isLocked = false)
            .clickText(ServerDrivenInputTestLayout.RADIO_OTHER_OPTION)
            .assertSentEvents(
                listOf(
                    ComponentEvent.ClickDropdownOption(
                        componentId = ServerDrivenInputTestLayout.RADIO_ID,
                        option = ServerDrivenInputTestLayout.RADIO_OTHER_OPTION
                    )
                )
            )
    }

    @Test
    fun clickRadioOption_whenNotEditable_sendsNothing() = runDesktopComposeUiTest {
        recordingRobot(isLocked = true)
            .clickText(ServerDrivenInputTestLayout.RADIO_OTHER_OPTION)
            .assertSentEvents(emptyList())
    }

    @Test
    fun clickRatingStar_whenEditable_sendsStarCountAsFloat() = runDesktopComposeUiTest {
        recordingRobot(isLocked = false)
            .clickRatingStar(starIndex = 3)
            .assertSentEvents(
                listOf(ComponentEvent.ChangeSliderValue(componentId = ServerDrivenInputTestLayout.RATING_ID, value = 4f))
            )
    }

    @Test
    fun clickRatingStar_whenNotEditable_sendsNothing() = runDesktopComposeUiTest {
        recordingRobot(isLocked = true)
            .clickRatingStar(starIndex = 3)
            .assertSentEvents(emptyList())
    }

    @Test
    fun clickRatingStar_whenForwardedToEngine_keepsNewRatingAfterRecomposition() = runDesktopComposeUiTest {
        val robot = ServerDrivenScreenRobot(composeUiTest = this)
            .setLayout(json = ServerDrivenInputTestLayout.JSON)
            .setContent(isForwardingToEngine = true)
            .clickRatingStar(starIndex = 4)

        waitForIdle()

        assertEquals(5f, robot.engine.componentValues.value[ServerDrivenInputTestLayout.RATING_ID])
    }

    @Test
    fun confirmDatePicker_whenEditable_sendsSelectedDateAsIso() = runDesktopComposeUiTest {
        recordingRobot(isLocked = false)
            .clickText(ServerDrivenInputTestLayout.DATE_VALUE)
            .confirmDatePicker()
            .assertSentEvents(
                listOf(
                    ComponentEvent.EnterTextField(
                        componentId = ServerDrivenInputTestLayout.DATE_ID,
                        value = ServerDrivenInputTestLayout.DATE_VALUE
                    )
                )
            )
    }

    @Test
    fun clickDatePicker_whenNotEditable_doesNotOpenDialog() = runDesktopComposeUiTest {
        recordingRobot(isLocked = true)
            .clickText(ServerDrivenInputTestLayout.DATE_VALUE)
            .assertDatePickerNotShown()
            .assertSentEvents(emptyList())
    }

    private suspend fun ComposeUiTest.recordingRobot(isLocked: Boolean): ServerDrivenScreenRobot {
        val lockEvents = if (isLocked) {
            listOf(ComponentEvent.ClickSwitch(componentId = ServerDrivenInputTestLayout.LOCK_SWITCH_ID, isChecked = true))
        } else {
            emptyList()
        }

        return ServerDrivenScreenRobot(composeUiTest = this)
            .setLayout(json = ServerDrivenInputTestLayout.JSON)
            .prepareEngine(lockEvents)
            .setContent(isForwardingToEngine = false)
    }

    private companion object {
        const val TIME_ZONE_EAST_OF_UTC = "Europe/Istanbul"
    }
}
