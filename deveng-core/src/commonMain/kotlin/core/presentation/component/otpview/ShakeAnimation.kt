package core.presentation.component.otpview

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember

@Composable
fun rememberShakeOffset(trigger: Boolean): Float {
    val offsetX = remember { Animatable(0f) }

    LaunchedEffect(trigger) {
        if (trigger) {
            offsetX.animateTo(
                targetValue = 0f,
                animationSpec = keyframes {
                    durationMillis = 280
                    0f at 0
                    20f at 80
                    -20f at 120
                    10f at 160
                    -10f at 200
                    5f at 240
                    0f at 280
                }
            )
        }
    }

    return offsetX.value
}
