package core.presentation.haptics

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

/**
 * Haptic feedback for common moments. Android plays the matching `HapticFeedbackConstants`
 * (honoring the system touch-feedback setting); iOS plays UIKit feedback generators; desktop and
 * web do nothing.
 */
class AppHaptics(private val hapticFeedback: HapticFeedback) {

    fun tap() {
        hapticFeedback.performHapticFeedback(HapticFeedbackType.VirtualKey)
    }

    fun success() {
        hapticFeedback.performHapticFeedback(HapticFeedbackType.Confirm)
    }

    fun warning() {
        hapticFeedback.performHapticFeedback(HapticFeedbackType.Reject)
    }
}

@Composable
fun rememberAppHaptics(): AppHaptics {
    val hapticFeedback = LocalHapticFeedback.current
    return remember(hapticFeedback) { AppHaptics(hapticFeedback = hapticFeedback) }
}
