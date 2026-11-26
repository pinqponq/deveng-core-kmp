package core.domain.camera.controller

/**
 * Suspends until the browser has opened the camera or the attempt has failed. On the web the camera opens
 * after [CameraController.startSession] returns and the browser may first ask the user for permission, so the
 * camera stays in the initializing state until then. On the other platforms [CameraController.startSession]
 * is enough.
 */
internal expect suspend fun CameraController.awaitPreviewStarted()
