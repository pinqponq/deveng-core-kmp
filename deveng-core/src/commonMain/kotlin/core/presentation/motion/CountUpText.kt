package core.presentation.motion

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.min

/**
 * A number that counts to [value] one step at a time instead of jumping. Every step calls
 * [onStep] (for a tick sound or a haptic) and pops [juiceState]. A large change is spread over at
 * most [maxStepCount] steps and [maxTotalMillis] in total. While reduce motion is on, the number
 * changes at once and no step callback runs.
 *
 * @param value The number to show when the count ends.
 * @param modifier Modifier for the text.
 * @param startValue The number shown first. The text counts from it to [value] when it appears.
 * @param style Text style.
 * @param color Text color.
 * @param stepDelayMillis Time between two steps.
 * @param maxStepCount Most steps for one change.
 * @param maxTotalMillis Most time for one change; the delay between steps shrinks to fit.
 * @param formatter Turns the shown number into text.
 * @param juiceState Optional state that pops on every step.
 * @param onStep Called with the number shown at each step.
 */
@Composable
fun CountUpText(
    value: Int,
    modifier: Modifier = Modifier,
    startValue: Int = 0,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    stepDelayMillis: Long = MotionDefaults.COUNT_UP_STEP_DELAY_MILLIS,
    maxStepCount: Int = MotionDefaults.COUNT_UP_MAX_STEP_COUNT,
    maxTotalMillis: Long = MotionDefaults.COUNT_UP_MAX_TOTAL_MILLIS,
    formatter: (Int) -> String = Int::toString,
    juiceState: JuiceState? = null,
    onStep: (Int) -> Unit = {}
) {
    val isReduceMotionEnabled = LocalReduceMotion.current
    val currentOnStep by rememberUpdatedState(onStep)
    var shownValue by remember { mutableIntStateOf(startValue) }

    LaunchedEffect(value, isReduceMotionEnabled) {
        if (isReduceMotionEnabled) {
            shownValue = value
            return@LaunchedEffect
        }
        val stepValues = countUpValues(
            startValue = shownValue,
            endValue = value,
            maxStepCount = maxStepCount
        )
        val delayMillis = countUpStepDelayMillis(
            stepCount = stepValues.size,
            stepDelayMillis = stepDelayMillis,
            maxTotalMillis = maxTotalMillis
        )
        stepValues.forEachIndexed { stepIndex, stepValue ->
            if (stepIndex > 0) delay(delayMillis)
            shownValue = stepValue
            juiceState?.pop()
            currentOnStep(stepValue)
        }
    }

    Text(
        text = formatter(shownValue),
        modifier = modifier,
        style = style,
        color = color
    )
}

internal fun countUpValues(startValue: Int, endValue: Int, maxStepCount: Int): List<Int> {
    val distance = endValue.toLong() - startValue
    if (distance == 0L) return emptyList()
    val stepCount = min(abs(distance), maxStepCount.toLong()).toInt()
    return (1..stepCount).map { stepNumber -> (startValue + distance * stepNumber / stepCount).toInt() }
}

internal fun countUpStepDelayMillis(stepCount: Int, stepDelayMillis: Long, maxTotalMillis: Long): Long {
    if (stepCount == 0) return 0L
    return min(stepDelayMillis, maxTotalMillis / stepCount)
}
