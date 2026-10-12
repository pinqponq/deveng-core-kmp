package core.presentation.component.scrollablecontentdialog

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import core.presentation.theme.AppTheme
import core.presentation.theme.DefaultComponentTheme
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

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

    @Test
    fun checkApproval_whenChecked_showsFilledCheckbox() = runComposeUiTest {
        var isChecked by mutableStateOf(false)
        setContent {
            AppTheme {
                ScrollableContentDialog(
                    title = TITLE,
                    content = CONTENT,
                    isChecked = isChecked,
                    isDialogVisible = true,
                    isCheckboxVisible = true,
                    checkboxIconDescription = CHECKBOX_DESCRIPTION,
                    onBackIconClick = {},
                    onDismissRequest = {}
                )
            }
        }
        val uncheckedInk = checkboxInkPixelCount()

        isChecked = true
        waitForIdle()

        assertTrue(checkboxInkPixelCount() > uncheckedInk)
    }

    private fun ComposeUiTest.checkboxInkPixelCount(): Int {
        val pixels = onNodeWithContentDescription(CHECKBOX_DESCRIPTION).captureToImage().toPixelMap()
        val colorCounts = buildMap<Color, Int> {
            for (x in 0 until pixels.width) for (y in 0 until pixels.height) merge(pixels[x, y], 1, Int::plus)
        }
        val background = colorCounts.maxBy { (_, count) -> count }.key
        val tint = DefaultComponentTheme.scrollableContentDialog.checkboxIconTint
        return colorCounts.filterKeys { color -> color != background && color.isNear(tint) }.values.sum()
    }

    private fun Color.isNear(other: Color): Boolean =
        abs(red - other.red) < TINT_TOLERANCE && abs(green - other.green) < TINT_TOLERANCE && abs(blue - other.blue) < TINT_TOLERANCE

    private companion object {
        const val TINT_TOLERANCE = 0.1f
        const val TITLE = "Terms of Use"
        const val CONTENT = "Plain paragraph of the terms."
        const val APPROVAL_TEXT = "I have read and approve."
        const val BACK_DESCRIPTION = "Back"
        const val CHECKBOX_DESCRIPTION = "Approval checkbox"
    }
}
