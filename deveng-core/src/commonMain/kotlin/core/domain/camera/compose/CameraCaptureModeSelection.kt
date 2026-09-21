package core.domain.camera.compose

/**
 * Hands the Photo/Video choice of [DefaultCameraPreview] to the host, e.g. to ask for microphone access before
 * video and stay on Photo when it is refused. The preview shows [isVideoModeSelected] and reports a tap on the
 * mode row through [onCaptureModeSelect] instead of switching by itself.
 */
data class CameraCaptureModeSelection(
    val isVideoModeSelected: Boolean,
    val onCaptureModeSelect: (isVideoMode: Boolean) -> Unit,
)
