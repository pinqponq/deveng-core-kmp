package core.domain.camera.enums

/**
 * How the camera takes a still photo. Android maps each value to the CameraX capture mode of the
 * same name; the other platforms have no such modes.
 */
enum class StillCaptureMode {
    ZERO_SHUTTER_LAG,
    MINIMIZE_LATENCY,
    MAXIMIZE_QUALITY,
}
