package core.presentation.motion

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import kotlin.math.roundToInt

/**
 * Default screen transitions for NavHost or AnimatedContent: the new screen slides in from the end
 * and fades in, back navigation reverses it. Every transition is none while reduce motion is on.
 *
 * Example:
 * ```
 * val isReduceMotionEnabled = LocalReduceMotion.current
 * NavHost(
 *     enterTransition = { NavTransitions.enter(isReduceMotionEnabled) },
 *     exitTransition = { NavTransitions.exit(isReduceMotionEnabled) },
 *     popEnterTransition = { NavTransitions.popEnter(isReduceMotionEnabled) },
 *     popExitTransition = { NavTransitions.popExit(isReduceMotionEnabled) },
 *     ...
 * )
 * ```
 */
object NavTransitions {

    fun enter(isReduceMotionEnabled: Boolean): EnterTransition {
        if (isReduceMotionEnabled) return EnterTransition.None
        return slideInHorizontally(
            animationSpec = navigationAnimationSpec(),
            initialOffsetX = { fullWidth -> navigationSlideOffset(fullWidth) }
        ) + fadeIn(animationSpec = navigationAnimationSpec())
    }

    fun exit(isReduceMotionEnabled: Boolean): ExitTransition {
        if (isReduceMotionEnabled) return ExitTransition.None
        return slideOutHorizontally(
            animationSpec = navigationAnimationSpec(),
            targetOffsetX = { fullWidth -> -navigationSlideOffset(fullWidth) }
        ) + fadeOut(animationSpec = navigationAnimationSpec())
    }

    fun popEnter(isReduceMotionEnabled: Boolean): EnterTransition {
        if (isReduceMotionEnabled) return EnterTransition.None
        return slideInHorizontally(
            animationSpec = navigationAnimationSpec(),
            initialOffsetX = { fullWidth -> -navigationSlideOffset(fullWidth) }
        ) + fadeIn(animationSpec = navigationAnimationSpec())
    }

    fun popExit(isReduceMotionEnabled: Boolean): ExitTransition {
        if (isReduceMotionEnabled) return ExitTransition.None
        return slideOutHorizontally(
            animationSpec = navigationAnimationSpec(),
            targetOffsetX = { fullWidth -> navigationSlideOffset(fullWidth) }
        ) + fadeOut(animationSpec = navigationAnimationSpec())
    }

    private fun <T> navigationAnimationSpec(): FiniteAnimationSpec<T> = tween(
        durationMillis = MotionTokens.DURATION_MEDIUM_MILLIS,
        easing = MotionTokens.StandardEasing
    )
}

internal fun navigationSlideOffset(fullWidth: Int): Int =
    (fullWidth * MotionTokens.NAVIGATION_SLIDE_FRACTION).roundToInt()
