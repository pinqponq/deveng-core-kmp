package core.domain.camera.controller

import androidx.camera.core.ExperimentalZeroShutterLag
import androidx.camera.core.ImageCapture
import androidx.test.ext.junit.runners.AndroidJUnit4
import core.domain.camera.enums.StillCaptureMode
import kotlin.test.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalZeroShutterLag::class)
@RunWith(AndroidJUnit4::class)
class StillCaptureModeCameraXTest {

    @Test
    fun toImageCaptureMode_withZeroShutterLag_returnsCameraXZeroShutterLag() {
        assertEquals(ImageCapture.CAPTURE_MODE_ZERO_SHUTTER_LAG, StillCaptureMode.ZERO_SHUTTER_LAG.toImageCaptureMode())
    }

    @Test
    fun toImageCaptureMode_withMinimizeLatency_returnsCameraXMinimizeLatency() {
        assertEquals(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY, StillCaptureMode.MINIMIZE_LATENCY.toImageCaptureMode())
    }

    @Test
    fun toImageCaptureMode_withMaximizeQuality_returnsCameraXMaximizeQuality() {
        assertEquals(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY, StillCaptureMode.MAXIMIZE_QUALITY.toImageCaptureMode())
    }
}
