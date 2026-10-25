package core.presentation.motion

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.TouchInjectionScope
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import core.presentation.haptics.fake.FakeHapticFeedback
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class SwipeToReplyTest {

    private val hapticFeedback = FakeHapticFeedback()
    private var triggerCount = 0

    @Test
    fun swipe_pastTrigger_callsOnTriggerOnceAndPlaysOneTap() = runDesktopComposeUiTest {
        setSwipeableContent()

        onNodeWithTag(ROW_TAG).performTouchInput { swipeSideways(LONG_SWIPE_PX) }
        waitForIdle()

        assertEquals(1, triggerCount)
        assertEquals(listOf(HapticFeedbackType.VirtualKey), hapticFeedback.performedTypes)
    }

    @Test
    fun swipe_belowTrigger_doesNotCallOnTriggerOrPlayHaptic() = runDesktopComposeUiTest {
        setSwipeableContent()

        onNodeWithTag(ROW_TAG).performTouchInput { swipeSideways(SHORT_SWIPE_PX) }
        waitForIdle()

        assertEquals(0, triggerCount)
        assertTrue(hapticFeedback.performedTypes.isEmpty())
    }

    @Test
    fun swipe_pastTriggerAndBackBeforeRelease_doesNotCallOnTrigger() = runDesktopComposeUiTest {
        setSwipeableContent()

        onNodeWithTag(ROW_TAG).performTouchInput {
            down(centerLeft + Offset(EDGE_INSET_PX, 0f))
            moveInSteps(LONG_SWIPE_PX)
            moveInSteps(-LONG_SWIPE_PX)
            up()
        }
        waitForIdle()

        assertEquals(0, triggerCount)
        assertEquals(1, hapticFeedback.performedTypes.size)
    }

    @Test
    fun swipe_whileHeld_movesRowUpToMaximumDistance() = runDesktopComposeUiTest {
        setSwipeableContent()

        onNodeWithTag(ROW_TAG).performTouchInput {
            down(centerLeft + Offset(EDGE_INSET_PX, 0f))
            moveInSteps(VERY_LONG_SWIPE_PX)
        }

        val rowLeft = onNodeWithTag(ROW_TAG).fetchSemanticsNode().boundsInRoot.left
        assertTrue(rowLeft > TRIGGER_DISTANCE_DP, "row moved only $rowLeft")
        assertTrue(rowLeft <= MAX_DISTANCE_DP + TOLERANCE_PX, "row moved past the maximum: $rowLeft")
    }

    @Test
    fun release_afterSwipe_returnsRowToItsPlace() = runDesktopComposeUiTest {
        setSwipeableContent()

        onNodeWithTag(ROW_TAG).performTouchInput { swipeSideways(LONG_SWIPE_PX) }
        mainClock.advanceTimeBy(SETTLE_TIME_MILLIS)

        assertEquals(0f, onNodeWithTag(ROW_TAG).fetchSemanticsNode().boundsInRoot.left, absoluteTolerance = TOLERANCE_PX)
    }

    @Test
    fun swipe_againstTheDirection_doesNotMoveRowOrCallOnTrigger() = runDesktopComposeUiTest {
        setSwipeableContent(direction = SwipeToReplyDirection.EndToStart)

        onNodeWithTag(ROW_TAG).performTouchInput { swipeSideways(LONG_SWIPE_PX) }
        waitForIdle()

        assertEquals(0, triggerCount)
    }

    @Test
    fun swipe_towardsStartWithEndToStartDirection_callsOnTrigger() = runDesktopComposeUiTest {
        setSwipeableContent(direction = SwipeToReplyDirection.EndToStart)

        onNodeWithTag(ROW_TAG).performTouchInput {
            down(centerRight - Offset(EDGE_INSET_PX, 0f))
            moveInSteps(-LONG_SWIPE_PX)
            up()
        }
        waitForIdle()

        assertEquals(1, triggerCount)
    }

    @Test
    fun swipe_whenDisabled_doesNotCallOnTrigger() = runDesktopComposeUiTest {
        setSwipeableContent(enabled = false)

        onNodeWithTag(ROW_TAG).performTouchInput { swipeSideways(LONG_SWIPE_PX) }
        waitForIdle()

        assertEquals(0, triggerCount)
    }

    @Test
    fun verticalSwipe_insideList_scrollsListWithoutTrigger() = runDesktopComposeUiTest {
        lateinit var listState: LazyListState
        setContentWithFakes {
            listState = rememberLazyListState()
            LazyColumn(state = listState, modifier = Modifier.size(LIST_SIZE_DP.dp).testTag(LIST_TAG)) {
                items(LIST_ITEM_COUNT) {
                    val swipeState = rememberSwipeToReplyState()
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(LIST_ITEM_HEIGHT_DP.dp)
                            .swipeToReply(state = swipeState, onTrigger = { triggerCount++ })
                    )
                }
            }
        }

        onNodeWithTag(LIST_TAG).performTouchInput { swipeUp() }
        waitForIdle()

        assertTrue(listState.firstVisibleItemIndex > 0, "list did not scroll")
        assertEquals(0, triggerCount)
    }

    @Test
    fun diagonalSwipe_insideList_scrollsListWithoutTrigger() = runDesktopComposeUiTest {
        lateinit var listState: LazyListState
        setContentWithFakes {
            listState = rememberLazyListState()
            LazyColumn(state = listState, modifier = Modifier.size(LIST_SIZE_DP.dp).testTag(LIST_TAG)) {
                items(LIST_ITEM_COUNT) {
                    val swipeState = rememberSwipeToReplyState()
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(LIST_ITEM_HEIGHT_DP.dp)
                            .swipeToReply(state = swipeState, onTrigger = { triggerCount++ })
                    )
                }
            }
        }

        onNodeWithTag(LIST_TAG).performTouchInput {
            down(center)
            repeat(MOVE_STEP_COUNT) { moveBy(Offset(DIAGONAL_STEP_X_PX, -DIAGONAL_STEP_Y_PX)) }
            up()
        }
        waitForIdle()

        assertTrue(listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0, "list did not scroll")
        assertEquals(0, triggerCount)
    }

    private fun ComposeUiTest.setSwipeableContent(
        direction: SwipeToReplyDirection = SwipeToReplyDirection.StartToEnd,
        enabled: Boolean = true
    ) {
        setContentWithFakes {
            val swipeState = rememberSwipeToReplyState()
            Box(
                modifier = Modifier
                    .size(width = ROW_WIDTH_DP.dp, height = ROW_HEIGHT_DP.dp)
                    .swipeToReply(
                        state = swipeState,
                        onTrigger = { triggerCount++ },
                        direction = direction,
                        enabled = enabled
                    )
                    .testTag(ROW_TAG)
            )
        }
    }

    private fun ComposeUiTest.setContentWithFakes(content: @Composable () -> Unit) {
        setContent {
            CompositionLocalProvider(
                LocalHapticFeedback provides hapticFeedback,
                LocalDensity provides Density(1f),
                LocalReduceMotion provides false
            ) {
                content()
            }
        }
    }

    private fun TouchInjectionScope.swipeSideways(distancePx: Float) {
        down(centerLeft + Offset(EDGE_INSET_PX, 0f))
        moveInSteps(distancePx)
        up()
    }

    private fun TouchInjectionScope.moveInSteps(distancePx: Float) {
        repeat(MOVE_STEP_COUNT) { moveBy(Offset(distancePx / MOVE_STEP_COUNT, 0f)) }
    }

    private companion object {
        const val ROW_TAG = "swipeRow"
        const val LIST_TAG = "list"
        const val ROW_WIDTH_DP = 300
        const val ROW_HEIGHT_DP = 60
        const val LIST_SIZE_DP = 300
        const val LIST_ITEM_HEIGHT_DP = 60
        const val LIST_ITEM_COUNT = 30
        const val TRIGGER_DISTANCE_DP = 64f
        const val MAX_DISTANCE_DP = 96f
        const val EDGE_INSET_PX = 20f
        const val SHORT_SWIPE_PX = 50f
        const val LONG_SWIPE_PX = 150f
        const val VERY_LONG_SWIPE_PX = 260f
        const val DIAGONAL_STEP_X_PX = 4f
        const val DIAGONAL_STEP_Y_PX = 20f
        const val MOVE_STEP_COUNT = 10
        const val TOLERANCE_PX = 0.5f
        const val SETTLE_TIME_MILLIS = 1_000L
    }
}
