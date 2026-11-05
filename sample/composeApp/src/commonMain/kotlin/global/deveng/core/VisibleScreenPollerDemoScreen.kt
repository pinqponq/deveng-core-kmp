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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import core.presentation.component.CustomButton
import core.presentation.component.CustomIconButton
import core.presentation.theme.CoreMediumTextStyle
import core.presentation.theme.CoreRegularTextStyle
import core.util.polling.PollAttemptOutcome
import core.util.polling.PollingSchedule
import core.util.polling.VisibleScreenPoller
import deveng_core_kmp.sample.composeapp.generated.resources.Res
import deveng_core_kmp.sample.composeapp.generated.resources.ic_arrow_left
import kotlin.time.Duration.Companion.seconds

private const val DEMO_MAX_ATTEMPTS = 10
private const val DEMO_ATTEMPTS_UNTIL_FINISHED = 6
private val DEMO_INTERVAL = 2.seconds
private val DEMO_INITIAL_DELAY = 1.seconds

@Composable
internal fun VisibleScreenPollerDemoScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val poller = remember { VisibleScreenPoller() }
    var attemptCount by remember { mutableIntStateOf(0) }
    var isScreenVisible by remember { mutableStateOf(true) }
    var statusText by remember { mutableStateOf("Idle") }

    DisposableEffect(poller) {
        poller.changeVisibility(isVisible = true)
        onDispose { poller.stop() }
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
            Text(
                text = "Checks every 2 s after a 1 s wait, at most 10 times. The job is done on check 6.",
                style = CoreRegularTextStyle().copy(fontSize = 14.sp),
            )
            Text(text = "Status: $statusText", style = CoreRegularTextStyle().copy(fontSize = 14.sp))
            Text(text = "Checks made: $attemptCount", style = CoreRegularTextStyle().copy(fontSize = 14.sp))
            Text(
                text = if (isScreenVisible) "Screen: visible" else "Screen: hidden (paused)",
                style = CoreRegularTextStyle().copy(fontSize = 14.sp),
            )
            CustomButton(
                text = "Start polling",
                onClick = {
                    attemptCount = 0
                    statusText = "Polling"
                    poller.start(
                        scope = scope,
                        schedule = PollingSchedule.fixedInterval(
                            interval = DEMO_INTERVAL,
                            maxAttempts = DEMO_MAX_ATTEMPTS,
                            initialDelay = DEMO_INITIAL_DELAY,
                        ),
                        pollAttempt = {
                            attemptCount++
                            if (attemptCount >= DEMO_ATTEMPTS_UNTIL_FINISHED) {
                                statusText = "Finished"
                                PollAttemptOutcome.FINISHED
                            } else {
                                PollAttemptOutcome.CONTINUE
                            }
                        },
                        onScheduleExhausted = { statusText = "Schedule ran out" },
                    )
                },
            )
            CustomButton(
                text = if (isScreenVisible) "Hide screen" else "Show screen",
                onClick = {
                    isScreenVisible = !isScreenVisible
                    poller.changeVisibility(isVisible = isScreenVisible)
                },
            )
            CustomButton(
                text = "Stop",
                onClick = {
                    poller.stop()
                    statusText = "Stopped"
                },
            )
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
