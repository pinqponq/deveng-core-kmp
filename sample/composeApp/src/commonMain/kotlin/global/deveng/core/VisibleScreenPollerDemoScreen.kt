package global.deveng.core

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import core.presentation.component.CustomButton
import core.presentation.component.CustomIconButton
import core.presentation.theme.CoreMediumTextStyle
import core.presentation.theme.CoreRegularTextStyle
import deveng_core_kmp.sample.composeapp.generated.resources.Res
import deveng_core_kmp.sample.composeapp.generated.resources.ic_arrow_left

private val DEMO_SCHEDULE_DESCRIPTION =
    "Makes an attempt every $DEMO_INTERVAL after a $DEMO_INITIAL_DELAY wait, at most $DEMO_MAX_ATTEMPTS times."

@Composable
internal fun VisibleScreenPollerDemoScreen(onBack: () -> Unit) {
    val viewModel = remember { VisibleScreenPollerDemoViewModel() }
    val state by viewModel.state.collectAsState()

    DisposableEffect(viewModel) {
        onDispose(viewModel::stopPolling)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color = Color.White)
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 16.dp, top = 72.dp, end = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(space = 16.dp),
        ) {
            Text(text = "Wait for a server job", style = CoreMediumTextStyle().copy(fontSize = 18.sp))
            Text(text = DEMO_SCHEDULE_DESCRIPTION, style = CoreRegularTextStyle().copy(fontSize = 14.sp))
            Text(text = "Status: ${state.pollingStatus.label}", style = CoreRegularTextStyle().copy(fontSize = 14.sp))
            Text(text = "Attempts made: ${state.attemptCount}", style = CoreRegularTextStyle().copy(fontSize = 14.sp))
            Text(text = state.screenVisibility.label, style = CoreRegularTextStyle().copy(fontSize = 14.sp))
            Text(text = state.jobBehavior.label, style = CoreRegularTextStyle().copy(fontSize = 14.sp))
            CustomButton(text = "Start polling", onClick = viewModel::startPolling)
            CustomButton(
                text = state.screenVisibility.toggleButtonText,
                onClick = viewModel::toggleScreenVisibility,
            )

            CustomButton(text = state.jobBehavior.toggleButtonText, onClick = viewModel::toggleJobBehavior)
            CustomButton(text = "Stop", onClick = viewModel::stopPolling)
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
