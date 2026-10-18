package core.presentation.motion

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.down
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.test.up
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class PressScaleTest {

    @Test
    fun press_withMotionAllowed_shrinksElement() = runDesktopComposeUiTest {
        setPressableContent(isReduceMotionEnabled = false)

        pressAndSettle()

        assertEquals(PRESSED_SIZE_DP_VALUE, pressableWidthDp(), absoluteTolerance = SIZE_TOLERANCE_DP)
        assertEquals(PRESSED_SIZE_DP_VALUE, pressableHeightDp(), absoluteTolerance = SIZE_TOLERANCE_DP)
    }

    @Test
    fun release_afterPress_returnsToFullSize() = runDesktopComposeUiTest {
        setPressableContent(isReduceMotionEnabled = false)
        pressAndSettle()

        onNodeWithTag(PRESSABLE_TAG).performTouchInput { up() }
        mainClock.advanceTimeBy(SETTLE_TIME_MILLIS)

        assertEquals(FULL_SIZE_DP_VALUE, pressableWidthDp(), absoluteTolerance = SIZE_TOLERANCE_DP)
    }

    @Test
    fun press_withReduceMotion_keepsFullSize() = runDesktopComposeUiTest {
        setPressableContent(isReduceMotionEnabled = true)

        pressAndSettle()

        assertEquals(FULL_SIZE_DP_VALUE, pressableWidthDp(), absoluteTolerance = SIZE_TOLERANCE_DP)
    }

    private fun ComposeUiTest.setPressableContent(isReduceMotionEnabled: Boolean) {
        setContent {
            CompositionLocalProvider(LocalReduceMotion provides isReduceMotionEnabled) {
                val interactionSource = remember { MutableInteractionSource() }
                Box(
                    modifier = Modifier
                        .size(FULL_SIZE_DP.dp)
                        .pressScale(interactionSource = interactionSource, pressedScale = PRESSED_SCALE)
                        .testTag(PRESSABLE_TAG)
                        .clickable(interactionSource = interactionSource, indication = null, onClick = {})
                )
            }
        }
    }

    private fun ComposeUiTest.pressAndSettle() {
        onNodeWithTag(PRESSABLE_TAG).performTouchInput { down(center) }
        mainClock.advanceTimeBy(SETTLE_TIME_MILLIS)
    }

    private fun ComposeUiTest.pressableWidthDp(): Float =
        onNodeWithTag(PRESSABLE_TAG, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot.width / density.density

    private fun ComposeUiTest.pressableHeightDp(): Float =
        onNodeWithTag(PRESSABLE_TAG, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot.height / density.density

    private companion object {
        const val PRESSABLE_TAG = "pressable"
        const val FULL_SIZE_DP = 100
        const val FULL_SIZE_DP_VALUE = 100f
        const val PRESSED_SCALE = 0.8f
        const val PRESSED_SIZE_DP_VALUE = 80f
        const val SIZE_TOLERANCE_DP = 0.5f
        const val SETTLE_TIME_MILLIS = 1_000L
    }
}
