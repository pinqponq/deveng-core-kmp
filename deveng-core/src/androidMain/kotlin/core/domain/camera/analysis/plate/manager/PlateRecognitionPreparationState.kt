package core.domain.camera.analysis.plate.manager

/** Public lifecycle state of [PlateRecognitionModelManager]. */
sealed interface PlateRecognitionPreparationState {
    data object Idle : PlateRecognitionPreparationState
    data object Loading : PlateRecognitionPreparationState
    data object Warming : PlateRecognitionPreparationState
    data object Ready : PlateRecognitionPreparationState

    data class Failed(
        val error: Throwable
    ) : PlateRecognitionPreparationState

    data object Closed : PlateRecognitionPreparationState
}