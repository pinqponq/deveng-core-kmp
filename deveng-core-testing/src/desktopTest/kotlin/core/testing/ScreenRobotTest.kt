package core.testing

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import core.testing.fake.FakeCounterEvent
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class ScreenRobotTest {

    @Test
    fun clickAddButton_twice_recordsBothEventsInOrder() = runComposeUiTest {
        val robot = CounterScreenRobot(composeUiTest = this)
            .setContent()
            .clickAddButton()
            .clickAddButton()

        val expectedEvent = FakeCounterEvent.AddToCounter(amount = CounterScreenRobot.ADDED_AMOUNT)
        assertEquals(listOf(expectedEvent, expectedEvent), robot.sentEvents)
    }
}
