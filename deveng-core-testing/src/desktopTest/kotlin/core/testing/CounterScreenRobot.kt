package core.testing

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import core.testing.fake.FakeCounterEvent
import org.jetbrains.compose.resources.InternalResourceApi
import org.jetbrains.compose.resources.ResourceItem
import org.jetbrains.compose.resources.StringResource

@OptIn(ExperimentalTestApi::class)
class CounterScreenRobot(composeUiTest: ComposeUiTest) : ScreenRobot<FakeCounterEvent>(composeUiTest) {

    fun setContent() = apply {
        val addButtonText = resolveString(ADD_BUTTON_RESOURCE)
        composeUiTest.setContent {
            Button(onClick = { recordEvent(FakeCounterEvent.AddToCounter(amount = ADDED_AMOUNT)) }) {
                Text(text = addButtonText)
            }
        }
    }

    fun clickAddButton() = apply {
        composeUiTest.onNodeWithText(ADD_BUTTON_TEXT).performClick()
    }

    companion object {
        const val ADDED_AMOUNT = 1
        private const val ADD_BUTTON_TEXT = "Add"
        private const val ADD_BUTTON_KEY = "testing_robot_add_button"
        private const val ROBOT_STRINGS_PATH = "testing/robot-strings.cvr"
        private const val ROBOT_STRINGS_RECORD_SIZE = 36L

        // This module has no Compose resources of its own, so the test builds the record the
        // resources plugin would generate (desktopTest/resources/testing/robot-strings.cvr).
        @OptIn(InternalResourceApi::class)
        private val ADD_BUTTON_RESOURCE = StringResource(
            id = "string:$ADD_BUTTON_KEY",
            key = ADD_BUTTON_KEY,
            items = setOf(
                ResourceItem(qualifiers = emptySet(), path = ROBOT_STRINGS_PATH, offset = 0, size = ROBOT_STRINGS_RECORD_SIZE)
            )
        )
    }
}
