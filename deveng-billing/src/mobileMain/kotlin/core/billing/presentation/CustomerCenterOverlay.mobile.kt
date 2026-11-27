package core.billing.presentation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.revenuecat.purchases.kmp.ui.revenuecatui.CustomerCenter

@Composable
actual fun CustomerCenterOverlay(
    isVisible: Boolean,
    onDismiss: () -> Unit,
) {
    if (isVisible) {
        CustomerCenter(
            modifier = Modifier.fillMaxSize(),
            onDismiss = onDismiss,
        )
    }
}
