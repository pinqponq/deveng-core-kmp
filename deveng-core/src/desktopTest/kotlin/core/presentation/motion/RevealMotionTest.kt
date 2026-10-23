package core.presentation.motion

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onRoot
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

    @Test
    fun countUpText_withJuiceState_popsOnEveryStep() = runDesktopComposeUiTest {
        lateinit var juiceState: JuiceState
        var peakScale = 1f
        setContent {
            juiceState = rememberJuiceState()
            CountUpText(value = COUNT_TARGET, stepDelayMillis = COUNT_STEP_DELAY_MILLIS, juiceState = juiceState)
        }
        mainClock.autoAdvance = false

        mainClock.advanceTimeBy(POP_PEAK_MILLIS)
        peakScale = juiceState.scale.value

        assertTrue(peakScale > 1.05f, "peak scale was $peakScale")
    }

    @Test
    fun countUpText_beforeStepDelay_doesNotAdvanceToNextStep() = runDesktopComposeUiTest {
        val stepValues = mutableListOf<Int>()
        setContent {
            CountUpText(
                value = COUNT_TARGET,
                stepDelayMillis = COUNT_LONG_STEP_DELAY_MILLIS,
                onStep = { stepValue -> stepValues += stepValue }
            )
        }
        mainClock.autoAdvance = false

        mainClock.advanceTimeBy(COUNT_LONG_STEP_DELAY_MILLIS / 2)

        assertEquals(listOf(1), stepValues)
    }

    @Test
    fun countUpText_withNewValueAfterReduceMotionToggle_showsNewValueAtOnce() = runDesktopComposeUiTest {
        var isReduceMotionEnabled by mutableStateOf(false)
        setContent {
            CompositionLocalProvider(LocalReduceMotion provides isReduceMotionEnabled) {
                CountUpText(value = COUNT_TARGET, stepDelayMillis = COUNT_STEP_DELAY_MILLIS)
            }
        }
        mainClock.advanceTimeBy(COUNT_SETTLE_MILLIS)

        isReduceMotionEnabled = true
        mainClock.advanceTimeByFrame()
        mainClock.advanceTimeByFrame()

        onNodeWithText(COUNT_TARGET.toString()).assertExists()
    }

    @Test
    fun juice_withPeakRotation_rotatesAtPeak() = runDesktopComposeUiTest {
        lateinit var juiceState: JuiceState
        setContent {
            juiceState = rememberJuiceState(peakRotationDegrees = 3f)
            Box(modifier = Modifier.size(BOX_SIZE_DP.dp).juice(juiceState))
        }
        mainClock.autoAdvance = false

        juiceState.pop()
        mainClock.advanceTimeBy(POP_PEAK_MILLIS)

        assertEquals(3f, juiceState.rotationDegrees.value, absoluteTolerance = 0.5f)
    }

    @Test
    fun juice_afterPop_rendersEnlargedBox() = runDesktopComposeUiTest {
        lateinit var juiceState: JuiceState
        setContent {
            juiceState = rememberJuiceState(peakScale = 2f)
            Box(modifier = Modifier.background(Color.White).size(ROOT_SIZE_DP.dp), contentAlignment = Alignment.Center) {
                Box(modifier = Modifier.size(BOX_SIZE_DP.dp).juice(juiceState).background(Color.Black))
            }
        }
        val restPixelCount = onRoot().captureToImage().countPixelsOtherThan(Color.White)
        mainClock.autoAdvance = false

        juiceState.pop()
        mainClock.advanceTimeBy(POP_PEAK_MILLIS)
        val peakPixelCount = onRoot().captureToImage().countPixelsOtherThan(Color.White)

        assertTrue(peakPixelCount > restPixelCount * 3, "rest $restPixelCount, peak $peakPixelCount")
    }

    @Test
    fun flipCard_midTurn_rendersNarrowerCardThanAtRest() = runDesktopComposeUiTest {
        var isFlipped by mutableStateOf(false)
        setContent {
            Box(modifier = Modifier.background(Color.White).size(ROOT_SIZE_DP.dp), contentAlignment = Alignment.Center) {
                FlipCard(
                    modifier = Modifier.size(BOX_SIZE_DP.dp),
                    isFlipped = isFlipped,
                    durationMillis = FLIP_DURATION_MILLIS,
                    front = { Box(modifier = Modifier.size(BOX_SIZE_DP.dp).background(Color.Black)) },
                    back = { Box(modifier = Modifier.size(BOX_SIZE_DP.dp).background(Color.Black)) }
                )
            }
        }
        val restPixelCount = onRoot().captureToImage().countPixelsOtherThan(Color.White)
        mainClock.autoAdvance = false

        isFlipped = true
        mainClock.advanceTimeBy(FLIP_QUARTER_MILLIS)
        val turningPixelCount = onRoot().captureToImage().countPixelsOtherThan(Color.White)

        assertTrue(turningPixelCount < restPixelCount * 0.8, "rest $restPixelCount, turning $turningPixelCount")
    }

    private fun ImageBitmap.countPixelsOtherThan(color: Color): Int {
        val pixels = IntArray(width * height)
        readPixels(pixels)
        return pixels.count { pixel -> Color(pixel) != color }
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
        const val COUNT_LONG_STEP_DELAY_MILLIS = 400L
        const val ROOT_SIZE_DP = 100
        const val FLIP_QUARTER_MILLIS = 100L
        const val BOX_SIZE_DP = 40
        const val POP_PEAK_MILLIS = 72L
        const val POP_SETTLE_TOTAL_MILLIS = 400L
        const val SCALE_TOLERANCE = 0.01f
    }
}
