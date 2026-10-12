package core.presentation.component.scrollablecontentdialog

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
class ScrollableContentDialogScreenshotTest {

    @Test
    fun dialog_withCheckedCheckbox_matchesBaseline() = runDesktopComposeUiTest(width = CANVAS_WIDTH_PX, height = CANVAS_HEIGHT_PX) {
        setContent {
            CompositionLocalProvider(LocalDensity provides Density(density = SCREENSHOT_DENSITY)) {
                AppTheme(darkTheme = false) {
                    ScrollableContentDialog(
                        title = TITLE,
                        content = CONTENT,
                        isChecked = true,
                        isDialogVisible = true,
                        isCheckboxVisible = true,
                        onBackIconClick = {},
                        onDismissRequest = {}
                    )
                }
            }
        }
        waitForIdle()

        onNode(isDialog()).captureRoboImage(filePath = screenshotPath(name = "scrollable_content_dialog_checked"))
    }

    private companion object {
        const val CANVAS_WIDTH_PX = 720
        const val CANVAS_HEIGHT_PX = 1000
        const val TITLE = "Terms of Use"
        const val CONTENT = "# Terms\n\nThese terms apply to **every** account.\n- First rule\n- Second rule"
    }
}
