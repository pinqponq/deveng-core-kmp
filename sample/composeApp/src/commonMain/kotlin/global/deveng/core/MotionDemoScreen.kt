package global.deveng.core

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
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
import core.presentation.component.CustomIconButton
import core.presentation.component.LabeledSwitch
import core.presentation.component.shimmer.ShimmerBox
import core.presentation.haptics.rememberAppHaptics
import core.presentation.motion.LocalReduceMotion
import core.presentation.motion.NavTransitions
import core.presentation.motion.pressScale
import core.presentation.theme.CoreMediumTextStyle
import deveng_core_kmp.sample.composeapp.generated.resources.Res
import deveng_core_kmp.sample.composeapp.generated.resources.ic_arrow_left

private const val MOTION_DEMO_PAGE_COUNT = 3
private const val MOTION_DEMO_SHIMMER_ROW_COUNT = 3

/**
 * Playground for core motion: press feedback, shimmer placeholders, screen transitions, haptics and reveal motion (pop, flip card, count-up).
 * The switch forces reduce motion on for this screen, on top of the system setting.
 */
@Composable
internal fun MotionDemoScreen(onBack: () -> Unit) {
    val isSystemReduceMotionEnabled = LocalReduceMotion.current
    var isReduceMotionOverridden by remember { mutableStateOf(false) }
    val isReduceMotionEnabled = isSystemReduceMotionEnabled || isReduceMotionOverridden

    CompositionLocalProvider(LocalReduceMotion provides isReduceMotionEnabled) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(color = Color.White)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .verticalScroll(rememberScrollState())
                .padding(all = 16.dp),
            verticalArrangement = Arrangement.spacedBy(space = 16.dp),
        ) {
            CustomIconButton(
                icon = Res.drawable.ic_arrow_left,
                iconDescription = "Back",
                onClick = onBack,
            )

            Text(
                text = "System reduce motion: ${if (isSystemReduceMotionEnabled) "on" else "off"}",
                style = CoreMediumTextStyle().copy(fontSize = 16.sp),
            )

            LabeledSwitch(
                label = "Reduce motion on this screen",
                isChecked = isReduceMotionOverridden,
                onSwitchClick = { isChecked -> isReduceMotionOverridden = isChecked },
            )

            MotionDemoPressCard()

            MotionDemoHapticButtons()

            MotionDemoShimmer()

            MotionRevealDemo()

            MotionDemoTransitions(isReduceMotionEnabled = isReduceMotionEnabled)
        }
    }
}

@Composable
private fun MotionDemoPressCard() {
    val appHaptics = rememberAppHaptics()
    val interactionSource = remember { MutableInteractionSource() }
    var pressCount by remember { mutableStateOf(0) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .pressScale(interactionSource = interactionSource)
            .clip(RoundedCornerShape(16.dp))
            .background(color = Color(0xFF1976D2))
            .clickable(interactionSource = interactionSource, indication = null) {
                pressCount++
                appHaptics.tap()
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Press me (pressScale + tap haptic): $pressCount",
            color = Color.White,
            style = CoreMediumTextStyle().copy(fontSize = 16.sp),
        )
    }
}

@Composable
private fun MotionDemoHapticButtons() {
    val appHaptics = rememberAppHaptics()

    Row(horizontalArrangement = Arrangement.spacedBy(space = 8.dp)) {
        CustomButton(
            modifier = Modifier.weight(1f),
            text = "Success haptic",
            containerColor = Color(0xFF2E7D32),
            onClick = { appHaptics.success() },
        )

        CustomButton(
            modifier = Modifier.weight(1f),
            text = "Warning haptic",
            containerColor = Color(0xFFC62828),
            onClick = { appHaptics.warning() },
        )
    }
}

@Composable
private fun MotionDemoShimmer() {
    Column(verticalArrangement = Arrangement.spacedBy(space = 8.dp)) {
        Text(text = "ShimmerBox", style = CoreMediumTextStyle().copy(fontSize = 16.sp))
        repeat(MOTION_DEMO_SHIMMER_ROW_COUNT) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(space = 12.dp),
            ) {
                ShimmerBox(modifier = Modifier.size(48.dp), cornerRadius = 24.dp)
                Column(verticalArrangement = Arrangement.spacedBy(space = 6.dp)) {
                    ShimmerBox(modifier = Modifier.fillMaxWidth().height(14.dp))
                    ShimmerBox(modifier = Modifier.fillMaxWidth(fraction = 0.6f).height(14.dp))
                }
            }
        }
    }
}

@Composable
private fun MotionDemoTransitions(isReduceMotionEnabled: Boolean) {
    var currentPage by remember { mutableStateOf(1) }
    var isBackNavigation by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(space = 8.dp)) {
        Text(text = "NavTransitions", style = CoreMediumTextStyle().copy(fontSize = 16.sp))

        AnimatedContent(
            targetState = currentPage,
            transitionSpec = {
                if (isBackNavigation) {
                    NavTransitions.popEnter(isReduceMotionEnabled) togetherWith
                        NavTransitions.popExit(isReduceMotionEnabled)
                } else {
                    NavTransitions.enter(isReduceMotionEnabled) togetherWith
                        NavTransitions.exit(isReduceMotionEnabled)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .clip(RoundedCornerShape(16.dp)),
        ) { page ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color = Color(0xFF7C3AED)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Page $page",
                    color = Color.White,
                    style = CoreMediumTextStyle().copy(fontSize = 24.sp),
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(space = 8.dp)) {
            CustomButton(
                modifier = Modifier.weight(1f),
                text = "Back",
                enabled = currentPage > 1,
                onClick = {
                    isBackNavigation = true
                    currentPage--
                },
            )

            CustomButton(
                modifier = Modifier.weight(1f),
                text = "Next",
                enabled = currentPage < MOTION_DEMO_PAGE_COUNT,
                onClick = {
                    isBackNavigation = false
                    currentPage++
                },
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
