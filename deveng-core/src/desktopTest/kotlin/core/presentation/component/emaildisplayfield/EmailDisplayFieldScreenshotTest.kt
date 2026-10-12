package core.presentation.component.emaildisplayfield

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import core.presentation.theme.AppTheme
import core.testing.SCREENSHOT_DENSITY
import core.testing.screenshotPath
import io.github.takahirom.roborazzi.captureRoboImage
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class EmailDisplayFieldScreenshotTest {

    @Test
    fun field_withEditTextAndError_matchesBaseline() = runDesktopComposeUiTest(width = CANVAS_WIDTH_PX, height = CANVAS_HEIGHT_PX) {
        setContent {
            CompositionLocalProvider(LocalDensity provides Density(density = SCREENSHOT_DENSITY)) {
                AppTheme(darkTheme = false) {
                    Box(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        EmailDisplayField(
                            email = EMAIL,
                            title = TITLE,
                            editText = EDIT_TEXT,
                            errorMessage = ERROR_MESSAGE,
                            isEditTextVisible = true,
                            isErrorMessageVisible = true
                        )
                    }
                }
            }
        }
        waitForIdle()

        onRoot().captureRoboImage(filePath = screenshotPath(name = "email_display_field_edit_error"))
    }

    private companion object {
        const val CANVAS_WIDTH_PX = 720
        const val CANVAS_HEIGHT_PX = 320
        const val EMAIL = "person@example.com"
        const val TITLE = "E-Mail"
        const val EDIT_TEXT = "edit"
        const val ERROR_MESSAGE = "Please enter a valid email address."
    }
}
