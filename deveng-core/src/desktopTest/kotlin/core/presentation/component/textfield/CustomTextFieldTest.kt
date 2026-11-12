package core.presentation.component.textfield

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.test.DesktopComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.test.withKeyDown
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import core.presentation.theme.AppTheme
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class CustomTextFieldTest {

    @Test
    fun readOnlyField_whileFocused_showsNewCallerValue() = runDesktopComposeUiTest {
        var value by mutableStateOf(FIRST_VALUE)
        setContent {
            AppTheme(darkTheme = false) {
                CustomTextField(value = value, readOnly = true, onValueChange = {})
            }
        }

        onNodeWithText(FIRST_VALUE)
            .performClick()
            .assertIsFocused()

        value = SECOND_VALUE

        onNodeWithText(SECOND_VALUE).assertIsFocused()
    }

    private class FieldRecord {
        var value by mutableStateOf(FIRST_VALUE)
        var valueChangeCount = 0
        var enterPressesReachingParentCount = 0
    }

    private fun DesktopComposeUiTest.setMultiLineField(
        singleLine: Boolean = false,
        readOnly: Boolean = false,
        isEditable: Boolean = true,
        maxLength: Int = 254
    ): FieldRecord {
        val record = FieldRecord()
        setContent {
            AppTheme(darkTheme = false) {
                CustomTextField(
                    containerModifier = Modifier.onKeyEvent { keyEvent ->
                        if (keyEvent.key == Key.Enter && keyEvent.type == KeyEventType.KeyDown) record.enterPressesReachingParentCount++
                        false
                    },
                    value = record.value,
                    singleLine = singleLine,
                    readOnly = readOnly,
                    isEditable = isEditable,
                    maxLength = maxLength,
                    onValueChange = {
                        record.valueChangeCount++
                        record.value = it
                    }
                )
            }
        }
        val field = onNodeWithText(FIRST_VALUE)
        // A read-only field refuses the focus request, but a click still focuses it.
        if (readOnly) field.performClick() else field.requestFocus()
        return record
    }

    private fun DesktopComposeUiTest.pressShiftEnterIn(
        singleLine: Boolean,
        readOnly: Boolean = false,
        maxLength: Int = 254
    ): String {
        val record = setMultiLineField(singleLine = singleLine, readOnly = readOnly, maxLength = maxLength)
        onNodeWithText(FIRST_VALUE).performKeyInput { withKeyDown(Key.ShiftLeft) { pressKey(Key.Enter) } }
        waitForIdle()
        return record.value
    }

    @Test
    fun pressShiftEnter_inAMultiLineField_isNotPassedToTheParent() = runDesktopComposeUiTest {
        val record = setMultiLineField()

        onNodeWithText(FIRST_VALUE).performKeyInput { withKeyDown(Key.ShiftLeft) { pressKey(Key.Enter) } }
        waitForIdle()

        assertEquals(0, record.enterPressesReachingParentCount)
    }

    @Test
    fun holdShiftEnterDown_inAMultiLineField_addsTheLineBreakBeforeTheKeyIsReleased() = runDesktopComposeUiTest {
        val record = setMultiLineField()

        onNodeWithText(FIRST_VALUE).performKeyInput {
            keyDown(Key.ShiftLeft)
            keyDown(Key.Enter)
        }
        waitForIdle()

        assertEquals("$FIRST_VALUE\n", record.value)
    }

    @Test
    fun pressShiftNumPadEnter_inAMultiLineField_addsALineBreak() = runDesktopComposeUiTest {
        val record = setMultiLineField()

        onNodeWithText(FIRST_VALUE).performKeyInput { withKeyDown(Key.ShiftLeft) { pressKey(Key.NumPadEnter) } }
        waitForIdle()

        assertEquals("$FIRST_VALUE\n", record.value)
    }

    @Test
    fun pressCtrlShiftEnter_inAMultiLineField_addsNothing() = runDesktopComposeUiTest {
        val record = setMultiLineField()

        onNodeWithText(FIRST_VALUE).performKeyInput {
            withKeyDown(Key.CtrlLeft) { withKeyDown(Key.ShiftLeft) { pressKey(Key.Enter) } }
        }
        waitForIdle()

        assertEquals(FIRST_VALUE, record.value)
    }

    @Test
    fun pressBackspace_inAMultiLineField_stillReachesTheField() = runDesktopComposeUiTest {
        val record = setMultiLineField()

        onNodeWithText(FIRST_VALUE).performKeyInput { pressKey(Key.Backspace) }
        waitForIdle()

        assertEquals(FIRST_VALUE.dropLast(1), record.value)
    }

    @Test
    fun pressShiftEnter_inANotEditableField_addsNothing() = runDesktopComposeUiTest {
        val record = setMultiLineField(isEditable = false)

        onNodeWithText(FIRST_VALUE).performKeyInput { withKeyDown(Key.ShiftLeft) { pressKey(Key.Enter) } }
        waitForIdle()

        assertEquals(FIRST_VALUE, record.value)
    }

    @Test
    fun pressShiftEnter_withRoomForExactlyOneMoreCharacter_addsALineBreak() = runDesktopComposeUiTest {
        assertEquals("$FIRST_VALUE\n", pressShiftEnterIn(singleLine = false, maxLength = FIRST_VALUE.length + 1))
    }

    @Test
    fun moveTheCaret_inAMultiLineField_doesNotReportAValueChange() = runDesktopComposeUiTest {
        val record = setMultiLineField()

        onNodeWithText(FIRST_VALUE).performKeyInput { pressKey(Key.DirectionLeft) }
        waitForIdle()

        assertEquals(0, record.valueChangeCount)
    }

    @Test
    fun pressShiftEnter_inAMultiLineField_addsALineBreakAtTheCaret() = runDesktopComposeUiTest {
        assertEquals("$FIRST_VALUE\n", pressShiftEnterIn(singleLine = false))
    }

    @Test
    fun pressShiftEnter_inAMultiLineField_showsTheLineBreakInTheField() = runDesktopComposeUiTest {
        pressShiftEnterIn(singleLine = false)

        onNodeWithText("$FIRST_VALUE\n").assertExists()
    }

    @Test
    fun pressShiftEnter_inASingleLineField_addsNothing() = runDesktopComposeUiTest {
        assertEquals(FIRST_VALUE, pressShiftEnterIn(singleLine = true))
    }

    @Test
    fun pressShiftEnter_inAReadOnlyField_addsNothing() = runDesktopComposeUiTest {
        assertEquals(FIRST_VALUE, pressShiftEnterIn(singleLine = false, readOnly = true))
    }

    @Test
    fun pressShiftEnter_inAFullField_addsNothing() = runDesktopComposeUiTest {
        assertEquals(FIRST_VALUE, pressShiftEnterIn(singleLine = false, maxLength = FIRST_VALUE.length))
    }

    @Test
    fun withLineBreakAtSelection_withSelectedText_replacesItAndPutsTheCaretAfterTheBreak() {
        val value = TextFieldValue(text = "Hello world", selection = TextRange(5, 6))

        val result = value.withLineBreakAtSelection()

        assertEquals("Hello\nworld", result.text)
        assertEquals(TextRange(6), result.selection)
    }

    private companion object {
        const val FIRST_VALUE = "1080p"
        const val SECOND_VALUE = "4K"
    }
}
