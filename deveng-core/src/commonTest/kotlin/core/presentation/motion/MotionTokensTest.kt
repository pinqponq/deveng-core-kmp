package core.presentation.motion

import androidx.compose.animation.core.SnapSpec
import androidx.compose.animation.core.TweenSpec
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

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
}
