package global.deveng.core

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import core.presentation.component.CustomButton
import core.presentation.component.CustomIconButton
import core.presentation.component.emaildisplayfield.EmailDisplayField
import core.presentation.component.mediaviewer.MediaViewerHeader
import core.presentation.component.notificationpermissiondialog.NotificationPermissionDialog
import core.presentation.component.promotwobuttoncard.PromoTwoButtonCard
import core.presentation.component.scrollablecontentdialog.ScrollableContentDialog
import core.presentation.theme.ComponentTheme
import core.presentation.theme.CoreSemiBoldTextStyle
import core.presentation.theme.LocalComponentTheme
import deveng_core_kmp.sample.composeapp.generated.resources.Res
import deveng_core_kmp.sample.composeapp.generated.resources.ic_arrow_left

private const val LAYOUT_THEME_DEMO_TERMS = "# Terms\n\nThese terms explain how the app stores your photos.\n\n" +
    "- Photos stay on your device until you share them.\n- You can delete a shared photo at any time."

/**
 * Playground for the layout values in the themes of PromoTwoButtonCard, ScrollableContentDialog,
 * MediaViewerHeader, NotificationPermissionDialog and EmailDisplayField. "Custom layout" applies a
 * compact [ComponentTheme] with other paddings and sizes, so both looks can be compared on one screen.
 */
@Composable
internal fun ComponentLayoutThemeDemoScreen(onBack: () -> Unit) {
    var isCustomLayoutApplied by remember { mutableStateOf(false) }
    var isTermsDialogVisible by remember { mutableStateOf(false) }
    var isTermsApproved by remember { mutableStateOf(false) }
    var isNotificationDialogVisible by remember { mutableStateOf(false) }
    val baseTheme = LocalComponentTheme.current
    val componentTheme = if (isCustomLayoutApplied) compactLayoutTheme(baseTheme) else baseTheme

    CompositionLocalProvider(LocalComponentTheme provides componentTheme) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF121212))
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CustomIconButton(
                icon = Res.drawable.ic_arrow_left,
                iconDescription = "Back",
                onClick = onBack
            )

            Text(
                text = if (isCustomLayoutApplied) "Custom layout theme" else "Default layout theme",
                color = Color.White,
                style = CoreSemiBoldTextStyle().copy(fontSize = 18.sp)
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CustomButton(
                    modifier = Modifier.weight(1f),
                    text = "Default layout",
                    onClick = { isCustomLayoutApplied = false }
                )
                CustomButton(
                    modifier = Modifier.weight(1f),
                    text = "Custom layout",
                    onClick = { isCustomLayoutApplied = true }
                )
            }

            PromoTwoButtonCard(
                title = "Stay in touch",
                body = "Turn on notifications to hear when someone answers.",
                primaryButtonLabel = "Turn on",
                secondaryButtonLabel = "Not now",
                onPrimaryClick = {},
                onSecondaryClick = {}
            )

            EmailDisplayField(
                email = "person@example.com",
                isEditTextVisible = true
            )

            Box(modifier = Modifier.fillMaxWidth().background(Color.Black)) {
                MediaViewerHeader(
                    onCloseButtonClick = {},
                    actions = { Text(text = "Share", color = Color.White) }
                )
            }

            CustomButton(
                modifier = Modifier.fillMaxWidth(),
                text = "Open ScrollableContentDialog",
                onClick = { isTermsDialogVisible = true }
            )

            CustomButton(
                modifier = Modifier.fillMaxWidth(),
                text = "Open NotificationPermissionDialog",
                onClick = { isNotificationDialogVisible = true }
            )
        }

        ScrollableContentDialog(
            title = "Terms",
            content = LAYOUT_THEME_DEMO_TERMS,
            isChecked = isTermsApproved,
            isDialogVisible = isTermsDialogVisible,
            isCheckboxVisible = true,
            onBackIconClick = { isTermsDialogVisible = false },
            onDismissRequest = { isTermsDialogVisible = false },
            onCheckIconClick = { isTermsApproved = !isTermsApproved }
        )

        NotificationPermissionDialog(
            isVisible = isNotificationDialogVisible,
            title = "Stay in touch",
            body = "Turn on notifications to hear when someone answers.",
            primaryButtonLabel = "Turn on",
            onDismissRequest = { isNotificationDialogVisible = false },
            onEnableButtonClick = { isNotificationDialogVisible = false }
        )
    }
}

private fun compactLayoutTheme(baseTheme: ComponentTheme): ComponentTheme = baseTheme.copy(
    promoTwoButtonCard = baseTheme.promoTwoButtonCard.copy(
        horizontalPadding = 16.dp,
        verticalPadding = 20.dp,
        contentSpacing = 4.dp,
        bodyHorizontalPadding = 0.dp,
        buttonVerticalPadding = 10.dp,
        secondaryButtonBorderWidth = 2.dp
    ),
    scrollableContentDialog = baseTheme.scrollableContentDialog.copy(
        headerTopPadding = 12.dp,
        headerBottomPadding = 8.dp,
        titleHorizontalPadding = 56.dp,
        contentHorizontalPadding = 32.dp,
        approvalRowBottomPadding = 16.dp
    ),
    mediaViewerHeader = baseTheme.mediaViewerHeader.copy(
        outerHorizontalPadding = 16.dp,
        panelCornerRadius = 24.dp,
        closeButtonSize = 36.dp,
        closeIconSize = 18.dp
    ),
    notificationPermissionDialog = baseTheme.notificationPermissionDialog.copy(horizontalPadding = 48.dp),
    emailDisplayField = baseTheme.emailDisplayField.copy(editTextEndPadding = 24.dp)
)
