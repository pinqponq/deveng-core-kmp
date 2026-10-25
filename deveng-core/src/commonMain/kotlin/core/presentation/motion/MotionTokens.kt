package core.presentation.motion

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import kotlin.math.roundToInt

/**
 * Shared motion values (durations, easings, springs) so every screen animates with the same timing.
 */
object MotionTokens {
    const val DURATION_MICRO_MILLIS = 50
    const val DURATION_QUICK_MILLIS = 100
    const val DURATION_SHORT_MILLIS = 150
    const val DURATION_REGULAR_MILLIS = 200
    const val DURATION_MEDIUM_MILLIS = 300
    const val DURATION_LONG_MILLIS = 500
    const val SHIMMER_DURATION_MILLIS = 1200
    const val PRESSED_SCALE = 0.96f
    const val NAVIGATION_SLIDE_FRACTION = 0.25f

    const val DISTANCE_SCALED_SNAP_BACK_MAX_MILLIS = 320
    const val DISTANCE_SCALED_SNAP_BACK_MIN_MILLIS = 120
    const val DISTANCE_SCALED_FINISH_MAX_MILLIS = 200
    const val DISTANCE_SCALED_FINISH_MIN_MILLIS = 50

    val StandardEasing: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val StandardDecelerateEasing: Easing = CubicBezierEasing(0f, 0f, 0f, 1f)
    val AccelerateDecelerateEasing: Easing = CubicBezierEasing(0.42f, 0f, 0.58f, 1f)
    val DecelerateEasing: Easing = CubicBezierEasing(0f, 0f, 0.58f, 1f)
    val DistanceScaledEndingEasing: Easing = CubicBezierEasing(0.23f, 1f, 0.32f, 1f)
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

    /**
     * Time for a gesture to finish, growing with the distance left so a short move ends fast and a long one never drags.
     *
     * @param remainingFraction Distance left divided by the full distance; values outside 0 to 1 are clamped.
     * @param maxMillis Time for the full distance.
     * @param minMillis Lowest time returned.
     */
    fun distanceScaledDurationMillis(
        remainingFraction: Float,
        maxMillis: Int = DISTANCE_SCALED_SNAP_BACK_MAX_MILLIS,
        minMillis: Int = DISTANCE_SCALED_SNAP_BACK_MIN_MILLIS
    ): Int = maxOf((maxMillis * remainingFraction.coerceIn(0f, 1f)).roundToInt(), minMillis)
}
