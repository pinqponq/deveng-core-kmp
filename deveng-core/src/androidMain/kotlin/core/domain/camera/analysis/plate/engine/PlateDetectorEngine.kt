package core.domain.camera.analysis.plate.engine

import core.domain.camera.analysis.AndroidCameraFrame
import core.domain.camera.analysis.nanodet.model.PlateDetectionResult

internal interface PlateDetectorEngine : AutoCloseable {
    suspend fun detect(frame: AndroidCameraFrame): PlateDetectionResult?

    fun warmUp() {}
}