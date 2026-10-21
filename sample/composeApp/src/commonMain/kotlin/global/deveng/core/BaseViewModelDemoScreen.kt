package global.deveng.core

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import core.presentation.baseviewmodel.BaseViewModel
import core.presentation.baseviewmodel.ErrorDecision
import core.presentation.baseviewmodel.InitEffect
import core.presentation.baseviewmodel.StateLayout
import core.presentation.baseviewmodel.UiState
import core.presentation.baseviewmodel.ViewEvent
import core.presentation.baseviewmodel.ViewModelInterceptor
import core.presentation.baseviewmodel.ViewModelInterceptors
import core.presentation.baseviewmodel.ViewSideEffect
import core.presentation.baseviewmodel.launch
import core.presentation.component.CustomButton
import core.presentation.component.CustomIconButton
import core.presentation.theme.CoreMediumTextStyle
import core.presentation.theme.CoreRegularTextStyle
import deveng_core_kmp.sample.composeapp.generated.resources.Res
import deveng_core_kmp.sample.composeapp.generated.resources.ic_arrow_left
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay

private const val LOAD_DURATION_MILLIS = 1_500L
private const val MAX_VISIBLE_LOG_LINES = 8

private data class BaseViewModelDemoUiState(
    val counterValue: Int = 0,
    val initialLoadCount: Int = 0
) : UiState

private sealed interface BaseViewModelDemoEvent : ViewEvent {
    data object IncreaseCounter : BaseViewModelDemoEvent
    data object Load : BaseViewModelDemoEvent
    data object FailWithDialog : BaseViewModelDemoEvent
    data object FailSilently : BaseViewModelDemoEvent
    data object LoadInitialData : BaseViewModelDemoEvent
}

private sealed interface BaseViewModelDemoEffect : ViewSideEffect

private class BaseViewModelDemoViewModel :
    BaseViewModel<BaseViewModelDemoUiState, BaseViewModelDemoEvent, BaseViewModelDemoEffect>(
        initialState = BaseViewModelDemoUiState()
    ) {

    override fun handleEvents(event: BaseViewModelDemoEvent) {
        when (event) {
            BaseViewModelDemoEvent.IncreaseCounter -> setState { copy(counterValue = counterValue + 1) }
            BaseViewModelDemoEvent.Load -> launch(dispatcher = Dispatchers.Default) {
                delay(LOAD_DURATION_MILLIS)
            }

            BaseViewModelDemoEvent.FailWithDialog -> launch(dispatcher = Dispatchers.Default) {
                error("The request failed")
            }

            BaseViewModelDemoEvent.FailSilently -> launch(dispatcher = Dispatchers.Default, notifyError = false) {
                error("The background refresh failed")
            }

            BaseViewModelDemoEvent.LoadInitialData -> setState { copy(initialLoadCount = initialLoadCount + 1) }
        }
    }
}

private class DemoLogInterceptor(private val logLineList: SnapshotStateList<String>) : ViewModelInterceptor {

    override fun onEvent(viewModelName: String, event: ViewEvent) {
        addLine("event: $event")
    }

    override fun onError(viewModelName: String, exception: Exception): ErrorDecision {
        addLine("error (shown): ${exception.message}")
        return ErrorDecision.SHOW
    }

    override fun onSilentError(viewModelName: String, exception: Exception) {
        addLine("error (silent): ${exception.message}")
    }

    private fun addLine(line: String) {
        logLineList.add(0, line)
        if (logLineList.size > MAX_VISIBLE_LOG_LINES) logLineList.removeAt(logLineList.lastIndex)
    }
}

@Composable
internal fun BaseViewModelDemoScreen(onBack: () -> Unit) {
    val viewModel = remember { BaseViewModelDemoViewModel() }
    val logLineList = remember { mutableStateListOf<String>() }
    val state = viewModel.getState()

    DisposableEffect(Unit) {
        ViewModelInterceptors.register(DemoLogInterceptor(logLineList = logLineList))
        onDispose { ViewModelInterceptors.unregisterAll() }
    }

    InitEffect(viewModel = viewModel) { isInit ->
        if (isInit) viewModel.setEvent(BaseViewModelDemoEvent.LoadInitialData)
    }

    StateLayout(
        state = state,
        onCloseError = viewModel::hideError,
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing),
        backgroundColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, top = 72.dp, end = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(space = 12.dp),
        ) {
            Text(text = "Counter: ${state.uiState.counterValue}", style = CoreMediumTextStyle())
            Text(text = "Initial loads: ${state.uiState.initialLoadCount}", style = CoreRegularTextStyle())

            CustomButton(
                text = "Add one",
                onClick = { viewModel.setEvent(BaseViewModelDemoEvent.IncreaseCounter) }
            )
            CustomButton(
                text = "Load for 1.5 s",
                onClick = { viewModel.setEvent(BaseViewModelDemoEvent.Load) }
            )
            CustomButton(
                text = "Fail with dialog",
                onClick = { viewModel.setEvent(BaseViewModelDemoEvent.FailWithDialog) }
            )
            CustomButton(
                text = "Fail silently",
                onClick = { viewModel.setEvent(BaseViewModelDemoEvent.FailSilently) }
            )

            Text(text = "Interceptor log", style = CoreMediumTextStyle())
            logLineList.forEach { line ->
                Text(text = line, style = CoreRegularTextStyle())
            }
        }

        CustomIconButton(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(all = 16.dp),
            icon = Res.drawable.ic_arrow_left,
            iconDescription = "Back",
            onClick = onBack,
        )
    }
}
