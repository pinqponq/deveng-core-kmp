package core.presentation.component.notificationpermissiondialog

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import core.presentation.component.Slot
import core.presentation.component.alertdialog.CustomDialog
import core.presentation.component.promotwobuttoncard.PromoTwoButtonCard
import global.deveng.deveng_core.generated.resources.Res
import global.deveng.deveng_core.generated.resources.shared_notification_permission_later
import org.jetbrains.compose.resources.stringResource

/**
 * Asks for notifications in the app's own words before the system dialog appears. iOS shows its
 * alert once per install and Android stops showing it after two refusals, so a refusal here costs
 * nothing while a refusal there cannot be taken back from inside the app.
 *
 * @param isVisible Whether the dialog is shown.
 * @param title Title of the dialog.
 * @param body Body text explaining why notifications are needed.
 * @param primaryButtonLabel Label of the button that enables notifications.
 * @param laterButtonLabel Label of the button that postpones the request.
 * @param heroContent Optional content displayed above the title, e.g. the app logo.
 * @param onDismissRequest Callback invoked when the dialog is dismissed or postponed.
 * @param onEnableButtonClick Callback invoked when the enable button is clicked.
 */
@Composable
fun NotificationPermissionDialog(
    isVisible: Boolean,
    title: String,
    body: String,
    primaryButtonLabel: String,
    laterButtonLabel: String = stringResource(Res.string.shared_notification_permission_later),
    heroContent: Slot? = null,
    onDismissRequest: () -> Unit,
    onEnableButtonClick: () -> Unit
) {
    if (isVisible) {
        CustomDialog(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            onDismissRequest = onDismissRequest,
            backgroundColor = Color.Transparent,
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true,
                usePlatformDefaultWidth = false
            )
        ) {
            PromoTwoButtonCard(
                title = title,
                body = body,
                primaryButtonLabel = primaryButtonLabel,
                secondaryButtonLabel = laterButtonLabel,
                heroContent = heroContent,
                onPrimaryClick = onEnableButtonClick,
                onSecondaryClick = onDismissRequest
            )
        }
    }
}
