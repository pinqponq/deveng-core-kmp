package core.domain.camera.controller

/**
 * Suspends until the preview shows camera frames. On the web the browser opens the camera after
 * [CameraController.startSession] returns and may first ask the user for permission, so the camera stays in
 * the initializing state until then. On the other platforms [CameraController.startSession] is enough.
 */
internal expect suspend fun CameraController.awaitPreviewStarted()
