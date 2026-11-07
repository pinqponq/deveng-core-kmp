package core.domain.camera.controller

import androidx.camera.core.ExperimentalZeroShutterLag
import androidx.camera.core.ImageCapture
import core.domain.camera.enums.StillCaptureMode

@OptIn(ExperimentalZeroShutterLag::class)
internal fun StillCaptureMode.toImageCaptureMode(): Int = when (this) {
    StillCaptureMode.ZERO_SHUTTER_LAG -> ImageCapture.CAPTURE_MODE_ZERO_SHUTTER_LAG
    StillCaptureMode.MINIMIZE_LATENCY -> ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY
    StillCaptureMode.MAXIMIZE_QUALITY -> ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY
}
