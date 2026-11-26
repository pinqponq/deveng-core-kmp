package core.domain.camera.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import core.presentation.theme.AppTheme
import core.testing.SCREENSHOT_DENSITY
import core.testing.screenshotPath
import io.github.takahirom.roborazzi.captureRoboImage
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class CameraModeSwitcherTest {

    @Test
    fun render_eachMode_hasTouchTargetOfAtLeast48Dp() = runDesktopComposeUiTest {
        setContent { ModeSwitcherUnderTest(onModeChange = {}) }

        listOf(PHOTO_LABEL, VIDEO_LABEL).forEach { label ->
            mode(label)
                .assertWidthIsAtLeast(MINIMUM_TOUCH_TARGET)
                .assertHeightIsAtLeast(MINIMUM_TOUCH_TARGET)
        }
    }

    @Test
    fun tapOnMode_whenEnabled_reportsThatMode() = runDesktopComposeUiTest {
        val selectedModes = mutableListOf<CameraCaptureMode>()
        setContent { ModeSwitcherUnderTest(onModeChange = { selectedModes += it }) }

        mode(VIDEO_LABEL).performClick()
        mode(PHOTO_LABEL).performClick()

        assertEquals(listOf(CameraCaptureMode.Video, CameraCaptureMode.Photo), selectedModes)
    }

    @Test
    fun tapOnMode_whenDisabled_reportsNothing() = runDesktopComposeUiTest {
        val selectedModes = mutableListOf<CameraCaptureMode>()
        setContent { ModeSwitcherUnderTest(onModeChange = { selectedModes += it }, enabled = false) }

        mode(VIDEO_LABEL).performClick()

        assertEquals(emptyList(), selectedModes)
    }

    @Test
    fun render_onDarkPreview_showsSelectedPhotoAndBrighterVideoLabel() = runDesktopComposeUiTest(
        width = SCREENSHOT_WIDTH_PX,
        height = SCREENSHOT_HEIGHT_PX,
    ) {
        setContent {
            CompositionLocalProvider(LocalDensity provides Density(density = SCREENSHOT_DENSITY)) {
                Box(modifier = Modifier.background(Color.DarkGray).padding(8.dp)) {
                    ModeSwitcherUnderTest(onModeChange = {})
                }
            }
        }
        onRoot().captureRoboImage(filePath = screenshotPath(name = "camera_mode_switcher"))
    }

    @Composable
    private fun ModeSwitcherUnderTest(
        onModeChange: (CameraCaptureMode) -> Unit,
        enabled: Boolean = true,
    ) {
        AppTheme {
            ModeSwitcher(
                currentMode = CameraCaptureMode.Photo,
                onModeChange = onModeChange,
                enabled = enabled,
            )
        }
    }

    private fun ComposeUiTest.mode(label: String): SemanticsNodeInteraction =
        onNode(hasClickAction() and hasText(label), useUnmergedTree = false)

    private companion object {
        const val PHOTO_LABEL = "PHOTO"
        const val VIDEO_LABEL = "VIDEO"
        const val SCREENSHOT_WIDTH_PX = 320
        const val SCREENSHOT_HEIGHT_PX = 140
        val MINIMUM_TOUCH_TARGET = 48.dp
    }
}
