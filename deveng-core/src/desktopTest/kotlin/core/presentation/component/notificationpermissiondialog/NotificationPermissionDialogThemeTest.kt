package core.presentation.component.notificationpermissiondialog

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import core.presentation.theme.AppTheme
import core.presentation.theme.DefaultComponentTheme
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class NotificationPermissionDialogThemeTest {

    @Test
    fun showDialog_withCustomHorizontalPadding_narrowsCardByThemeValue() = runComposeUiTest {
        var componentTheme by mutableStateOf(DefaultComponentTheme)
        setContent {
            AppTheme(darkTheme = false, componentTheme = componentTheme) {
                NotificationPermissionDialog(
                    isVisible = true,
                    title = TITLE,
                    body = LONG_BODY,
                    primaryButtonLabel = ENABLE_LABEL,
                    laterButtonLabel = LATER_LABEL,
                    onDismissRequest = {},
                    onEnableButtonClick = {}
                )
            }
        }
        waitForIdle()
        val defaultButtonWidth = onNodeWithText(ENABLE_LABEL).getUnclippedBoundsInRoot().width

        componentTheme = CUSTOM_THEME
        waitForIdle()
        val customButtonWidth = onNodeWithText(ENABLE_LABEL).getUnclippedBoundsInRoot().width

        val paddingChange = CUSTOM_THEME.notificationPermissionDialog.horizontalPadding -
            DefaultComponentTheme.notificationPermissionDialog.horizontalPadding
        val widthChange = defaultButtonWidth - customButtonWidth
        assertTrue(
            abs((paddingChange * 2).value - widthChange.value) <= DP_TOLERANCE,
            "expected ${paddingChange * 2} but was $widthChange"
        )
    }

    private companion object {
        const val DP_TOLERANCE = 0.5f
        const val TITLE = "Stay in touch"
        const val LONG_BODY = "Turn on notifications to hear when your partner answers, when a new photo arrives " +
            "and when a reminder you set is due."
        const val ENABLE_LABEL = "Turn on"
        const val LATER_LABEL = "Not now"
        val CUSTOM_THEME = DefaultComponentTheme.copy(
            notificationPermissionDialog = DefaultComponentTheme.notificationPermissionDialog.copy(horizontalPadding = 40.dp)
        )
    }
}
