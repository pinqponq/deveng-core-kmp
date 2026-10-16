package core.domain.camera.analysis.plate.manager

import kotlinx.coroutines.CompletableDeferred

internal data class PlateRecognitionModelPreparation(
    val spec: PlateRecognitionModelSpec,
    val completion: CompletableDeferred<Unit>
)
