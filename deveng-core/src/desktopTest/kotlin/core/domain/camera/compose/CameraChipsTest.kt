package core.domain.camera.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import core.domain.camera.enums.CameraDeviceType
import core.presentation.theme.AppTheme
import core.testing.SCREENSHOT_DENSITY
import core.testing.screenshotPath
import io.github.takahirom.roborazzi.captureRoboImage
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class CameraChipsTest {

    @Test
    fun backCameraZoomChips_everyChipHasTouchTargetOfAtLeast48Dp() = runDesktopComposeUiTest {
        setContent { BackZoomChips(onZoomChange = {}) }

        listOf("0.5x", "1x", "2x", "10x").forEach { label ->
            zoomChip(label)
                .assertWidthIsAtLeast(MINIMUM_TOUCH_TARGET)
                .assertHeightIsAtLeast(MINIMUM_TOUCH_TARGET)
        }
    }

    @Test
    fun backCameraZoomChips_tapOnChip_selectsItsZoomLevel() = runDesktopComposeUiTest {
        val selectedZoomLevels = mutableListOf<Float>()
        setContent { BackZoomChips(onZoomChange = { selectedZoomLevels += it }) }

        zoomChip("0.5x").performClick()
        zoomChip("2x").performClick()

        assertEquals(listOf(0.5f, 2f), selectedZoomLevels)
    }

    @Test
    fun frontCameraModeChips_everyChipHasTouchTargetOfAtLeast48Dp() = runDesktopComposeUiTest {
        setContent { AppTheme { FrontCameraModeChips(isWideSelfie = false, onSelect = {}) } }

        listOf("Person", "Group").forEach { description ->
            modeChip(description)
                .assertWidthIsAtLeast(MINIMUM_TOUCH_TARGET)
                .assertHeightIsAtLeast(MINIMUM_TOUCH_TARGET)
        }
    }

    @Test
    fun frontCameraModeChips_tapOnGroup_selectsWideSelfie() = runDesktopComposeUiTest {
        val selections = mutableListOf<Boolean>()
        setContent { AppTheme { FrontCameraModeChips(isWideSelfie = false, onSelect = { selections += it }) } }

        modeChip("Group").performClick()

        assertEquals(listOf(true), selections)
    }

    @Test
    fun backCameraZoomChips_atZoom1_marksOnly1xSelected() = runDesktopComposeUiTest {
        setContent { BackZoomChips(zoomLevel = 1f, onZoomChange = {}) }

        zoomChip("1x").assertIsSelected()
        listOf("0.5x", "2x", "10x").forEach { label -> zoomChip(label).assertIsNotSelected() }
    }

    @Test
    fun backCameraZoomChips_slightlyAboveStop_stillMarksThatStopSelected() = runDesktopComposeUiTest {
        setContent { BackZoomChips(zoomLevel = 2.1f, onZoomChange = {}) }

        zoomChip("2x").assertIsSelected()
        listOf("0.5x", "1x", "10x").forEach { label -> zoomChip(label).assertIsNotSelected() }
    }

    @Test
    fun backCameraZoomChips_slightlyBelowStop_stillMarksThatStopSelected() = runDesktopComposeUiTest {
        setContent { BackZoomChips(zoomLevel = 1.9f, onZoomChange = {}) }

        zoomChip("2x").assertIsSelected()
        zoomChip("1x").assertIsNotSelected()
    }

    @Test
    fun frontCameraModeChips_wideSelfie_marksOnlyGroupSelected() = runDesktopComposeUiTest {
        setContent { AppTheme { FrontCameraModeChips(isWideSelfie = true, onSelect = {}) } }

        modeChip("Group").assertIsSelected()
        modeChip("Person").assertIsNotSelected()
    }

    @Test
    fun cameraChips_matchBaseline() = runDesktopComposeUiTest(width = CANVAS_WIDTH_PX, height = CANVAS_HEIGHT_PX) {
        setContent {
            CompositionLocalProvider(LocalDensity provides Density(density = SCREENSHOT_DENSITY)) {
                AppTheme {
                    Column(
                        modifier = Modifier
                            .background(PREVIEW_BACKGROUND)
                            .padding(16.dp),
                    ) {
                        BackCameraZoomChips(
                            zoomLevel = 1f,
                            cameraDeviceType = CameraDeviceType.DEFAULT,
                            maxZoom = 10f,
                            onZoomChange = {},
                        )
                        Box(modifier = Modifier.padding(top = 16.dp)) {
                            FrontCameraModeChips(isWideSelfie = false, onSelect = {})
                        }
                    }
                }
            }
        }

        waitForIdle()

        onRoot().captureRoboImage(filePath = screenshotPath(name = "camera_zoom_and_selfie_chips"))
    }

    @androidx.compose.runtime.Composable
    private fun BackZoomChips(zoomLevel: Float = 1f, onZoomChange: (Float) -> Unit) {
        AppTheme {
            BackCameraZoomChips(
                zoomLevel = zoomLevel,
                cameraDeviceType = CameraDeviceType.DEFAULT,
                maxZoom = 10f,
                onZoomChange = onZoomChange,
            )
        }
    }

    private fun ComposeUiTest.zoomChip(label: String): SemanticsNodeInteraction =
        onNode(hasClickAction() and hasAnyDescendant(hasText(label)), useUnmergedTree = true)

    private fun ComposeUiTest.modeChip(description: String): SemanticsNodeInteraction =
        onNode(hasClickAction() and hasAnyDescendant(hasContentDescription(description)), useUnmergedTree = true)

    private companion object {
        const val CANVAS_WIDTH_PX = 600
        const val CANVAS_HEIGHT_PX = 400
        val MINIMUM_TOUCH_TARGET = 48.dp
        val PREVIEW_BACKGROUND = Color(0xFF3A3A3A)
    }
}
