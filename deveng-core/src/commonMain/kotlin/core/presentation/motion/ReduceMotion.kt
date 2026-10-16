package core.presentation.motion

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf

/**
 * Whether the user asked the system to reduce motion. [core.presentation.theme.AppTheme] provides
 * the system setting; core animations switch to instant changes while it is true.
 */
val LocalReduceMotion = compositionLocalOf { false }

/**
 * Reads the system reduce-motion setting and recomposes when it changes.
 * Android: "Remove animations" (animator duration scale 0). iOS: Reduce Motion.
 * Desktop and web always return false.
 */
@Composable
expect fun rememberSystemReduceMotion(): Boolean
