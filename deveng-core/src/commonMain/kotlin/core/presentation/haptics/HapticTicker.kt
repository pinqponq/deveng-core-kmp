package core.presentation.haptics

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import core.presentation.motion.MotionDefaults
import kotlin.time.TimeSource

/**
 * Haptic ticks of three strengths for steps and reveals. [light] ticks that come faster than
 * [minLightIntervalMillis] are dropped, so a fast counter does not turn into a continuous buzz.
 * [medium] and [strong] always play.
 */
class HapticTicker internal constructor(
    private val hapticFeedback: HapticFeedback,
    private val minLightIntervalMillis: Long,
    private val currentTimeMillis: () -> Long
) {
    private var lastLightTickMillis: Long? = null

    /** A soft tick for each step of a counter or a list. */
    fun light() {
        val nowMillis = currentTimeMillis()
        val previousTickMillis = lastLightTickMillis
        if (previousTickMillis != null && nowMillis - previousTickMillis < minLightIntervalMillis) return
        lastLightTickMillis = nowMillis
        hapticFeedback.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
    }

    /** A clear tick for a selection or a landing. */
    fun medium() {
        hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
    }

    /** A strong tick for a reveal. */
    fun strong() {
        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
    }
}

/**
 * Remembers a [HapticTicker] bound to the current [LocalHapticFeedback].
 *
 * @param minLightIntervalMillis Shortest time between two [HapticTicker.light] ticks.
 */
@Composable
fun rememberHapticTicker(
    minLightIntervalMillis: Long = MotionDefaults.HAPTIC_MIN_INTERVAL_MILLIS
): HapticTicker {
    val hapticFeedback = LocalHapticFeedback.current
    return remember(hapticFeedback, minLightIntervalMillis) {
        val startMark = TimeSource.Monotonic.markNow()
        HapticTicker(
            hapticFeedback = hapticFeedback,
            minLightIntervalMillis = minLightIntervalMillis,
            currentTimeMillis = { startMark.elapsedNow().inWholeMilliseconds }
        )
    }
}
