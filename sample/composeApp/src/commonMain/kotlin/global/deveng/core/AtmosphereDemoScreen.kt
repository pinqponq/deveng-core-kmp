package global.deveng.core

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.getValue
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
import core.presentation.component.LabeledSwitch
import core.presentation.effect.SwirlBackground
import core.presentation.effect.SwirlPalette
import core.presentation.effect.VortexFlash
import core.presentation.effect.crtScreen
import core.presentation.theme.CoreMediumTextStyle
import kotlinx.coroutines.launch

private const val VORTEX_MILLIS = 700

private val demoPalette = SwirlPalette(
    base = Color(0xFF2A1B4D),
    accent = Color(0xFFFFC83D),
    shade = Color(0xFF7C3AED)
)

/** Shows the CRT screen look, the swirl background and the vortex flash; the switch turns the CRT look on and off. */
@Composable
internal fun AtmosphereDemoScreen(onBack: () -> Unit) {
    var isCrtEnabled by remember { mutableStateOf(true) }
    val vortexProgress = remember { Animatable(initialValue = 0f) }
    val scope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize().then(if (isCrtEnabled) Modifier.crtScreen() else Modifier)) {
        SwirlBackground(palette = demoPalette)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(all = 16.dp),
            verticalArrangement = Arrangement.spacedBy(space = 16.dp, alignment = Alignment.CenterVertically)
        ) {
            Text(
                text = "Atmosphere: CRT, swirl, vortex",
                color = Color.White,
                style = CoreMediumTextStyle().copy(fontSize = 20.sp)
            )
            LabeledSwitch(
                label = "CRT screen look",
                isChecked = isCrtEnabled,
                onSwitchClick = { isChecked -> isCrtEnabled = isChecked }
            )
            CustomButton(
                text = "Play vortex flash",
                onClick = {
                    scope.launch {
                        vortexProgress.snapTo(targetValue = 0f)
                        vortexProgress.animateTo(targetValue = 1f, animationSpec = tween(durationMillis = VORTEX_MILLIS))
                    }
                }
            )
            CustomButton(text = "Back", onClick = onBack)
        }

        VortexFlash(progress = vortexProgress.value, palette = demoPalette)
    }
}
