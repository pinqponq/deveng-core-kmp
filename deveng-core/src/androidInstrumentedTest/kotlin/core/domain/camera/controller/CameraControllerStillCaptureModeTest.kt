package core.domain.camera.controller

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import core.domain.camera.enums.AspectRatio
import core.domain.camera.enums.CameraDeviceType
import core.domain.camera.enums.CameraLens
import core.domain.camera.enums.Directory
import core.domain.camera.enums.FlashMode
import core.domain.camera.enums.ImageFormat
import core.domain.camera.enums.QualityPrioritization
import core.domain.camera.enums.StillCaptureMode
import core.domain.camera.enums.TorchMode
import kotlinx.coroutines.test.runTest
import kotlin.test.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CameraControllerStillCaptureModeTest {

    @Test
    fun getStillCaptureMode_withSpeedAndFlashOff_returnsZeroShutterLag() {
        assertEquals(StillCaptureMode.ZERO_SHUTTER_LAG, cameraController().getStillCaptureMode())
    }

    @Test
    fun setStillCaptureModeOverride_withAMode_makesItTheCurrentMode() = runTest {
        val controller = cameraController()

        controller.setStillCaptureModeOverride(StillCaptureMode.MINIMIZE_LATENCY)

        assertEquals(StillCaptureMode.MINIMIZE_LATENCY, controller.getStillCaptureMode())
    }

    @Test
    fun setStillCaptureModeOverride_withNull_returnsToTheResolvedMode() = runTest {
        val controller = cameraController()
        controller.setStillCaptureModeOverride(StillCaptureMode.MAXIMIZE_QUALITY)

        controller.setStillCaptureModeOverride(null)

        assertEquals(StillCaptureMode.ZERO_SHUTTER_LAG, controller.getStillCaptureMode())
    }

    private fun cameraController() = CameraController(
        context = InstrumentationRegistry.getInstrumentation().targetContext,
        lifecycleOwner = TestLifecycleOwner(),
        flashMode = FlashMode.OFF,
        torchMode = TorchMode.OFF,
        cameraLens = CameraLens.BACK,
        imageFormat = ImageFormat.JPEG,
        qualityPriority = QualityPrioritization.SPEED,
        directory = Directory.PICTURES,
        cameraDeviceType = CameraDeviceType.WIDE_ANGLE,
        returnFilePath = false,
        aspectRatio = AspectRatio.RATIO_16_9,
        plugins = mutableListOf(),
    )

    private class TestLifecycleOwner : LifecycleOwner {
        override val lifecycle: Lifecycle = LifecycleRegistry.createUnsafe(this)
    }
}
