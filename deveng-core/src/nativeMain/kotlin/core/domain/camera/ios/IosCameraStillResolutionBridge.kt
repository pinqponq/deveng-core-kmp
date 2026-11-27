package core.domain.camera.ios

/**
 * Set from the host app (e.g. Rindle [MainViewController]) via Swift/AVFoundation.
 * Returns comma-separated still sizes: `"4032x3024,1920x1080,..."` (largest first).
 */
object IosCameraStillResolutionBridge {
    var supportedSizesCsvProvider: ((isFrontCamera: Boolean) -> String)? = null
}
