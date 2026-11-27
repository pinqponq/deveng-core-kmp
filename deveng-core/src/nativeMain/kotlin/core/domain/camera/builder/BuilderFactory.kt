package core.domain.camera.builder

/**
 * Creates an iOS-specific [CameraControllerBuilder].
 *
 * @return An instance of [CameraControllerBuilder].
 */
fun createIOSCameraControllerBuilder(): CameraControllerBuilder = IOSCameraControllerBuilder()
