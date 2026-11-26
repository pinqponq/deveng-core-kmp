package core.domain.camera.controller

internal actual suspend fun CameraController.awaitPreviewStarted() {
    previewStartJob?.join()
}
