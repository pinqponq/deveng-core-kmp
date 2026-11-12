package core.presentation.component.textfield

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.Key
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

    private fun DesktopComposeUiTest.pressShiftEnterIn(
        singleLine: Boolean,
        readOnly: Boolean = false,
        maxLength: Int = 254
    ): String {
        var value by mutableStateOf(FIRST_VALUE)
        setContent {
            AppTheme(darkTheme = false) {
                CustomTextField(
                    value = value,
                    singleLine = singleLine,
                    readOnly = readOnly,
                    maxLength = maxLength,
                    onValueChange = { value = it }
                )
            }
        }
        val field = onNodeWithText(FIRST_VALUE)
        // A read-only field refuses the focus request, but a click still focuses it.
        if (readOnly) field.performClick() else field.requestFocus()
        field.performKeyInput { withKeyDown(Key.ShiftLeft) { pressKey(Key.Enter) } }
        waitForIdle()
        return value
    }

    @Test
    fun pressShiftEnter_inAMultiLineField_addsALineBreakAtTheCaret() = runDesktopComposeUiTest {
        assertEquals("$FIRST_VALUE\n", pressShiftEnterIn(singleLine = false))
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
