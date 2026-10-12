package core.presentation.component.notificationpermissiondialog

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.unit.Density
import core.presentation.theme.AppTheme
import core.testing.SCREENSHOT_DENSITY
import core.testing.screenshotPath
import io.github.takahirom.roborazzi.captureRoboImage
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class NotificationPermissionDialogScreenshotTest {

    @Test
    fun dialog_whenVisible_matchesBaseline() = runDesktopComposeUiTest(width = CANVAS_WIDTH_PX, height = CANVAS_HEIGHT_PX) {
        setContent {
            CompositionLocalProvider(LocalDensity provides Density(density = SCREENSHOT_DENSITY)) {
                AppTheme(darkTheme = false) {
                    NotificationPermissionDialog(
                        isVisible = true,
                        title = TITLE,
                        body = BODY,
                        primaryButtonLabel = ENABLE_LABEL,
                        laterButtonLabel = LATER_LABEL,
                        onDismissRequest = {},
                        onEnableButtonClick = {}
                    )
                }
            }
        }
        waitForIdle()

        onNode(isDialog()).captureRoboImage(filePath = screenshotPath(name = "notification_permission_dialog_visible"))
    }

    private companion object {
        const val CANVAS_WIDTH_PX = 720
        const val CANVAS_HEIGHT_PX = 1000
        const val TITLE = "Stay in touch"
        const val BODY = "Turn on notifications to hear when your partner answers."
        const val ENABLE_LABEL = "Turn on"
        const val LATER_LABEL = "Not now"
    }
}
