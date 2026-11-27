package core.domain.camera.compose

internal fun isShutterUsable(
    isShutterEnabled: Boolean,
    singleCaptureModeEnabled: Boolean,
    hasClaimedSingleCapture: Boolean,
): Boolean = isShutterEnabled && !(singleCaptureModeEnabled && hasClaimedSingleCapture)

/** An in-progress recording is stopped with the same button, so the button stays enabled while recording. */
internal fun isShutterButtonEnabled(
    isShutterEnabled: Boolean,
    singleCaptureModeEnabled: Boolean,
    hasClaimedSingleCapture: Boolean,
    isRecording: Boolean,
): Boolean = isRecording || isShutterUsable(isShutterEnabled, singleCaptureModeEnabled, hasClaimedSingleCapture)
