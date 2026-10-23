package core.presentation.motion

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class RevealMotionParameterTest {

    @Test
    fun countUpText_withModifier_appliesIt() = runDesktopComposeUiTest {
        setContent { CountUpText(value = 0, modifier = Modifier.testTag(TAG_A)) }

        onNodeWithTag(TAG_A).assertExists()
    }

    @Test
    fun countUpText_whenModifierChanges_appliesNewModifier() = runDesktopComposeUiTest {
        var tag by mutableStateOf(TAG_A)
        setContent { CountUpAll(value = 0, modifier = Modifier.testTag(tag)) }

        tag = TAG_B
        waitForIdle()

        onNodeWithTag(TAG_B).assertExists()
        onNodeWithTag(TAG_A).assertDoesNotExist()
    }

    @Test
    fun countUpText_withStartValue_countsFromIt() = runDesktopComposeUiTest {
        val stepValues = mutableListOf<Int>()
        setContent {
            CountUpText(
                value = 3,
                startValue = 1,
                stepDelayMillis = STEP_DELAY_MILLIS,
                onStep = { stepValues += it }
            )
        }

        mainClock.advanceTimeBy(SETTLE_MILLIS)

        assertEquals(listOf(2, 3), stepValues)
    }

    @Test
    fun countUpText_withoutStartValue_countsFromZero() = runDesktopComposeUiTest {
        val stepValues = mutableListOf<Int>()
        setContent { CountUpText(value = 2, stepDelayMillis = STEP_DELAY_MILLIS, onStep = { stepValues += it }) }

        mainClock.advanceTimeBy(SETTLE_MILLIS)

        assertEquals(listOf(1, 2), stepValues)
    }

    @Test
    fun countUpText_withStyle_usesIt() = runDesktopComposeUiTest {
        setContent {
            CompositionLocalProvider(LocalTextStyle provides TextStyle(fontSize = 30.sp)) {
                CountUpText(value = 0, style = TextStyle(fontSize = 20.sp))
            }
        }

        assertEquals(20.sp, onNodeWithText("0").textStyle().fontSize)
    }

    @Test
    fun countUpText_withoutStyle_usesLocalTextStyle() = runDesktopComposeUiTest {
        setContent {
            CompositionLocalProvider(LocalTextStyle provides TextStyle(fontSize = 30.sp)) {
                CountUpText(value = 0)
            }
        }

        assertEquals(30.sp, onNodeWithText("0").textStyle().fontSize)
    }

    @Test
    fun countUpText_withoutStyle_whenLocalTextStyleChanges_usesNewLocalStyle() = runDesktopComposeUiTest {
        var fontSize by mutableStateOf(30.sp)
        setContent {
            CompositionLocalProvider(LocalTextStyle provides TextStyle(fontSize = fontSize)) {
                CountUpText(
                    value = 0,
                    modifier = Modifier,
                    startValue = 0,
                    color = Color.Unspecified,
                    stepDelayMillis = STEP_DELAY_MILLIS,
                    maxStepCount = 12,
                    maxTotalMillis = SETTLE_MILLIS,
                    formatter = Int::toString,
                    juiceState = null,
                    onStep = {}
                )
            }
        }

        fontSize = 34.sp
        waitForIdle()

        assertEquals(34.sp, onNodeWithText("0").textStyle().fontSize)
    }

    @Test
    fun countUpText_whenStyleChanges_usesNewStyle() = runDesktopComposeUiTest {
        var style by mutableStateOf(TextStyle(fontSize = 20.sp))
        setContent { CountUpAll(value = 0, style = style) }

        style = TextStyle(fontSize = 24.sp)
        waitForIdle()

        assertEquals(24.sp, onNodeWithText("0").textStyle().fontSize)
    }

    @Test
    fun countUpText_withColor_usesIt() = runDesktopComposeUiTest {
        setContent { CountUpText(value = 0, color = Color.Red) }

        assertEquals(Color.Red, onNodeWithText("0").textStyle().color)
    }

    @Test
    fun countUpText_withoutColor_usesContentColor() = runDesktopComposeUiTest {
        setContent {
            CompositionLocalProvider(LocalContentColor provides Color.Blue) {
                CountUpText(value = 0)
            }
        }

        assertEquals(Color.Blue, onNodeWithText("0").textStyle().color)
    }

    @Test
    fun countUpText_whenColorChanges_usesNewColor() = runDesktopComposeUiTest {
        var color by mutableStateOf(Color.Red)
        setContent { CountUpAll(value = 0, color = color) }

        color = Color.Green
        waitForIdle()

        assertEquals(Color.Green, onNodeWithText("0").textStyle().color)
    }

    @Test
    fun countUpText_withMaxStepCount_limitsSteps() = runDesktopComposeUiTest {
        val stepValues = mutableListOf<Int>()
        setContent {
            CountUpText(
                value = 10,
                maxStepCount = 2,
                stepDelayMillis = STEP_DELAY_MILLIS,
                onStep = { stepValues += it }
            )
        }

        mainClock.advanceTimeBy(SETTLE_MILLIS)

        assertEquals(listOf(5, 10), stepValues)
    }

    @Test
    fun countUpText_withoutMaxStepCount_limitsStepsToDefault() = runDesktopComposeUiTest {
        val stepValues = mutableListOf<Int>()
        setContent {
            CountUpText(
                value = 240,
                stepDelayMillis = 1L,
                onStep = { stepValues += it }
            )
        }

        mainClock.advanceTimeBy(SETTLE_MILLIS)

        assertEquals(MotionDefaults.COUNT_UP_MAX_STEP_COUNT, stepValues.size)
        assertEquals(240, stepValues.last())
    }

    @Test
    fun countUpText_withoutStepDelay_waitsDefaultDelayBetweenSteps() = runDesktopComposeUiTest {
        val stepValues = mutableListOf<Int>()
        setContent { CountUpText(value = 3, onStep = { stepValues += it }) }
        mainClock.autoAdvance = false

        mainClock.advanceTimeBy(MotionDefaults.COUNT_UP_STEP_DELAY_MILLIS / 2)

        assertEquals(listOf(1), stepValues)
    }

    @Test
    fun countUpText_withStepDelay_waitsThatDelayBetweenSteps() = runDesktopComposeUiTest {
        val stepValues = mutableListOf<Int>()
        setContent { CountUpText(value = 3, stepDelayMillis = STEP_DELAY_MILLIS, onStep = { stepValues += it }) }
        mainClock.autoAdvance = false

        mainClock.advanceTimeBy(STEP_DELAY_MILLIS * 2 + FRAME_MILLIS / 2)

        assertEquals(listOf(1, 2, 3), stepValues)
    }

    @Test
    fun countUpText_withoutMaxTotalMillis_capsTotalTimeAtDefault() = runDesktopComposeUiTest {
        val stepValues = mutableListOf<Int>()
        setContent { CountUpText(value = 3, stepDelayMillis = LONG_DELAY_MILLIS, onStep = { stepValues += it }) }
        mainClock.autoAdvance = false

        mainClock.advanceTimeBy(MotionDefaults.COUNT_UP_MAX_TOTAL_MILLIS / 3 + FRAME_MILLIS)

        assertEquals(listOf(1, 2), stepValues)
    }

    @Test
    fun countUpText_withMaxTotalMillis_capsTotalTime() = runDesktopComposeUiTest {
        val stepValues = mutableListOf<Int>()
        setContent {
            CountUpText(
                value = 3,
                stepDelayMillis = LONG_DELAY_MILLIS,
                maxTotalMillis = 60L,
                onStep = { stepValues += it }
            )
        }
        mainClock.autoAdvance = false

        mainClock.advanceTimeBy(SETTLE_MILLIS / 2)

        assertEquals(listOf(1, 2, 3), stepValues)
    }

    @Test
    fun countUpText_whenFormatterChanges_showsNewFormat() = runDesktopComposeUiTest {
        var suffix by mutableStateOf("a")
        setContent { CountUpAll(value = 0, formatter = { "$it$suffix" }) }

        suffix = "b"
        waitForIdle()

        onNodeWithText("0b").assertExists()
    }

    @Test
    fun countUpText_whenOnStepChanges_callsNewCallback() = runDesktopComposeUiTest {
        var firstCalls = 0
        var secondCalls = 0
        var onStep by mutableStateOf<(Int) -> Unit>({ firstCalls++ })
        var value by mutableStateOf(0)
        setContent { CountUpAll(value = value, stepDelayMillis = STEP_DELAY_MILLIS, onStep = onStep) }

        onStep = { secondCalls++ }
        waitForIdle()
        value = 2
        mainClock.advanceTimeBy(SETTLE_MILLIS)

        assertEquals(0, firstCalls)
        assertEquals(2, secondCalls)
    }

    @Test
    fun countUpText_whenJuiceStateChanges_popsNewState() = runDesktopComposeUiTest {
        lateinit var first: JuiceState
        lateinit var second: JuiceState
        var useSecond by mutableStateOf(false)
        var value by mutableStateOf(0)
        setContent {
            first = rememberJuiceState()
            second = rememberJuiceState()
            CountUpAll(
                value = value,
                stepDelayMillis = STEP_DELAY_MILLIS,
                juiceState = if (useSecond) second else first
            )
        }
        mainClock.autoAdvance = false

        useSecond = true
        mainClock.advanceTimeByFrame()
        value = 1
        var secondPeak = 1f
        var firstPeak = 1f
        repeat(PEAK_FRAME_COUNT) {
            mainClock.advanceTimeByFrame()
            secondPeak = maxOf(secondPeak, second.scale.value)
            firstPeak = maxOf(firstPeak, first.scale.value)
        }

        assertTrue(secondPeak > 1.05f, "second peak $secondPeak")
        assertEquals(1f, firstPeak)
    }

    @Test
    fun countUpText_withoutJuiceState_stillCountsUp() = runDesktopComposeUiTest {
        setContent { CountUpText(value = 2, stepDelayMillis = STEP_DELAY_MILLIS) }

        mainClock.advanceTimeBy(SETTLE_MILLIS)

        onNodeWithText("2").assertExists()
    }

    @Test
    fun flipCard_withModifier_appliesIt() = runDesktopComposeUiTest {
        setContent {
            FlipCard(
                front = { Text(FRONT_TEXT) },
                back = { Text(BACK_TEXT) },
                isFlipped = false,
                modifier = Modifier.testTag(TAG_A)
            )
        }

        onNodeWithTag(TAG_A).assertExists()
    }

    @Test
    fun flipCard_whenModifierChanges_appliesNewModifier() = runDesktopComposeUiTest {
        var tag by mutableStateOf(TAG_A)
        setContent { FlipCardLabeled(isFlipped = false, modifier = Modifier.testTag(tag)) }

        tag = TAG_B
        waitForIdle()

        onNodeWithTag(TAG_B).assertExists()
        onNodeWithTag(TAG_A).assertDoesNotExist()
    }

    @Test
    fun flipCard_whenFacesChange_showsNewFaces() = runDesktopComposeUiTest {
        var frontLabel by mutableStateOf("one")
        var backLabel by mutableStateOf("one")
        var isFlipped by mutableStateOf(false)
        setContent { FlipCardLabeled(isFlipped = isFlipped, frontLabel = frontLabel, backLabel = backLabel) }
        backLabel = "two"
        waitForIdle()
        onNodeWithText("back two").assertExists()

        isFlipped = true
        mainClock.advanceTimeBy(FLIP_SETTLE_MILLIS)
        frontLabel = "three"
        waitForIdle()

        onNodeWithText("front three").assertExists()
    }

    @Test
    fun flipCard_withDelay_keepsRestUntilDelayPassed() = runDesktopComposeUiTest {
        var delayMillis by mutableStateOf(0)
        var isFlipped by mutableStateOf(false)
        setContent { FlipCardSquare(isFlipped = isFlipped, delayMillis = delayMillis) }
        val restPixelCount = onRoot().captureToImage().countNonWhite()
        mainClock.autoAdvance = false

        delayMillis = FLIP_DELAY_MILLIS
        mainClock.advanceTimeByFrame()
        isFlipped = true
        mainClock.advanceTimeBy(FLIP_DELAY_MILLIS / 2L)

        assertEquals(restPixelCount, onRoot().captureToImage().countNonWhite())
    }

    @Test
    fun flipCard_whenDurationChanges_turnsWithNewDuration() = runDesktopComposeUiTest {
        var durationMillis by mutableStateOf(LONG_FLIP_DURATION_MILLIS)
        var isFlipped by mutableStateOf(false)
        setContent { FlipCardSquare(isFlipped = isFlipped, durationMillis = durationMillis) }
        mainClock.autoAdvance = false

        durationMillis = FLIP_DURATION_MILLIS
        mainClock.advanceTimeByFrame()
        isFlipped = true
        mainClock.advanceTimeBy(FLIP_SETTLE_MILLIS)

        onRoot().captureToImage().let { image ->
            assertEquals(SQUARE_PX, image.countNonWhite())
        }
    }

    @Test
    fun flipCard_atMidTurn_viewsCardWithPerspective() = runDesktopComposeUiTest {
        val nearCount = midTurnPixelCount(density = 1f, cameraDistance = 3f)
        val farCount = midTurnPixelCount(density = 1f, cameraDistance = 300f)

        assertTrue(nearCount != farCount, "near $nearCount, far $farCount")
    }

    @Test
    fun flipCard_withoutCameraDistance_usesDefaultDistance() = runDesktopComposeUiTest {
        val defaultCount = midTurnPixelCount(density = 1f, cameraDistance = null)
        val explicitCount = midTurnPixelCount(density = 1f, cameraDistance = MotionDefaults.FLIP_CAMERA_DISTANCE)

        assertEquals(explicitCount, defaultCount)
    }

    @Test
    fun flipCard_cameraDistance_scalesWithDensity() = runDesktopComposeUiTest {
        val denseCount = midTurnPixelCount(density = 2f, cameraDistance = 6f)
        val equivalentCount = midTurnPixelCount(density = 1f, cameraDistance = 12f)
        val unscaledCount = midTurnPixelCount(density = 1f, cameraDistance = 6f)

        assertEquals(equivalentCount, denseCount)
        assertNotEquals(unscaledCount, denseCount)
    }

    @Test
    fun flipCard_whenCameraDistanceChanges_usesNewDistance() = runDesktopComposeUiTest {
        var cameraDistance by mutableStateOf(300f)
        var isFlipped by mutableStateOf(false)
        setContent { FlipCardSquare(isFlipped = isFlipped, durationMillis = LONG_FLIP_DURATION_MILLIS, cameraDistance = cameraDistance) }
        mainClock.autoAdvance = false
        isFlipped = true
        mainClock.advanceTimeBy(LONG_FLIP_MID_MILLIS)
        val farCount = onRoot().captureToImage().countNonWhite()

        cameraDistance = 3f
        mainClock.advanceTimeByFrame()
        val nearCount = onRoot().captureToImage().countNonWhite()

        assertTrue(nearCount != farCount, "near $nearCount, far $farCount")
    }

    @Test
    fun flipCard_afterFlip_showsFrontFaceUnmirrored() = runDesktopComposeUiTest {
        var isFlipped by mutableStateOf(false)
        setContent {
            Box(modifier = Modifier.background(Color.White).size(ROOT_DP.dp), contentAlignment = Alignment.Center) {
                FlipCard(
                    isFlipped = isFlipped,
                    durationMillis = FLIP_DURATION_MILLIS,
                    front = {
                        Row {
                            Box(modifier = Modifier.size(HALF_DP.dp, FULL_DP.dp).background(Color.Black))
                            Box(modifier = Modifier.size(HALF_DP.dp, FULL_DP.dp).background(Color.Red))
                        }
                    },
                    back = { Box(modifier = Modifier.size(FULL_DP.dp)) }
                )
            }
        }

        isFlipped = true
        mainClock.advanceTimeBy(FLIP_SETTLE_MILLIS)
        val pixels = onRoot().captureToImage().toPixelMap()
        val centerY = ROOT_DP / 2

        assertEquals(Color.Black, pixels[ROOT_DP / 2 - HALF_DP / 2, centerY])
        assertEquals(Color.Red, pixels[ROOT_DP / 2 + HALF_DP / 2, centerY])
    }

    @Test
    fun juice_withPeakScale_growsToIt() = runDesktopComposeUiTest {
        lateinit var juiceState: JuiceState
        setContent { juiceState = rememberJuiceState(peakScale = 1.5f) }
        mainClock.autoAdvance = false

        juiceState.pop()
        mainClock.advanceTimeBy(MotionDefaults.POP_RISE_MILLIS.toLong())

        assertEquals(1.5f, juiceState.scale.value, absoluteTolerance = TOLERANCE)
    }

    @Test
    fun juice_withoutPeakScale_growsToDefault() = runDesktopComposeUiTest {
        lateinit var juiceState: JuiceState
        setContent { juiceState = rememberJuiceState() }
        mainClock.autoAdvance = false

        juiceState.pop()
        mainClock.advanceTimeBy(MotionDefaults.POP_RISE_MILLIS.toLong())

        assertEquals(MotionDefaults.POP_PEAK_SCALE, juiceState.scale.value, absoluteTolerance = TOLERANCE)
    }

    @Test
    fun juice_withoutPeakRotation_doesNotRotate() = runDesktopComposeUiTest {
        lateinit var juiceState: JuiceState
        setContent { juiceState = rememberJuiceState() }
        mainClock.autoAdvance = false

        juiceState.pop()
        mainClock.advanceTimeBy(MotionDefaults.POP_RISE_MILLIS.toLong())

        assertEquals(0f, juiceState.rotationDegrees.value)
    }

    @Test
    fun juice_withRiseMillis_risesOverThatTime() = runDesktopComposeUiTest {
        lateinit var juiceState: JuiceState
        setContent { juiceState = rememberJuiceState(riseMillis = LONG_MILLIS) }
        mainClock.autoAdvance = false

        juiceState.pop()
        mainClock.advanceTimeBy(MotionDefaults.POP_RISE_MILLIS.toLong() * 2)

        assertTrue(juiceState.scale.value < MotionDefaults.POP_PEAK_SCALE - 0.05f, "scale ${juiceState.scale.value}")
    }

    @Test
    fun juice_withoutRiseMillis_risesInDefaultTime() = runDesktopComposeUiTest {
        lateinit var juiceState: JuiceState
        setContent { juiceState = rememberJuiceState() }
        mainClock.autoAdvance = false

        juiceState.pop()
        mainClock.advanceTimeBy(MotionDefaults.POP_RISE_MILLIS.toLong() * 2)

        assertTrue(juiceState.scale.value < MotionDefaults.POP_PEAK_SCALE - 0.02f, "scale ${juiceState.scale.value}")
        assertTrue(juiceState.scale.value > 1f, "scale ${juiceState.scale.value}")
    }

    @Test
    fun juice_withSettleMillis_settlesOverThatTime() = runDesktopComposeUiTest {
        lateinit var juiceState: JuiceState
        setContent { juiceState = rememberJuiceState(settleMillis = LONG_MILLIS) }
        mainClock.autoAdvance = false

        juiceState.pop()
        mainClock.advanceTimeBy(MotionDefaults.POP_RISE_MILLIS + MotionDefaults.POP_SETTLE_MILLIS.toLong() + FRAME_MILLIS)

        assertTrue(juiceState.scale.value > 1.05f, "scale ${juiceState.scale.value}")
    }

    @Test
    fun juice_withoutSettleMillis_settlesInDefaultTime() = runDesktopComposeUiTest {
        lateinit var juiceState: JuiceState
        setContent { juiceState = rememberJuiceState() }
        mainClock.autoAdvance = false

        juiceState.pop()
        mainClock.advanceTimeBy(MotionDefaults.POP_RISE_MILLIS + MotionDefaults.POP_SETTLE_MILLIS.toLong() / 2)
        assertTrue(juiceState.scale.value > 1.01f, "scale ${juiceState.scale.value}")
        mainClock.advanceTimeBy(MotionDefaults.POP_SETTLE_MILLIS.toLong())

        assertEquals(1f, juiceState.scale.value, absoluteTolerance = TOLERANCE)
    }

    @Test
    fun juice_whenPeakScaleChanges_popsToNewPeak() = runDesktopComposeUiTest {
        var peakScale by mutableStateOf(1.5f)
        lateinit var juiceState: JuiceState
        setContent { juiceState = rememberJuiceState(peakScale = peakScale) }
        val firstState = juiceState

        peakScale = 2f
        waitForIdle()
        mainClock.autoAdvance = false
        juiceState.pop()
        mainClock.advanceTimeBy(MotionDefaults.POP_RISE_MILLIS.toLong())

        assertNotEquals(firstState, juiceState)
        assertEquals(2f, juiceState.scale.value, absoluteTolerance = TOLERANCE)
    }

    @Test
    fun juice_whenPeakRotationChanges_popsToNewRotation() = runDesktopComposeUiTest {
        var peakRotation by mutableStateOf(10f)
        lateinit var juiceState: JuiceState
        setContent { juiceState = rememberJuiceState(peakRotationDegrees = peakRotation) }
        val firstState = juiceState

        peakRotation = 20f
        waitForIdle()
        mainClock.autoAdvance = false
        juiceState.pop()
        mainClock.advanceTimeBy(MotionDefaults.POP_RISE_MILLIS.toLong())

        assertNotEquals(firstState, juiceState)
        assertEquals(20f, juiceState.rotationDegrees.value, absoluteTolerance = 0.5f)
    }

    @Test
    fun juice_whenRiseMillisChanges_usesNewRise() = runDesktopComposeUiTest {
        var riseMillis by mutableStateOf(MotionDefaults.POP_RISE_MILLIS)
        lateinit var juiceState: JuiceState
        setContent { juiceState = rememberJuiceState(riseMillis = riseMillis) }
        val firstState = juiceState

        riseMillis = LONG_MILLIS
        waitForIdle()
        mainClock.autoAdvance = false
        juiceState.pop()
        mainClock.advanceTimeBy(MotionDefaults.POP_RISE_MILLIS.toLong() * 2)

        assertNotEquals(firstState, juiceState)
        assertTrue(juiceState.scale.value < MotionDefaults.POP_PEAK_SCALE - 0.05f, "scale ${juiceState.scale.value}")
    }

    @Test
    fun juice_whenSettleMillisChanges_usesNewSettle() = runDesktopComposeUiTest {
        var settleMillis by mutableStateOf(MotionDefaults.POP_SETTLE_MILLIS)
        lateinit var juiceState: JuiceState
        setContent { juiceState = rememberJuiceState(settleMillis = settleMillis) }
        val firstState = juiceState

        settleMillis = LONG_MILLIS
        waitForIdle()
        mainClock.autoAdvance = false
        juiceState.pop()
        mainClock.advanceTimeBy(MotionDefaults.POP_RISE_MILLIS + MotionDefaults.POP_SETTLE_MILLIS.toLong() + FRAME_MILLIS)

        assertNotEquals(firstState, juiceState)
        assertTrue(juiceState.scale.value > 1.05f, "scale ${juiceState.scale.value}")
    }

    @Test
    fun juice_whenRecomposedWithSameParameters_keepsState() = runDesktopComposeUiTest {
        var tick by mutableStateOf(0)
        lateinit var juiceState: JuiceState
        setContent {
            tick
            juiceState = rememberJuiceState()
        }
        val firstState = juiceState

        tick = 1
        waitForIdle()

        assertSame(firstState, juiceState)
    }

    @Test
    fun juice_popAgainInSettle_restartsFromCurrentSize() = runDesktopComposeUiTest {
        lateinit var juiceState: JuiceState
        setContent { juiceState = rememberJuiceState(peakRotationDegrees = 10f) }
        mainClock.autoAdvance = false

        juiceState.pop()
        mainClock.advanceTimeBy(MotionDefaults.POP_RISE_MILLIS + MotionDefaults.POP_SETTLE_MILLIS / 2L)
        juiceState.pop()
        mainClock.advanceTimeBy(MotionDefaults.POP_RISE_MILLIS.toLong())

        assertEquals(MotionDefaults.POP_PEAK_SCALE, juiceState.scale.value, absoluteTolerance = TOLERANCE)
        assertEquals(10f, juiceState.rotationDegrees.value, absoluteTolerance = 0.5f)
        mainClock.autoAdvance = true
        mainClock.advanceTimeBy(MotionDefaults.POP_SETTLE_MILLIS.toLong() * 2)
        assertEquals(1f, juiceState.scale.value, absoluteTolerance = TOLERANCE)
        assertEquals(0f, juiceState.rotationDegrees.value, absoluteTolerance = TOLERANCE)
    }

    @Test
    fun juice_atRotationPeak_rotatesRenderedBox() = runDesktopComposeUiTest {
        lateinit var juiceState: JuiceState
        setContent {
            juiceState = rememberJuiceState(peakRotationDegrees = 45f)
            Box(modifier = Modifier.background(Color.White).size(ROOT_DP.dp), contentAlignment = Alignment.Center) {
                Box(modifier = Modifier.size(FULL_DP.dp).juice(juiceState).background(Color.Black))
            }
        }
        val cornerX = (ROOT_DP - FULL_DP) / 2 + 1
        assertEquals(Color.Black, onRoot().captureToImage().toPixelMap()[cornerX, cornerX])
        mainClock.autoAdvance = false

        juiceState.pop()
        mainClock.advanceTimeBy(MotionDefaults.POP_RISE_MILLIS.toLong())

        assertEquals(Color.White, onRoot().captureToImage().toPixelMap()[cornerX, cornerX])
    }

    private fun androidx.compose.ui.test.ComposeUiTest.midTurnPixelCount(density: Float, cameraDistance: Float?): Int {
        var isFlipped by mutableStateOf(false)
        setContent {
            CompositionLocalProvider(LocalDensity provides Density(density)) {
                Box(modifier = Modifier.background(Color.White).size((ROOT_PX / density).dp), contentAlignment = Alignment.Center) {
                    if (cameraDistance == null) {
                        FlipCardBox(isFlipped, density)
                    } else {
                        FlipCardBox(isFlipped, density, cameraDistance)
                    }
                }
            }
        }
        mainClock.autoAdvance = false
        isFlipped = true
        mainClock.advanceTimeBy(LONG_FLIP_MID_MILLIS)
        return onRoot().captureToImage().countNonWhite()
    }

    @androidx.compose.runtime.Composable
    private fun FlipCardBox(isFlipped: Boolean, density: Float) {
        FlipCard(
            isFlipped = isFlipped,
            durationMillis = LONG_FLIP_DURATION_MILLIS,
            front = { Box(modifier = Modifier.size((SQUARE_SIDE_PX / density).dp).background(Color.Black)) },
            back = { Box(modifier = Modifier.size((SQUARE_SIDE_PX / density).dp).background(Color.Black)) }
        )
    }

    @androidx.compose.runtime.Composable
    private fun FlipCardBox(isFlipped: Boolean, density: Float, cameraDistance: Float) {
        FlipCard(
            isFlipped = isFlipped,
            durationMillis = LONG_FLIP_DURATION_MILLIS,
            cameraDistance = cameraDistance,
            front = { Box(modifier = Modifier.size((SQUARE_SIDE_PX / density).dp).background(Color.Black)) },
            back = { Box(modifier = Modifier.size((SQUARE_SIDE_PX / density).dp).background(Color.Black)) }
        )
    }

    @androidx.compose.runtime.Composable
    private fun FlipCardLabeled(
        isFlipped: Boolean,
        modifier: Modifier = Modifier,
        frontLabel: String = "one",
        backLabel: String = "one"
    ) {
        FlipCard(
            front = { Text("front $frontLabel") },
            back = { Text("back $backLabel") },
            isFlipped = isFlipped,
            modifier = modifier,
            durationMillis = FLIP_DURATION_MILLIS,
            delayMillis = 0,
            cameraDistance = MotionDefaults.FLIP_CAMERA_DISTANCE
        )
    }

    @androidx.compose.runtime.Composable
    private fun CountUpAll(
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
        CountUpText(
            value = value,
            modifier = modifier,
            startValue = startValue,
            style = style,
            color = color,
            stepDelayMillis = stepDelayMillis,
            maxStepCount = maxStepCount,
            maxTotalMillis = maxTotalMillis,
            formatter = formatter,
            juiceState = juiceState,
            onStep = onStep
        )
    }

    @androidx.compose.runtime.Composable
    private fun FlipCardSquare(
        isFlipped: Boolean,
        delayMillis: Int = 0,
        durationMillis: Int = FLIP_DURATION_MILLIS,
        cameraDistance: Float = MotionDefaults.FLIP_CAMERA_DISTANCE
    ) {
        Box(modifier = Modifier.background(Color.White).size(ROOT_DP.dp), contentAlignment = Alignment.Center) {
            FlipCard(
                isFlipped = isFlipped,
                modifier = Modifier,
                delayMillis = delayMillis,
                durationMillis = durationMillis,
                cameraDistance = cameraDistance,
                front = { Box(modifier = Modifier.size(SQUARE_DP.dp).background(Color.Black)) },
                back = { Box(modifier = Modifier.size(SQUARE_DP.dp).background(Color.Black)) }
            )
        }
    }

    private fun SemanticsNodeInteraction.textStyle(): TextStyle {
        val layoutResults = mutableListOf<TextLayoutResult>()
        fetchSemanticsNode().config[SemanticsActions.GetTextLayoutResult].action?.invoke(layoutResults)
        return layoutResults.first().layoutInput.style
    }

    private fun androidx.compose.ui.graphics.ImageBitmap.countNonWhite(): Int {
        val pixels = toPixelMap()
        var count = 0
        for (x in 0 until width) for (y in 0 until height) if (pixels[x, y] != Color.White) count++
        return count
    }

    private companion object {
        const val TAG_A = "tag a"
        const val TAG_B = "tag b"
        const val FRONT_TEXT = "front face"
        const val BACK_TEXT = "back face"
        const val STEP_DELAY_MILLIS = 50L
        const val LONG_DELAY_MILLIS = 1000L
        const val SETTLE_MILLIS = 500L
        const val FRAME_MILLIS = 16L
        const val PEAK_FRAME_COUNT = 12
        const val LONG_MILLIS = 2000
        const val FLIP_DURATION_MILLIS = 400
        const val LONG_FLIP_DURATION_MILLIS = 4000
        const val LONG_FLIP_MID_MILLIS = 1000L
        const val FLIP_SETTLE_MILLIS = 600L
        const val FLIP_DELAY_MILLIS = 300
        const val ROOT_DP = 100
        const val ROOT_PX = 100
        const val FULL_DP = 40
        const val HALF_DP = 20
        const val SQUARE_DP = 40
        const val SQUARE_PX = 1600
        const val SQUARE_SIDE_PX = 40
        const val TOLERANCE = 0.02f
    }
}
