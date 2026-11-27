package core.presentation.baseviewmodel

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.CoroutineScope

/**
 * Runs [block] when the screen enters composition, telling it whether the initial load still has to
 * happen.
 *
 * The flag comes from [viewModel] instead of from saved state, so it shares the lifetime of the
 * data it guards. Re-entering a screen whose ViewModel is still alive skips the initial load, while
 * a screen whose ViewModel was recreated — after process death, or after the system dropped it
 * while the app sat in the background — initializes again instead of coming back empty.
 *
 * @param viewModel The screen's ViewModel; its lifetime decides whether the initial load is due.
 * @param block Runs when the screen enters composition; `isInit` is true only for the first time.
 */
@Composable
fun InitEffect(
    viewModel: BaseViewModel<*, *, *>,
    block: suspend CoroutineScope.(isInit: Boolean) -> Unit
) {
    LaunchedEffect(viewModel) {
        block(viewModel.markInitialLoadStarted())
    }
}
