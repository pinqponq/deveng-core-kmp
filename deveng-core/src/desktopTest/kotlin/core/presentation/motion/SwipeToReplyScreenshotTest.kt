package core.presentation.motion

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import core.presentation.theme.AppTheme
import core.testing.SCREENSHOT_DENSITY
import core.testing.screenshotPath
import io.github.takahirom.roborazzi.captureRoboImage
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class SwipeToReplyScreenshotTest {

    @Test
    fun swipeToReply_heldPastTrigger_matchesBaseline() = runDesktopComposeUiTest(width = CANVAS_WIDTH_PX, height = CANVAS_HEIGHT_PX) {
        setContent {
            CompositionLocalProvider(LocalDensity provides Density(density = SCREENSHOT_DENSITY)) {
                AppTheme(darkTheme = false) {
                    Box(modifier = Modifier.fillMaxWidth().background(Color.White).padding(16.dp)) {
                        val swipeState = rememberSwipeToReplyState()
                        Text(
                            text = "Message",
                            modifier = Modifier
                                .swipeToReply(state = swipeState, onTrigger = {})
                                .testTag(ROW_TAG)
                                .background(color = BUBBLE_COLOR, shape = RoundedCornerShape(18.dp))
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        )
                    }
                }
            }
        }

        onNodeWithTag(ROW_TAG).performTouchInput {
            down(center)
            repeat(MOVE_STEP_COUNT) { moveBy(Offset(HELD_SWIPE_PX / MOVE_STEP_COUNT, 0f)) }
        }

        onRoot().captureRoboImage(filePath = screenshotPath(name = "swipe_to_reply_held_past_trigger"))
    }

    private companion object {
        const val ROW_TAG = "swipeRow"
        const val CANVAS_WIDTH_PX = 600
        const val CANVAS_HEIGHT_PX = 160
        const val HELD_SWIPE_PX = 300f
        const val MOVE_STEP_COUNT = 10
        val BUBBLE_COLOR = Color(0xFFEDEDED)
    }
}
