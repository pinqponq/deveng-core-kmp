package core.presentation.component.emaildisplayfield

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runComposeUiTest
import core.presentation.theme.AppTheme
import core.util.EMPTY
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class EmailDisplayFieldTest {

    @Test
    fun enterEmail_whenChangeable_sendsTypedEmail() = runComposeUiTest {
        var enteredEmail = String.EMPTY
        setContent {
            AppTheme {
                EmailDisplayField(
                    email = String.EMPTY,
                    isChangeable = true,
                    onEmailChange = { email -> enteredEmail = email }
                )
            }
        }

        onNode(hasSetTextAction()).performTextInput(EMAIL)

        assertEquals(EMAIL, enteredEmail)
    }

    @Test
    fun clickEditText_whenEditTextVisible_sendsEditClick() = runComposeUiTest {
        var editClickCount = 0
        setContent {
            AppTheme {
                EmailDisplayField(
                    email = EMAIL,
                    editText = EDIT_TEXT,
                    isEditTextVisible = true,
                    onEditTextClick = { editClickCount++ }
                )
            }
        }

        onNodeWithText(EDIT_TEXT).performClick()

        assertEquals(1, editClickCount)
    }

    @Test
    fun render_withEditTextHidden_showsNoEditText() = runComposeUiTest {
        setContent {
            AppTheme {
                EmailDisplayField(
                    email = EMAIL,
                    editText = EDIT_TEXT,
                    isEditTextVisible = false
                )
            }
        }

        onAllNodesWithText(EDIT_TEXT).assertCountEquals(0)
    }

    @Test
    fun render_withErrorAndTitleVisibility_showsOnlyVisibleTexts() = runComposeUiTest {
        setContent {
            AppTheme {
                EmailDisplayField(
                    email = EMAIL,
                    title = TITLE,
                    errorMessage = ERROR_MESSAGE,
                    isErrorMessageVisible = true,
                    isTitleVisible = false
                )
            }
        }

        onAllNodesWithText(ERROR_MESSAGE).assertCountEquals(1)
        onAllNodesWithText(TITLE).assertCountEquals(0)
    }

    @Test
    fun render_withErrorHiddenAndTitleVisible_showsTitleWithoutError() = runComposeUiTest {
        setContent {
            AppTheme {
                EmailDisplayField(
                    email = EMAIL,
                    title = TITLE,
                    errorMessage = ERROR_MESSAGE
                )
            }
        }

        onAllNodesWithText(ERROR_MESSAGE).assertCountEquals(0)
        onAllNodesWithText(TITLE).assertCountEquals(1)
    }

    private companion object {
        const val EMAIL = "person@example.com"
        const val EDIT_TEXT = "edit"
        const val TITLE = "E-Mail"
        const val ERROR_MESSAGE = "Please enter a valid email address."
    }
}
