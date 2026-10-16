package core.domain.camera.analysis.plate.engine

import core.domain.camera.analysis.plate.PlateRecognitionConfig
import java.io.File

/**
 * Encapsulates the detection and OCR model engines required for plate recognition.
 */
internal class PlateRecognitionEngines(
    val detector: PlateDetectorEngine,
    val ocr: PlateOcrEngine
) : AutoCloseable {
    override fun close() {
        try {
            detector.close()
        } finally {
            ocr.close()
        }
    }

    companion object {
        fun open(
            detectorModelFile: File,
            ocrModelFile: File,
            config: PlateRecognitionConfig
        ): PlateRecognitionEngines {
            val detector = NanoDetPlateDetectorEngine(
                modelFile = detectorModelFile,
                config = config.detectorConfig
            )
            return try {
                val ocr = FastPlateOcrEngine(
                    modelFile = ocrModelFile,
                    config = config.ocrConfig
                )
                PlateRecognitionEngines(detector = detector, ocr = ocr)
            } catch (error: Throwable) {
                detector.close()
                throw error
            }
        }
    }
}