package core.presentation.component.scrollablecontentdialog

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import core.presentation.theme.AppTheme
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class ScrollableContentDialogTest {

    @Test
    fun clickBackIcon_whenDialogVisible_sendsBackClickOnly() = runComposeUiTest {
        var backClickCount = 0
        var checkClickCount = 0
        setContent {
            AppTheme {
                ScrollableContentDialog(
                    title = TITLE,
                    content = CONTENT,
                    isDialogVisible = true,
                    isCheckboxVisible = true,
                    backIconDescription = BACK_DESCRIPTION,
                    checkboxIconDescription = CHECKBOX_DESCRIPTION,
                    onBackIconClick = { backClickCount++ },
                    onDismissRequest = {},
                    onCheckIconClick = { checkClickCount++ }
                )
            }
        }

        onNodeWithContentDescription(BACK_DESCRIPTION).performClick()

        assertEquals(1, backClickCount)
        assertEquals(0, checkClickCount)
    }

    @Test
    fun clickCheckboxIcon_whenCheckboxVisible_sendsCheckClickOnly() = runComposeUiTest {
        var backClickCount = 0
        var checkClickCount = 0
        setContent {
            AppTheme {
                ScrollableContentDialog(
                    title = TITLE,
                    content = CONTENT,
                    isDialogVisible = true,
                    isCheckboxVisible = true,
                    backIconDescription = BACK_DESCRIPTION,
                    checkboxIconDescription = CHECKBOX_DESCRIPTION,
                    approvalText = APPROVAL_TEXT,
                    onBackIconClick = { backClickCount++ },
                    onDismissRequest = {},
                    onCheckIconClick = { checkClickCount++ }
                )
            }
        }

        onNodeWithText(text = "$TITLE $APPROVAL_TEXT").assertExists()
        onNodeWithContentDescription(CHECKBOX_DESCRIPTION).performClick()

        assertEquals(1, checkClickCount)
        assertEquals(0, backClickCount)
    }

    @Test
    fun showDialog_withCheckboxHidden_showsContentWithoutCheckbox() = runComposeUiTest {
        setContent {
            AppTheme {
                ScrollableContentDialog(
                    title = TITLE,
                    content = CONTENT,
                    isDialogVisible = true,
                    checkboxIconDescription = CHECKBOX_DESCRIPTION,
                    onBackIconClick = {},
                    onDismissRequest = {}
                )
            }
        }

        onNodeWithText(TITLE).assertExists()
        onNodeWithText(CONTENT).assertExists()
        onAllNodesWithContentDescription(CHECKBOX_DESCRIPTION).assertCountEquals(0)
    }

    @Test
    fun showDialog_whenNotVisible_rendersNothing() = runComposeUiTest {
        setContent {
            AppTheme {
                ScrollableContentDialog(
                    title = TITLE,
                    content = CONTENT,
                    isDialogVisible = false,
                    isCheckboxVisible = true,
                    onBackIconClick = {},
                    onDismissRequest = {}
                )
            }
        }

        onAllNodesWithText(TITLE).assertCountEquals(0)
        onAllNodesWithText(CONTENT).assertCountEquals(0)
    }

    private companion object {
        const val TITLE = "Terms of Use"
        const val CONTENT = "Plain paragraph of the terms."
        const val APPROVAL_TEXT = "I have read and approve."
        const val BACK_DESCRIPTION = "Back"
        const val CHECKBOX_DESCRIPTION = "Approval checkbox"
    }
}
