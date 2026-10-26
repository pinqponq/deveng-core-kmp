package core.presentation.component.scrollablecontentdialog

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import core.presentation.theme.AppTheme
import core.presentation.theme.ComponentTheme
import core.presentation.theme.DefaultComponentTheme
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class ScrollableContentDialogThemeTest {

    @Test
    fun showDialog_withCustomLayoutTheme_movesHeaderContentAndApprovalRowByThemeValues() = runComposeUiTest {
        var componentTheme by mutableStateOf(DefaultComponentTheme)
        showDialog(title = SHORT_TITLE, componentTheme = { componentTheme })
        val defaultLayout = measureLayout(title = SHORT_TITLE)

        componentTheme = CUSTOM_THEME
        waitForIdle()
        val customLayout = measureLayout(title = SHORT_TITLE)

        val defaultTheme = DefaultComponentTheme.scrollableContentDialog
        val customTheme = CUSTOM_THEME.scrollableContentDialog
        val headerTopPaddingChange = customTheme.headerTopPadding - defaultTheme.headerTopPadding
        val headerBottomPaddingChange = customTheme.headerBottomPadding - defaultTheme.headerBottomPadding
        assertDpEquals(customTheme.backIconWidth, customLayout.backIconWidth)
        assertDpEquals(customTheme.backIconHeight, customLayout.backIconHeight)
        assertDpEquals(headerTopPaddingChange, customLayout.backButtonTop - defaultLayout.backButtonTop)
        assertDpEquals(
            headerTopPaddingChange + headerBottomPaddingChange,
            customLayout.contentTop - defaultLayout.contentTop
        )
        assertDpEquals(
            customTheme.contentHorizontalPadding - defaultTheme.contentHorizontalPadding,
            customLayout.contentLeft - defaultLayout.contentLeft
        )
        assertDpEquals(
            customTheme.approvalRowTopPadding - defaultTheme.approvalRowTopPadding,
            customLayout.checkboxGapBelowContent - defaultLayout.checkboxGapBelowContent
        )
    }

    @Test
    fun showDialog_withCustomTitlePadding_narrowsTitleByThemeValue() = runComposeUiTest {
        var componentTheme by mutableStateOf(DefaultComponentTheme)
        showDialog(title = LONG_TITLE, componentTheme = { componentTheme })
        val defaultTitleWidth = onNodeWithText(LONG_TITLE).getUnclippedBoundsInRoot().width

        componentTheme = CUSTOM_THEME
        waitForIdle()
        val customTitleWidth = onNodeWithText(LONG_TITLE).getUnclippedBoundsInRoot().width

        val titlePaddingChange = CUSTOM_THEME.scrollableContentDialog.titleHorizontalPadding -
            DefaultComponentTheme.scrollableContentDialog.titleHorizontalPadding
        assertDpEquals(titlePaddingChange * 2, defaultTitleWidth - customTitleWidth)
    }

    @Test
    fun scrollToEnd_withCustomBottomPaddings_leavesThemeSpaceBelowApprovalRow() = runComposeUiTest {
        var componentTheme by mutableStateOf(DefaultComponentTheme)
        showDialog(title = SHORT_TITLE, content = LONG_CONTENT, componentTheme = { componentTheme })
        val defaultGap = measureGapBelowCheckboxAtScrollEnd()

        componentTheme = CUSTOM_THEME
        waitForIdle()
        val customGap = measureGapBelowCheckboxAtScrollEnd()

        val defaultTheme = DefaultComponentTheme.scrollableContentDialog
        val customTheme = CUSTOM_THEME.scrollableContentDialog
        assertDpEquals(
            (customTheme.approvalRowBottomPadding - defaultTheme.approvalRowBottomPadding) +
                (customTheme.contentBottomPadding - defaultTheme.contentBottomPadding),
            customGap - defaultGap
        )
    }

    private fun ComposeUiTest.measureGapBelowCheckboxAtScrollEnd(): Dp {
        onNode(hasScrollAction()).performSemanticsAction(SemanticsActions.ScrollBy) { scrollBy -> scrollBy(0f, SCROLL_TO_END_PX) }
        waitForIdle()
        val viewportBottom = onNode(hasScrollAction()).getUnclippedBoundsInRoot().bottom
        val checkboxBottom = onNodeWithContentDescription(CHECKBOX_DESCRIPTION).getUnclippedBoundsInRoot().bottom
        return viewportBottom - checkboxBottom
    }

    private fun ComposeUiTest.showDialog(
        title: String,
        content: String = CONTENT,
        componentTheme: () -> ComponentTheme
    ) {
        setContent {
            AppTheme(darkTheme = false, componentTheme = componentTheme()) {
                ScrollableContentDialog(
                    title = title,
                    content = content,
                    isDialogVisible = true,
                    isCheckboxVisible = true,
                    backIconDescription = BACK_DESCRIPTION,
                    checkboxIconDescription = CHECKBOX_DESCRIPTION,
                    approvalText = APPROVAL_TEXT,
                    onBackIconClick = {},
                    onDismissRequest = {}
                )
            }
        }
        waitForIdle()
    }

    private fun ComposeUiTest.measureLayout(title: String): DialogLayout {
        val backIconBounds = onNodeWithContentDescription(BACK_DESCRIPTION, useUnmergedTree = true).getUnclippedBoundsInRoot()
        val contentBounds = onNodeWithText(CONTENT).getUnclippedBoundsInRoot()
        val checkboxBounds = onNodeWithContentDescription(CHECKBOX_DESCRIPTION).getUnclippedBoundsInRoot()
        return DialogLayout(
            backIconWidth = backIconBounds.width,
            backIconHeight = backIconBounds.height,
            backButtonTop = onNodeWithContentDescription(BACK_DESCRIPTION).getUnclippedBoundsInRoot().top,
            contentTop = contentBounds.top,
            contentLeft = contentBounds.left,
            checkboxGapBelowContent = checkboxBounds.top - contentBounds.bottom
        )
    }

    private fun assertDpEquals(expected: Dp, actual: Dp) {
        assertTrue(abs(expected.value - actual.value) <= DP_TOLERANCE, "expected $expected but was $actual")
    }

    private data class DialogLayout(
        val backIconWidth: Dp,
        val backIconHeight: Dp,
        val backButtonTop: Dp,
        val contentTop: Dp,
        val contentLeft: Dp,
        val checkboxGapBelowContent: Dp
    )

    private companion object {
        const val DP_TOLERANCE = 0.5f
        const val SHORT_TITLE = "Terms"
        const val LONG_TITLE = "Terms of use, privacy notice and the rules for sharing photos with your partner"
        const val CONTENT = "These terms explain how the app stores your photos."
        const val SCROLL_TO_END_PX = 100_000f
        val LONG_CONTENT = List(size = 40) { index -> "Paragraph $index explains how the app stores your photos." }
            .joinToString(separator = "\n\n")
        const val BACK_DESCRIPTION = "Back"
        const val CHECKBOX_DESCRIPTION = "Approve"
        const val APPROVAL_TEXT = "I have read and approve."
        val CUSTOM_THEME = DefaultComponentTheme.copy(
            scrollableContentDialog = DefaultComponentTheme.scrollableContentDialog.copy(
                headerTopPadding = 33.dp,
                headerBottomPadding = 24.dp,
                backIconWidth = 26.dp,
                backIconHeight = 22.dp,
                titleHorizontalPadding = 60.dp,
                contentHorizontalPadding = 32.dp,
                approvalRowTopPadding = 27.dp,
                approvalRowBottomPadding = 60.dp,
                contentBottomPadding = 21.dp
            )
        )
    }
}
