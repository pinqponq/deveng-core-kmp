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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import core.presentation.component.CustomButton
import core.presentation.component.CustomIconButton
import core.presentation.theme.CoreMediumTextStyle
import core.presentation.theme.CoreRegularTextStyle
import core.util.CooldownTimer
import deveng_core_kmp.sample.composeapp.generated.resources.Res
import deveng_core_kmp.sample.composeapp.generated.resources.ic_arrow_left

private const val COOLDOWN_SECONDS = 10

@Composable
internal fun CooldownTimerDemoScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val timer = remember { CooldownTimer(scope = scope) }
    val remainingSeconds by timer.remainingSeconds.collectAsState()
    val isRunning by timer.isRunning.collectAsState()

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
            Text(text = "Resend code", style = CoreMediumTextStyle().copy(fontSize = 18.sp))
            Text(
                text = if (isRunning) "Resend available in (${remainingSeconds}s)" else "Ready to resend",
                style = CoreRegularTextStyle().copy(fontSize = 14.sp),
            )
            CustomButton(
                text = "Send code",
                enabled = !isRunning,
                onClick = { timer.start(seconds = COOLDOWN_SECONDS) },
            )
            CustomButton(
                text = "Cancel",
                enabled = isRunning,
                onClick = timer::cancel,
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
