package core.presentation.baseviewmodel

import androidx.compose.material3.Text
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import core.presentation.baseviewmodel.fake.FakeViewModel
import global.deveng.deveng_core.generated.resources.Res
import global.deveng.deveng_core.generated.resources.shared_error
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.jetbrains.compose.resources.stringResource
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class, ExperimentalCoroutinesApi::class)
class StateLayoutTest {
    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

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
        onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate)).assertDoesNotExist()
        onNodeWithText(CONFIRM_TEXT).performClick()

        assertEquals(1, closeRequestCount)
    }

    @Test
    fun errorState_withoutErrorTitle_showsSharedErrorTitle() = runComposeUiTest {
        var sharedErrorTitle = ""
        setContent {
            sharedErrorTitle = stringResource(Res.string.shared_error)
            StateLayout(
                state = State(uiState = EmptyUiState, error = IllegalStateException(), errorDescription = ERROR_DESCRIPTION),
                onCloseError = {}
            ) {
                Text(CONTENT_TEXT)
            }
        }

        onNodeWithText(sharedErrorTitle).assertIsDisplayed()
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
        onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate)).assertIsDisplayed()
    }

    @Test
    fun viewModelError_clickConfirmButton_hidesErrorDialog() = runComposeUiTest {
        val viewModel = FakeViewModel(dispatcher = testDispatcher)
        viewModel.setError(IllegalStateException(ERROR_DESCRIPTION))
        setContent {
            StateLayout(
                state = viewModel.getState(),
                errorTitle = ERROR_TITLE,
                errorPositiveButtonText = CONFIRM_TEXT,
                onCloseError = viewModel::hideError
            ) {
                Text(CONTENT_TEXT)
            }
        }

        onNodeWithText(ERROR_DESCRIPTION).assertIsDisplayed()
        onNodeWithText(CONFIRM_TEXT).performClick()

        onNodeWithText(ERROR_DESCRIPTION).assertDoesNotExist()
    }

    private data object EmptyUiState : UiState

    private companion object {
        const val ERROR_TITLE = "Error"
        const val ERROR_DESCRIPTION = "request failed"
        const val CONFIRM_TEXT = "OK"
        const val CONTENT_TEXT = "Screen content"
    }
}
