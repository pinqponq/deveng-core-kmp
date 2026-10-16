package core.presentation.motion

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween

/**
 * Shared motion values (durations, easings, springs) so every screen animates with the same timing.
 */
object MotionTokens {
    const val DURATION_SHORT_MILLIS = 150
    const val DURATION_MEDIUM_MILLIS = 300
    const val DURATION_LONG_MILLIS = 500
    const val SHIMMER_DURATION_MILLIS = 1200
    const val PRESSED_SCALE = 0.96f
    const val NAVIGATION_SLIDE_FRACTION = 0.25f

    val StandardEasing: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val StandardDecelerateEasing: Easing = CubicBezierEasing(0f, 0f, 0f, 1f)
    val StandardAccelerateEasing: Easing = CubicBezierEasing(0.3f, 0f, 1f, 1f)
    val EmphasizedDecelerateEasing: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
    val EmphasizedAccelerateEasing: Easing = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)

    fun <T> defaultSpring(): SpringSpec<T> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMediumLow
    )

    fun <T> bouncySpring(): SpringSpec<T> = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMedium
    )

    fun <T> pressSpring(): SpringSpec<T> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessHigh
    )

    /**
     * A tween that becomes an instant change when reduce motion is on.
     *
     * @param isReduceMotionEnabled Usually [LocalReduceMotion].current.
     * @param durationMillis Duration of the tween when motion is allowed.
     * @param easing Easing of the tween when motion is allowed.
     */
    fun <T> tweenOrSnap(
        isReduceMotionEnabled: Boolean,
        durationMillis: Int = DURATION_MEDIUM_MILLIS,
        easing: Easing = StandardEasing
    ): FiniteAnimationSpec<T> {
        if (isReduceMotionEnabled) return snap()
        return tween(durationMillis = durationMillis, easing = easing)
    }
}
