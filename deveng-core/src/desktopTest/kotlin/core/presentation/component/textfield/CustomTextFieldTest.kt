package core.presentation.component.textfield

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runDesktopComposeUiTest
import core.presentation.theme.AppTheme
import kotlin.test.Test

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

    private companion object {
        const val FIRST_VALUE = "1080p"
        const val SECOND_VALUE = "4K"
    }
}
