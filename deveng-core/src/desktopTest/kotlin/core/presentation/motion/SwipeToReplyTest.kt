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
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalViewConfiguration
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
import kotlin.test.assertFailsWith
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
    fun swipe_reachingTouchSlopExactly_startsAndCountsTheNextMove() = runDesktopComposeUiTest {
        var touchSlop = 0f
        setContentWithFakes {
            touchSlop = LocalViewConfiguration.current.touchSlop
            val swipeState = rememberSwipeToReplyState()
            Box(
                modifier = Modifier
                    .size(width = ROW_WIDTH_DP.dp, height = ROW_HEIGHT_DP.dp)
                    .swipeToReply(state = swipeState, onTrigger = { triggerCount++ })
                    .testTag(ROW_TAG)
            )
        }

        onNodeWithTag(ROW_TAG).performTouchInput {
            down(centerLeft + Offset(EDGE_INSET_PX, 0f))
            moveBy(Offset(touchSlop, 0f))
            moveBy(Offset(TRIGGER_DISTANCE_DP, 0f))
            up()
        }
        waitForIdle()

        assertEquals(1, triggerCount)
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
    fun release_withReduceMotion_returnsRowAtOnce() = runDesktopComposeUiTest {
        setSwipeableContent(isReduceMotionEnabled = true)
        mainClock.autoAdvance = false

        onNodeWithTag(ROW_TAG).performTouchInput { swipeSideways(LONG_SWIPE_PX) }
        mainClock.advanceTimeBy(FRAME_MILLIS * 2)

        assertEquals(0f, onNodeWithTag(ROW_TAG).fetchSemanticsNode().boundsInRoot.left, absoluteTolerance = TOLERANCE_PX)
    }

    @Test
    fun release_withMotionAllowed_isStillMovingAfterOneFrame() = runDesktopComposeUiTest {
        setSwipeableContent()
        mainClock.autoAdvance = false

        onNodeWithTag(ROW_TAG).performTouchInput { swipeSideways(LONG_SWIPE_PX) }
        mainClock.advanceTimeBy(FRAME_MILLIS * 2)

        assertTrue(onNodeWithTag(ROW_TAG).fetchSemanticsNode().boundsInRoot.left > TOLERANCE_PX)
    }

    @Test
    fun cancelledDrag_pastTrigger_returnsRowWithoutTrigger() = runDesktopComposeUiTest {
        var isParentConsumingMoves by mutableStateOf(false)
        setContentWithFakes {
            val swipeState = rememberSwipeToReplyState()
            Box(
                modifier = Modifier.pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            if (isParentConsumingMoves) event.changes.forEach { it.consume() }
                        }
                    }
                }
            ) {
                Box(
                    modifier = Modifier
                        .size(width = ROW_WIDTH_DP.dp, height = ROW_HEIGHT_DP.dp)
                        .swipeToReply(state = swipeState, onTrigger = { triggerCount++ })
                        .testTag(ROW_TAG)
                )
            }
        }

        onNodeWithTag(ROW_TAG).performTouchInput {
            down(centerLeft + Offset(EDGE_INSET_PX, 0f))
            moveInSteps(LONG_SWIPE_PX)
        }
        isParentConsumingMoves = true
        onNodeWithTag(ROW_TAG).performTouchInput {
            moveBy(Offset(EDGE_INSET_PX, 0f))
            up()
        }
        mainClock.advanceTimeBy(SETTLE_TIME_MILLIS)

        assertEquals(0, triggerCount)
        assertEquals(0f, onNodeWithTag(ROW_TAG).fetchSemanticsNode().boundsInRoot.left, absoluteTolerance = TOLERANCE_PX)
    }

    @Test
    fun disablingDuringDrag_pastTrigger_returnsRowWithoutTrigger() = runDesktopComposeUiTest {
        var isSwipeEnabled by mutableStateOf(true)
        setContentWithFakes {
            val swipeState = rememberSwipeToReplyState()
            Box(
                modifier = Modifier
                    .size(width = ROW_WIDTH_DP.dp, height = ROW_HEIGHT_DP.dp)
                    .swipeToReply(state = swipeState, onTrigger = { triggerCount++ }, enabled = isSwipeEnabled)
                    .testTag(ROW_TAG)
            )
        }

        onNodeWithTag(ROW_TAG).performTouchInput {
            down(centerLeft + Offset(EDGE_INSET_PX, 0f))
            moveInSteps(LONG_SWIPE_PX)
        }
        isSwipeEnabled = false
        mainClock.advanceTimeBy(SETTLE_TIME_MILLIS)
        onNodeWithTag(ROW_TAG).performTouchInput { up() }
        mainClock.advanceTimeBy(SETTLE_TIME_MILLIS)

        assertEquals(0, triggerCount)
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
        val listState = setSwipeableListContent()

        onNodeWithTag(LIST_TAG).performTouchInput { swipeUp() }
        waitForIdle()

        assertTrue(listState.firstVisibleItemIndex > 0, "list did not scroll")
        assertEquals(0, triggerCount)
    }

    @Test
    fun diagonalSwipe_insideList_scrollsListWithoutTrigger() = runDesktopComposeUiTest {
        val listState = setSwipeableListContent()

        onNodeWithTag(LIST_TAG).performTouchInput {
            down(center)
            repeat(MOVE_STEP_COUNT) { moveBy(Offset(DIAGONAL_STEP_X_PX, -DIAGONAL_STEP_Y_PX)) }
            up()
        }
        waitForIdle()

        assertTrue(listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0, "list did not scroll")
        assertEquals(0, triggerCount)
    }

    @Test
    fun newSwipe_whileRowIsReturning_keepsRowUnderTheFinger() = runDesktopComposeUiTest {
        setSwipeableContent()
        mainClock.autoAdvance = false
        onNodeWithTag(ROW_TAG).performTouchInput { swipeSideways(LONG_SWIPE_PX) }
        mainClock.advanceTimeBy(FRAME_MILLIS)

        onNodeWithTag(ROW_TAG).performTouchInput {
            down(centerLeft + Offset(EDGE_INSET_PX, 0f))
            moveInSteps(LONG_SWIPE_PX)
        }
        mainClock.advanceTimeBy(SETTLE_TIME_MILLIS)

        val rowLeft = onNodeWithTag(ROW_TAG).fetchSemanticsNode().boundsInRoot.left
        assertTrue(rowLeft >= TRIGGER_DISTANCE_DP, "row returned while held: $rowLeft")
    }

    @Test
    fun sidewaysSwipeWithVerticalDrift_insideList_keepsListStillAndCallsOnTrigger() = runDesktopComposeUiTest {
        val listState = setSwipeableListContent()

        onNodeWithTag(LIST_TAG).performTouchInput {
            down(centerLeft + Offset(EDGE_INSET_PX, 0f))
            repeat(MOVE_STEP_COUNT) { moveBy(Offset(DRIFT_STEP_X_PX, -DRIFT_STEP_Y_PX)) }
            up()
        }
        waitForIdle()

        assertEquals(0, listState.firstVisibleItemIndex)
        assertEquals(0, listState.firstVisibleItemScrollOffset)
        assertEquals(1, triggerCount)
    }

    @Test
    fun rememberSwipeToReplyState_withMaximumNotLargerThanTrigger_throws() {
        assertFailsWith<IllegalArgumentException> {
            runDesktopComposeUiTest {
                setContent { rememberSwipeToReplyState(triggerDistance = TRIGGER_DISTANCE_DP.dp, maxDistance = TRIGGER_DISTANCE_DP.dp) }
            }
        }
    }

    @Test
    fun rememberSwipeToReplyState_acrossRecomposition_keepsStateUntilASettingChanges() = runDesktopComposeUiTest {
        var recompositionCount by mutableStateOf(0)
        var settings by mutableStateOf(SwipeToReplySettings())
        val rememberedStates = mutableListOf<SwipeToReplyState>()
        setContent {
            CompositionLocalProvider(LocalDensity provides Density(settings.density)) {
                if (recompositionCount >= 0) {
                    val state = rememberSwipeToReplyState(
                        triggerDistance = settings.triggerDistanceDp.dp,
                        maxDistance = settings.maxDistanceDp.dp,
                        resistanceExponent = settings.resistanceExponent,
                        returnMillis = settings.returnMillis
                    )
                    SideEffect { rememberedStates += state }
                }
            }
        }

        recompositionCount++
        waitForIdle()
        assertEquals(1, rememberedStates.distinct().size)

        val changedSettings = listOf(
            settings.copy(density = 2f),
            settings.copy(density = 2f, triggerDistanceDp = 50f),
            settings.copy(density = 2f, triggerDistanceDp = 50f, maxDistanceDp = 80f),
            settings.copy(density = 2f, triggerDistanceDp = 50f, maxDistanceDp = 80f, resistanceExponent = 2f),
            settings.copy(density = 2f, triggerDistanceDp = 50f, maxDistanceDp = 80f, resistanceExponent = 2f, returnMillis = 300)
        )
        changedSettings.forEachIndexed { index, changed ->
            settings = changed
            waitForIdle()
            assertEquals(index + 2, rememberedStates.distinct().size, "no new state after setting change $index")
        }
    }

    private fun ComposeUiTest.setSwipeableContent(
        direction: SwipeToReplyDirection = SwipeToReplyDirection.StartToEnd,
        enabled: Boolean = true,
        isReduceMotionEnabled: Boolean = false
    ) {
        setContentWithFakes(isReduceMotionEnabled = isReduceMotionEnabled) {
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

    private fun ComposeUiTest.setSwipeableListContent(): LazyListState {
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
        return listState
    }

    private fun ComposeUiTest.setContentWithFakes(
        isReduceMotionEnabled: Boolean = false,
        content: @Composable () -> Unit
    ) {
        setContent {
            CompositionLocalProvider(
                LocalHapticFeedback provides hapticFeedback,
                LocalDensity provides Density(1f),
                LocalReduceMotion provides isReduceMotionEnabled
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

    private data class SwipeToReplySettings(
        val density: Float = 1f,
        val triggerDistanceDp: Float = MotionDefaults.SWIPE_TO_REPLY_TRIGGER_DISTANCE_DP.toFloat(),
        val maxDistanceDp: Float = MotionDefaults.SWIPE_TO_REPLY_MAX_DISTANCE_DP.toFloat(),
        val resistanceExponent: Float = MotionDefaults.SWIPE_TO_REPLY_RESISTANCE_EXPONENT,
        val returnMillis: Int = MotionDefaults.SWIPE_TO_REPLY_RETURN_MILLIS
    )

    private companion object {
        const val ROW_TAG = "swipeRow"
        const val LIST_TAG = "list"
        const val ROW_WIDTH_DP = 300
        const val ROW_HEIGHT_DP = 60
        const val LIST_SIZE_DP = 300
        const val LIST_ITEM_HEIGHT_DP = 60
        const val LIST_ITEM_COUNT = 30
        const val TRIGGER_DISTANCE_DP = MotionDefaults.SWIPE_TO_REPLY_TRIGGER_DISTANCE_DP.toFloat()
        const val MAX_DISTANCE_DP = MotionDefaults.SWIPE_TO_REPLY_MAX_DISTANCE_DP.toFloat()
        const val EDGE_INSET_PX = 20f
        const val SHORT_SWIPE_PX = 50f
        const val LONG_SWIPE_PX = 150f
        const val VERY_LONG_SWIPE_PX = 260f
        const val DIAGONAL_STEP_X_PX = 4f
        const val DIAGONAL_STEP_Y_PX = 20f
        const val MOVE_STEP_COUNT = 10
        const val DRIFT_STEP_X_PX = 20f
        const val DRIFT_STEP_Y_PX = 5f
        const val FRAME_MILLIS = 16L
        const val TOLERANCE_PX = 0.5f
        const val SETTLE_TIME_MILLIS = 1_000L
    }
}
