package core.presentation.component.keepscreenon

import androidx.compose.runtime.Composable

/**
 * Keeps the device screen awake while this composable is in the composition.
 * Android and iOS only; desktop and web do nothing.
 */
@Composable
expect fun KeepScreenOn()
