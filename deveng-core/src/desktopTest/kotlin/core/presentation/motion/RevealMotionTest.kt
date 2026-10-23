package core.presentation.motion

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class RevealMotionTest {

    @Test
    fun flipCard_beforeFlip_showsOnlyBackFace() = runDesktopComposeUiTest {
        setContent { FlipCardWithFaces(isFlipped = false) }

        onNodeWithText(BACK_TEXT).assertIsDisplayed()
        onNodeWithText(FRONT_TEXT).assertDoesNotExist()
    }

    @Test
    fun flipCard_afterFlip_showsOnlyFrontFace() = runDesktopComposeUiTest {
        var isFlipped by mutableStateOf(false)
        setContent { FlipCardWithFaces(isFlipped = isFlipped) }

        isFlipped = true
        mainClock.advanceTimeBy(FLIP_SETTLE_MILLIS)

        onNodeWithText(FRONT_TEXT).assertIsDisplayed()
        onNodeWithText(BACK_TEXT).assertDoesNotExist()
    }

    @Test
    fun flipCard_halfwayThroughFlip_stillShowsBackFace() = runDesktopComposeUiTest {
        var isFlipped by mutableStateOf(false)
        setContent { FlipCardWithFaces(isFlipped = isFlipped) }
        mainClock.autoAdvance = false

        isFlipped = true
        mainClock.advanceTimeBy(FLIP_EARLY_MILLIS)

        onNodeWithText(BACK_TEXT).assertExists()
        onNodeWithText(FRONT_TEXT).assertDoesNotExist()
    }

    @Test
    fun flipCard_withDelay_waitsBeforeTurning() = runDesktopComposeUiTest {
        var isFlipped by mutableStateOf(false)
        setContent { FlipCardWithFaces(isFlipped = isFlipped, delayMillis = FLIP_DELAY_MILLIS) }
        mainClock.autoAdvance = false

        isFlipped = true
        mainClock.advanceTimeBy(FLIP_DELAY_MILLIS + FLIP_EARLY_MILLIS)

        onNodeWithText(BACK_TEXT).assertExists()
        mainClock.autoAdvance = true
        mainClock.advanceTimeBy(FLIP_SETTLE_MILLIS + FLIP_DELAY_MILLIS)
        onNodeWithText(FRONT_TEXT).assertExists()
    }

    @Test
    fun flipCard_withReduceMotion_swapsFacesAtOnce() = runDesktopComposeUiTest {
        var isFlipped by mutableStateOf(false)
        setContent {
            CompositionLocalProvider(LocalReduceMotion provides true) {
                FlipCardWithFaces(isFlipped = isFlipped)
            }
        }
        mainClock.autoAdvance = false

        isFlipped = true
        mainClock.advanceTimeByFrame()
        mainClock.advanceTimeByFrame()

        onNodeWithText(FRONT_TEXT).assertExists()
    }

    @Test
    fun countUpText_onAppear_countsUpToValueAndReportsEveryStep() = runDesktopComposeUiTest {
        val stepValues = mutableListOf<Int>()
        setContent {
            CountUpText(
                value = COUNT_TARGET,
                stepDelayMillis = COUNT_STEP_DELAY_MILLIS,
                onStep = { stepValue -> stepValues += stepValue }
            )
        }

        mainClock.advanceTimeBy(COUNT_SETTLE_MILLIS)

        onNodeWithText(COUNT_TARGET.toString()).assertExists()
        assertEquals(listOf(1, 2, 3), stepValues)
    }

    @Test
    fun countUpText_withFormatter_showsFormattedValue() = runDesktopComposeUiTest {
        setContent {
            CountUpText(
                value = COUNT_TARGET,
                stepDelayMillis = COUNT_STEP_DELAY_MILLIS,
                formatter = { shownValue -> "$shownValue km" }
            )
        }

        mainClock.advanceTimeBy(COUNT_SETTLE_MILLIS)

        onNodeWithText("$COUNT_TARGET km").assertExists()
    }

    @Test
    fun countUpText_withReduceMotion_showsValueWithoutSteps() = runDesktopComposeUiTest {
        val stepValues = mutableListOf<Int>()
        setContent {
            CompositionLocalProvider(LocalReduceMotion provides true) {
                CountUpText(value = COUNT_TARGET, onStep = { stepValue -> stepValues += stepValue })
            }
        }

        mainClock.advanceTimeByFrame()
        mainClock.advanceTimeByFrame()

        onNodeWithText(COUNT_TARGET.toString()).assertExists()
        assertTrue(stepValues.isEmpty())
    }

    @Test
    fun countUpText_whenValueChanges_countsFromShownValue() = runDesktopComposeUiTest {
        var value by mutableStateOf(COUNT_TARGET)
        val stepValues = mutableListOf<Int>()
        setContent {
            CountUpText(
                value = value,
                stepDelayMillis = COUNT_STEP_DELAY_MILLIS,
                onStep = { stepValue -> stepValues += stepValue }
            )
        }
        mainClock.advanceTimeBy(COUNT_SETTLE_MILLIS)
        stepValues.clear()

        value = COUNT_TARGET + 2
        mainClock.advanceTimeBy(COUNT_SETTLE_MILLIS)

        assertEquals(listOf(COUNT_TARGET + 1, COUNT_TARGET + 2), stepValues)
    }

    @Test
    fun juice_afterPop_growsThenReturnsToRestSize() = runDesktopComposeUiTest {
        lateinit var juiceState: JuiceState
        setContent {
            juiceState = rememberJuiceState()
            Box(modifier = Modifier.size(BOX_SIZE_DP.dp).juice(juiceState))
        }
        mainClock.autoAdvance = false

        juiceState.pop()
        mainClock.advanceTimeBy(POP_PEAK_MILLIS)
        val peakScale = juiceState.scale.value
        mainClock.autoAdvance = true
        mainClock.advanceTimeBy(POP_SETTLE_TOTAL_MILLIS)

        assertTrue(peakScale > 1.05f, "peak scale was $peakScale")
        assertEquals(1f, juiceState.scale.value, absoluteTolerance = SCALE_TOLERANCE)
    }

    @Test
    fun juice_withRotation_returnsRotationToZero() = runDesktopComposeUiTest {
        lateinit var juiceState: JuiceState
        setContent {
            juiceState = rememberJuiceState(peakRotationDegrees = 3f)
            Box(modifier = Modifier.size(BOX_SIZE_DP.dp).juice(juiceState))
        }

        juiceState.pop()
        mainClock.advanceTimeBy(POP_SETTLE_TOTAL_MILLIS)

        assertEquals(0f, juiceState.rotationDegrees.value, absoluteTolerance = SCALE_TOLERANCE)
    }

    @Test
    fun juice_withReduceMotion_keepsRestSize() = runDesktopComposeUiTest {
        lateinit var juiceState: JuiceState
        setContent {
            CompositionLocalProvider(LocalReduceMotion provides true) {
                juiceState = rememberJuiceState()
                Box(modifier = Modifier.size(BOX_SIZE_DP.dp).juice(juiceState))
            }
        }
        mainClock.autoAdvance = false

        juiceState.pop()
        mainClock.advanceTimeBy(POP_PEAK_MILLIS)

        assertEquals(1f, juiceState.scale.value)
    }

    @Test
    fun juice_popTwiceQuickly_restartsAndStillSettles() = runDesktopComposeUiTest {
        lateinit var juiceState: JuiceState
        setContent {
            juiceState = rememberJuiceState()
            Box(modifier = Modifier.size(BOX_SIZE_DP.dp).juice(juiceState))
        }
        mainClock.autoAdvance = false

        juiceState.pop()
        mainClock.advanceTimeBy(POP_PEAK_MILLIS / 2)
        juiceState.pop()
        mainClock.autoAdvance = true
        mainClock.advanceTimeBy(POP_SETTLE_TOTAL_MILLIS)

        assertEquals(1f, juiceState.scale.value, absoluteTolerance = SCALE_TOLERANCE)
    }

    @Composable
    private fun FlipCardWithFaces(isFlipped: Boolean, delayMillis: Int = 0) {
        FlipCard(
            front = { Text(FRONT_TEXT) },
            back = { Text(BACK_TEXT) },
            isFlipped = isFlipped,
            delayMillis = delayMillis,
            durationMillis = FLIP_DURATION_MILLIS
        )
    }

    private companion object {
        const val FRONT_TEXT = "front face"
        const val BACK_TEXT = "back face"
        const val FLIP_DURATION_MILLIS = 400
        const val FLIP_EARLY_MILLIS = 100L
        const val FLIP_SETTLE_MILLIS = 600L
        const val FLIP_DELAY_MILLIS = 300
        const val COUNT_TARGET = 3
        const val COUNT_STEP_DELAY_MILLIS = 50L
        const val COUNT_SETTLE_MILLIS = 500L
        const val BOX_SIZE_DP = 40
        const val POP_PEAK_MILLIS = 72L
        const val POP_SETTLE_TOTAL_MILLIS = 400L
        const val SCALE_TOLERANCE = 0.01f
    }
}
