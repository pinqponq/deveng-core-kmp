package core.domain.camera.analysis.plate.engine

import core.domain.camera.analysis.AndroidCameraFrame
import core.domain.camera.analysis.nanodet.NanoDetConfig
import core.domain.camera.analysis.nanodet.NanoDetDetector
import core.domain.camera.analysis.nanodet.model.PlateDetectionResult
import java.io.File

internal class NanoDetPlateDetectorEngine(
    modelFile: File,
    config: NanoDetConfig
) : PlateDetectorEngine {
    private val detector = NanoDetDetector(
        modelFile = modelFile,
        config = config
    )

    override suspend fun detect(frame: AndroidCameraFrame): PlateDetectionResult? {
        return detector.analyze(frame)
    }

    override fun warmUp() {
        detector.warmUp()
    }

    override fun close() {
        detector.close()
    }
}