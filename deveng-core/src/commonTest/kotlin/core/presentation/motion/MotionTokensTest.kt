package core.presentation.motion

import androidx.compose.animation.core.SnapSpec
import androidx.compose.animation.core.TweenSpec
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class MotionTokensTest {

    @Test
    fun tweenOrSnap_withReduceMotion_returnsInstantSnap() {
        val animationSpec = MotionTokens.tweenOrSnap<Float>(isReduceMotionEnabled = true)

        assertEquals(0, assertIs<SnapSpec<Float>>(animationSpec).delay)
    }

    @Test
    fun tweenOrSnap_withoutReduceMotion_returnsTweenWithGivenTiming() {
        val animationSpec = MotionTokens.tweenOrSnap<Float>(
            isReduceMotionEnabled = false,
            durationMillis = MotionTokens.DURATION_LONG_MILLIS,
            easing = MotionTokens.EmphasizedDecelerateEasing
        )

        val tweenSpec = assertIs<TweenSpec<Float>>(animationSpec)
        assertEquals(MotionTokens.DURATION_LONG_MILLIS, tweenSpec.durationMillis)
        assertEquals(MotionTokens.EmphasizedDecelerateEasing, tweenSpec.easing)
    }

    @Test
    fun tweenOrSnap_withDefaults_usesMediumDurationAndStandardEasing() {
        val tweenSpec = assertIs<TweenSpec<Float>>(MotionTokens.tweenOrSnap<Float>(isReduceMotionEnabled = false))

        assertEquals(MotionTokens.DURATION_MEDIUM_MILLIS, tweenSpec.durationMillis)
        assertEquals(MotionTokens.StandardEasing, tweenSpec.easing)
    }

    @Test
    fun durations_followTheStudyScaleInOrder() {
        val durations = listOf(
            MotionTokens.DURATION_MICRO_MILLIS,
            MotionTokens.DURATION_QUICK_MILLIS,
            MotionTokens.DURATION_SHORT_MILLIS,
            MotionTokens.DURATION_REGULAR_MILLIS,
            MotionTokens.DURATION_MEDIUM_MILLIS
        )

        assertEquals(listOf(50, 100, 150, 200, 300), durations)
    }

    @Test
    fun easings_haveTheNamedCurvePoints() {
        assertEquals(0.5f, MotionTokens.AccelerateDecelerateEasing.transform(0.5f), 0.001f)
        assertTrue(MotionTokens.DecelerateEasing.transform(0.5f) > 0.5f)
        assertTrue(MotionTokens.DistanceScaledEndingEasing.transform(0.5f) > MotionTokens.DecelerateEasing.transform(0.5f))
        assertEquals(0f, MotionTokens.DistanceScaledEndingEasing.transform(0f), 0.001f)
        assertEquals(1f, MotionTokens.DistanceScaledEndingEasing.transform(1f), 0.001f)
    }

    @Test
    fun distanceScaledDuration_forFullDistance_returnsMax() {
        assertEquals(320, MotionTokens.distanceScaledDurationMillis(remainingFraction = 1f))
    }

    @Test
    fun distanceScaledDuration_forHalfDistance_returnsHalfOfMax() {
        assertEquals(160, MotionTokens.distanceScaledDurationMillis(remainingFraction = 0.5f))
    }

    @Test
    fun distanceScaledDuration_forShortDistance_returnsMin() {
        assertEquals(120, MotionTokens.distanceScaledDurationMillis(remainingFraction = 0.1f))
    }

    @Test
    fun distanceScaledDuration_outsideRange_isClamped() {
        assertEquals(320, MotionTokens.distanceScaledDurationMillis(remainingFraction = 2f))
        assertEquals(120, MotionTokens.distanceScaledDurationMillis(remainingFraction = -1f))
    }

    @Test
    fun distanceScaledDuration_withFinishValues_usesTheFinishScale() {
        val millis = MotionTokens.distanceScaledDurationMillis(
            remainingFraction = 0.5f,
            maxMillis = MotionTokens.DISTANCE_SCALED_FINISH_MAX_MILLIS,
            minMillis = MotionTokens.DISTANCE_SCALED_FINISH_MIN_MILLIS
        )

        assertEquals(100, millis)
    }
}
