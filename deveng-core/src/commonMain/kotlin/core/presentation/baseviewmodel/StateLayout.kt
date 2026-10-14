package core.presentation.baseviewmodel

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import core.presentation.component.alertdialog.CustomAlertDialog
import global.deveng.deveng_core.generated.resources.Res
import global.deveng.deveng_core.generated.resources.shared_error
import org.jetbrains.compose.resources.stringResource

/**
 * Screen container for a [BaseViewModel] state: shows the content, a progress indicator while
 * loading and the error dialog while [State.showError] is true.
 *
 * @param state The screen state from [BaseViewModel.getState].
 * @param onCloseError Callback invoked when the error dialog is dismissed; usually calls [BaseViewModel.hideError].
 * @param errorTitle Title of the error dialog.
 * @param modifier Modifier to be applied to the container.
 * @param hideContentWhileLoading Whether the content is hidden while [State.isLoading] is true.
 * @param backgroundColor Background color of the container.
 * @param errorPositiveButtonText Optional text for the error dialog's confirm button.
 * @param content The screen content.
 */
@Composable
fun <T : UiState> StateLayout(
    state: State<T>,
    onCloseError: () -> Unit,
    errorTitle: String = stringResource(Res.string.shared_error),
    modifier: Modifier = Modifier.fillMaxSize(),
    hideContentWhileLoading: Boolean = false,
    backgroundColor: Color = MaterialTheme.colorScheme.background,
    errorPositiveButtonText: String? = null,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier.background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        if (!hideContentWhileLoading || !state.isLoading) {
            content()
        }

        if (state.isLoading) {
            CircularProgressIndicator()
        }

        CustomAlertDialog(
            isDialogVisible = state.showError,
            title = errorTitle,
            description = state.errorDescription.orEmpty(),
            positiveButtonText = errorPositiveButtonText,
            onPositiveButtonClick = onCloseError,
            onDismissRequest = onCloseError
        )
    }
}
