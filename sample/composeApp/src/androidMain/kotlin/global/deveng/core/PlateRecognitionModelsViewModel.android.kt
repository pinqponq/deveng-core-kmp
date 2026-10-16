package global.deveng.core

import androidx.lifecycle.ViewModel
import core.domain.camera.analysis.plate.PlateRecognitionConfig
import core.domain.camera.analysis.plate.manager.PlateRecognitionModelManager
import core.domain.camera.analysis.plate.manager.PlateRecognitionModelSpec
import core.domain.camera.analysis.plate.manager.PlateRecognitionPreparationState
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal const val PLATE_DETECTOR_MODEL_NAME = "plate_detector.onnx"
internal const val FAST_PLATE_OCR_MODEL_NAME = "fast_plate_ocr.onnx"

internal fun plateModelDestination(
    filesDirectory: File,
    fileName: String,
): File = File(filesDirectory, "models/$fileName")

/**
 * Activity-scoped owner for plate detector/OCR sessions used by the sample validation flow.
 *
 * The manager owns native sessions independently from the validation composable. Removing the
 * camera screen therefore releases only that screen's recognition handle; preparation can finish
 * while the screen is no longer composed.
 */
internal class PlateRecognitionModelsViewModel : ViewModel() {
    val modelManager: PlateRecognitionModelManager = PlateRecognitionModelManager()
    val preparationState: StateFlow<PlateRecognitionPreparationState> = modelManager.state

    private val _requestError = MutableStateFlow<String?>(null)
    val requestError: StateFlow<String?> = _requestError.asStateFlow()

    private var lastRequestedSpec: PlateRecognitionModelSpec? = null

    /** Starts preparation early when both previously selected private model files are available. */
    fun prepareStoredModels(filesDirectory: File): Boolean {
        val detectorModelFile = plateModelDestination(filesDirectory, PLATE_DETECTOR_MODEL_NAME)
        val ocrModelFile = plateModelDestination(filesDirectory, FAST_PLATE_OCR_MODEL_NAME)
        if (!detectorModelFile.isFile || detectorModelFile.length() <= 0L) return false
        if (!ocrModelFile.isFile || ocrModelFile.length() <= 0L) return false

        prepareModels(
            detectorModelFile = detectorModelFile,
            ocrModelFile = ocrModelFile,
        )
        return true
    }

    /** Starts or reuses preparation without blocking the caller. */
    fun prepareModels(
        detectorModelFile: File,
        ocrModelFile: File,
    ) {
        val spec = PlateRecognitionModelSpec(
            detectorModelFile = detectorModelFile,
            ocrModelFile = ocrModelFile,
        )
        lastRequestedSpec = spec
        _requestError.value = null
        runCatching {
            modelManager.prepareModels(spec)
        }.onFailure { error ->
            _requestError.value = error.message ?: error::class.simpleName
        }
    }

    /** Retries the latest preparation request after a failed preparation. */
    fun retryPreparation() {
        val spec = lastRequestedSpec ?: return
        prepareModels(
            detectorModelFile = spec.detectorModelFile,
            ocrModelFile = spec.ocrModelFile,
        )
    }

    override fun onCleared() {
        modelManager.close()
        super.onCleared()
    }
}
