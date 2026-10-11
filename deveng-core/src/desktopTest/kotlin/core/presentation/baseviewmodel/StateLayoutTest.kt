package core.presentation.baseviewmodel

import androidx.compose.material3.Text
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class StateLayoutTest {

    @Test
    fun errorState_clickConfirmButton_showsErrorAndRequestsClose() = runComposeUiTest {
        var closeRequestCount = 0
        setContent {
            StateLayout(
                state = State(uiState = EmptyUiState, error = IllegalStateException(), errorDescription = ERROR_DESCRIPTION),
                errorTitle = ERROR_TITLE,
                errorPositiveButtonText = CONFIRM_TEXT,
                onCloseError = { closeRequestCount++ }
            ) {
                Text(CONTENT_TEXT)
            }
        }

        onNodeWithText(ERROR_TITLE).assertIsDisplayed()
        onNodeWithText(ERROR_DESCRIPTION).assertIsDisplayed()
        onNodeWithText(CONFIRM_TEXT).performClick()

        assertEquals(1, closeRequestCount)
    }

    @Test
    fun loadingState_withHideContentWhileLoading_hidesContent() = runComposeUiTest {
        setContent {
            StateLayout(
                state = State(uiState = EmptyUiState, isLoading = true),
                errorTitle = ERROR_TITLE,
                hideContentWhileLoading = true,
                onCloseError = {}
            ) {
                Text(CONTENT_TEXT)
            }
        }

        onNodeWithText(CONTENT_TEXT).assertDoesNotExist()
        onNodeWithText(ERROR_TITLE).assertDoesNotExist()
    }

    @Test
    fun loadingState_withoutHideContentWhileLoading_keepsContent() = runComposeUiTest {
        setContent {
            StateLayout(
                state = State(uiState = EmptyUiState, isLoading = true),
                errorTitle = ERROR_TITLE,
                onCloseError = {}
            ) {
                Text(CONTENT_TEXT)
            }
        }

        onNodeWithText(CONTENT_TEXT).assertIsDisplayed()
    }

    private data object EmptyUiState : UiState

    private companion object {
        const val ERROR_TITLE = "Error"
        const val ERROR_DESCRIPTION = "request failed"
        const val CONFIRM_TEXT = "OK"
        const val CONTENT_TEXT = "Screen content"
    }
}
