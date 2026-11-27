package core.billing.presentation

import androidx.compose.runtime.Composable

/**
 * RevenueCat Customer Center shown full screen while [isVisible] is true; renders nothing on desktop and web.
 *
 * @param isVisible Whether the Customer Center is shown.
 * @param onDismiss Called when the user closes the Customer Center.
 */
@Composable
expect fun CustomerCenterOverlay(
    isVisible: Boolean,
    onDismiss: () -> Unit,
)
