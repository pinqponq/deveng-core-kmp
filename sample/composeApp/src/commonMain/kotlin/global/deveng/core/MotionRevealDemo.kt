package global.deveng.core

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import core.presentation.component.CustomButton
import core.presentation.haptics.rememberHapticTicker
import core.presentation.motion.CountUpText
import core.presentation.motion.FlipCard
import core.presentation.motion.juice
import core.presentation.motion.rememberJuiceState
import core.presentation.theme.CoreMediumTextStyle

private const val REVEAL_DEMO_COUNT_STEP = 237
private const val REVEAL_DEMO_FLIP_STAGGER_MILLIS = 80
private const val REVEAL_DEMO_CARD_COUNT = 2

/**
 * Reveal motion: pop ([juice]), [FlipCard], [CountUpText] and the haptic ticker.
 */
@Composable
internal fun MotionRevealDemo() {
    Column(verticalArrangement = Arrangement.spacedBy(space = 16.dp)) {
        Text(text = "Reveal motion", style = CoreMediumTextStyle().copy(fontSize = 16.sp))

        MotionRevealJuiceDemo()

        MotionRevealFlipDemo()

        MotionRevealCountUpDemo()
    }
}

@Composable
private fun MotionRevealJuiceDemo() {
    val juiceState = rememberJuiceState(peakRotationDegrees = 3f)
    val hapticTicker = rememberHapticTicker()

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(space = 12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .juice(juiceState)
                .clip(RoundedCornerShape(12.dp))
                .background(color = Color(0xFFFFC964)),
        )

        CustomButton(
            modifier = Modifier.weight(1f),
            text = "Pop (juice + medium haptic)",
            onClick = {
                juiceState.pop()
                hapticTicker.medium()
            },
        )
    }
}

@Composable
private fun MotionRevealFlipDemo() {
    var isFlipped by remember { mutableStateOf(false) }
    val hapticTicker = rememberHapticTicker()

    Column(verticalArrangement = Arrangement.spacedBy(space = 8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(space = 12.dp)) {
            repeat(REVEAL_DEMO_CARD_COUNT) { cardIndex ->
                FlipCard(
                    modifier = Modifier
                        .weight(1f)
                        .height(140.dp),
                    isFlipped = isFlipped,
                    delayMillis = cardIndex * REVEAL_DEMO_FLIP_STAGGER_MILLIS,
                    front = { RevealDemoFace(text = "Front ${cardIndex + 1}", color = Color(0xFF7C3AED)) },
                    back = { RevealDemoFace(text = "Back", color = Color(0xFF1F1F1F)) },
                )
            }
        }

        CustomButton(
            modifier = Modifier.fillMaxWidth(),
            text = if (isFlipped) "Flip back" else "Flip both cards",
            onClick = {
                isFlipped = !isFlipped
                hapticTicker.strong()
            },
        )
    }
}

@Composable
private fun RevealDemoFace(text: String, color: Color) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(16.dp))
            .background(color = color),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, color = Color.White, style = CoreMediumTextStyle().copy(fontSize = 18.sp))
    }
}

@Composable
private fun MotionRevealCountUpDemo() {
    var targetValue by remember { mutableIntStateOf(0) }
    val juiceState = rememberJuiceState()
    val hapticTicker = rememberHapticTicker()

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(space = 12.dp),
    ) {
        CountUpText(
            modifier = Modifier.juice(juiceState),
            value = targetValue,
            style = CoreMediumTextStyle().copy(fontSize = 32.sp),
            formatter = { shownValue -> "$shownValue km" },
            juiceState = juiceState,
            onStep = { hapticTicker.light() },
        )

        CustomButton(
            modifier = Modifier.weight(1f),
            text = "Count up",
            onClick = { targetValue += REVEAL_DEMO_COUNT_STEP },
        )
    }
}
