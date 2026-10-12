package core.presentation.component.notificationpermissiondialog

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import core.presentation.theme.AppTheme
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class NotificationPermissionDialogTest {

    @Test
    fun clickEnableButton_whenVisible_sendsEnableClickOnly() = runComposeUiTest {
        var enableClickCount = 0
        var dismissCount = 0
        setContent {
            AppTheme {
                NotificationPermissionDialog(
                    isVisible = true,
                    title = TITLE,
                    body = BODY,
                    primaryButtonLabel = ENABLE_LABEL,
                    laterButtonLabel = LATER_LABEL,
                    onDismissRequest = { dismissCount++ },
                    onEnableButtonClick = { enableClickCount++ }
                )
            }
        }

        onNodeWithText(ENABLE_LABEL).performClick()

        assertEquals(1, enableClickCount)
        assertEquals(0, dismissCount)
    }

    @Test
    fun clickLaterButton_whenVisible_sendsDismissOnly() = runComposeUiTest {
        var enableClickCount = 0
        var dismissCount = 0
        setContent {
            AppTheme {
                NotificationPermissionDialog(
                    isVisible = true,
                    title = TITLE,
                    body = BODY,
                    primaryButtonLabel = ENABLE_LABEL,
                    laterButtonLabel = LATER_LABEL,
                    onDismissRequest = { dismissCount++ },
                    onEnableButtonClick = { enableClickCount++ }
                )
            }
        }

        onNodeWithText(LATER_LABEL).performClick()

        assertEquals(0, enableClickCount)
        assertEquals(1, dismissCount)
    }

    @Test
    fun render_whenHidden_showsNothing() = runComposeUiTest {
        setContent {
            AppTheme {
                NotificationPermissionDialog(
                    isVisible = false,
                    title = TITLE,
                    body = BODY,
                    primaryButtonLabel = ENABLE_LABEL,
                    laterButtonLabel = LATER_LABEL,
                    onDismissRequest = {},
                    onEnableButtonClick = {}
                )
            }
        }

        onAllNodesWithText(TITLE).assertCountEquals(0)
    }

    private companion object {
        const val TITLE = "Stay in touch"
        const val BODY = "Turn on notifications to hear when your partner answers."
        const val ENABLE_LABEL = "Turn on"
        const val LATER_LABEL = "Not now"
    }
}
