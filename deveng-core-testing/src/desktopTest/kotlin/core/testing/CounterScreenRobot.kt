package core.testing

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import core.testing.fake.FakeCounterEvent

@OptIn(ExperimentalTestApi::class)
class CounterScreenRobot(composeUiTest: ComposeUiTest) : ScreenRobot<FakeCounterEvent>(composeUiTest) {

    fun setContent() = apply {
        composeUiTest.setContent {
            Button(onClick = { recordEvent(FakeCounterEvent.AddToCounter(amount = ADDED_AMOUNT)) }) {
                Text(text = ADD_BUTTON_TEXT)
            }
        }
    }

    fun clickAddButton() = apply {
        composeUiTest.onNodeWithText(ADD_BUTTON_TEXT).performClick()
    }

    companion object {
        const val ADDED_AMOUNT = 1
        private const val ADD_BUTTON_TEXT = "Add"
    }
}
