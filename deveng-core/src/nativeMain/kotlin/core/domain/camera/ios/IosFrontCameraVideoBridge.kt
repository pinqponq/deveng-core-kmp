package core.domain.camera.ios

/**
 * Host-provided export that horizontally flips front-camera recordings so saved files match
 * non-mirrored stills (preview stays mirrored for selfie UX).
 */
object IosFrontCameraVideoBridge {
    var unmirrorRecordedVideoInPlace: ((filePath: String) -> Boolean)? = null
}
