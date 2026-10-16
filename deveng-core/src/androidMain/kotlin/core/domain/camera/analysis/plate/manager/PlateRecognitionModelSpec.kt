package core.domain.camera.analysis.plate.manager

import core.domain.camera.analysis.plate.PlateRecognitionConfig
import java.io.File

/**
 * The detector and OCR model files plus the runtime settings used to prepare them.
 *
 * File-backed model artifacts remain owned by the consumer application. The model manager only
 * owns the native sessions created from these files.
 */
data class PlateRecognitionModelSpec(
    val detectorModelFile: File,
    val ocrModelFile: File,
    val config: PlateRecognitionConfig = PlateRecognitionConfig()
)