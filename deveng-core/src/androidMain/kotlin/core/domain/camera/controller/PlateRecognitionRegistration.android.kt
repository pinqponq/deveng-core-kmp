package core.domain.camera.controller

import android.util.Size
import core.domain.camera.analysis.plate.PlateRecognitionAnalyzer
import core.domain.camera.analysis.plate.PlateRecognitionConfig
import core.domain.camera.analysis.plate.PlateRecognitionDebugSnapshot
import core.domain.camera.analysis.plate.PlateRecognitionResult
import core.domain.camera.analysis.plate.manager.ModelSessionReleasePolicy
import core.domain.camera.analysis.plate.manager.PlateRecognitionModelManager
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CompletableDeferred

/**
 * A consumer-owned registration for a plate recognition service.
 *
 * Closing this handle unregisters the plate analyzer and releases the registration's resources.
 * For manager-backed registrations this releases only the temporary model session; the
 * manager-owned detector and OCR sessions remain available. It never closes the camera preview or
 * performs application navigation.
 */
interface PlateRecognitionHandle : AutoCloseable {
    override fun close()

    /** Waits until the analyzer is unregistered and its model session is closed. */
    suspend fun closeAndAwait()
}

/**
 * Registers a complete plate-recognition service without exposing the generic analyzer contract.
 *
 * [onPlateRecognized] receives only validated model output. The callback runs on the controller's
 * analysis executor; callers must dispatch to their UI state when necessary. The caller owns all
 * product decisions after a result, including navigation, persistence, API calls, and camera UI.
 */
fun CameraController.registerPlateRecognition(
    modelManager: PlateRecognitionModelManager,
    config: PlateRecognitionConfig = PlateRecognitionConfig(),
    onPlateRecognized: (PlateRecognitionResult) -> Unit,
    onError: (Throwable) -> Unit = {},
    debugLogTag: String? = null,
    analyzeEveryNthFrame: Int = 1,
    minAnalysisIntervalMs: Long = 0,
    targetResolution: Size? = null,
    onDebugSnapshot: ((PlateRecognitionDebugSnapshot) -> Unit)? = null,
): PlateRecognitionHandle {
    val analyzer = createManagedPlateRecognitionAnalyzer(
        modelManager = modelManager,
        config = config,
        onDebugSnapshot = onDebugSnapshot,
    )
    return registerPlateRecognitionAnalyzer(
        analyzer = analyzer,
        config = config,
        onPlateRecognized = onPlateRecognized,
        onError = onError,
        debugLogTag = debugLogTag,
        analyzeEveryNthFrame = analyzeEveryNthFrame,
        minAnalysisIntervalMs = minAnalysisIntervalMs,
        targetResolution = targetResolution,
        releaseAfterDelivery = false,
    )
}

/**
 * Registers a file-backed plate-recognition service.
 *
 * This overload keeps the original model-file ownership and release-policy behavior.
 */
fun CameraController.registerPlateRecognition(
    detectorModelFile: File,
    ocrModelFile: File,
    config: PlateRecognitionConfig = PlateRecognitionConfig(),
    onPlateRecognized: (PlateRecognitionResult) -> Unit,
    onError: (Throwable) -> Unit = {},
    debugLogTag: String? = null,
    analyzeEveryNthFrame: Int = 1,
    minAnalysisIntervalMs: Long = 0,
    targetResolution: Size? = null,
): PlateRecognitionHandle {
    val analyzer = PlateRecognitionAnalyzer(
        detectorModelFile = detectorModelFile,
        ocrModelFile = ocrModelFile,
        config = config,
    )
    return registerPlateRecognitionAnalyzer(
        analyzer = analyzer,
        config = config,
        onPlateRecognized = onPlateRecognized,
        onError = onError,
        debugLogTag = debugLogTag,
        analyzeEveryNthFrame = analyzeEveryNthFrame,
        minAnalysisIntervalMs = minAnalysisIntervalMs,
        targetResolution = targetResolution,
        releaseAfterDelivery =
            config.modelSessionReleasePolicy == ModelSessionReleasePolicy.AFTER_SUCCESS,
    )
}

private fun CameraController.registerPlateRecognitionAnalyzer(
    analyzer: PlateRecognitionAnalyzer,
    config: PlateRecognitionConfig,
    onPlateRecognized: (PlateRecognitionResult) -> Unit,
    onError: (Throwable) -> Unit,
    debugLogTag: String?,
    analyzeEveryNthFrame: Int,
    minAnalysisIntervalMs: Long,
    targetResolution: Size?,
    releaseAfterDelivery: Boolean,
): PlateRecognitionHandle {
    val handle = ManagedPlateRecognitionHandle {
        unregisterFrameAnalyzer(analyzer)
    }
    try {
        registerFrameAnalyzer(
            analyzer = analyzer,
            onResult = { result ->
                handle.deliver(
                    result = result,
                    onPlateRecognized = onPlateRecognized,
                    releaseAfterDelivery = releaseAfterDelivery,
                )
            },
            onError = onError,
            debugLogTag = debugLogTag,
            analyzeEveryNthFrame = analyzeEveryNthFrame,
            minAnalysisIntervalMs = minAnalysisIntervalMs,
            allowConcurrentAfterFrameRelease = false,
            outputImageFormat = analyzer.requiredOutputImageFormat,
            targetResolution = targetResolution,
            onClosed = handle::markClosed,
        )
    } catch (error: Throwable) {
        analyzer.close()
        handle.markClosed()
        throw error
    }
    return handle
}

/** Creates a manager-backed analyzer after acquiring one temporary model session. */
internal fun createManagedPlateRecognitionAnalyzer(
    modelManager: PlateRecognitionModelManager,
    config: PlateRecognitionConfig = PlateRecognitionConfig(),
    onDebugSnapshot: ((PlateRecognitionDebugSnapshot) -> Unit)? = null,
): PlateRecognitionAnalyzer {
    require(config.modelSessionReleasePolicy == ModelSessionReleasePolicy.MANUAL) {
        "Manager-backed plate recognition requires ModelSessionReleasePolicy.MANUAL."
    }
    val session = modelManager.acquireResources()
    return try {
        PlateRecognitionAnalyzer(
            modelSession = session,
            config = config,
            onDebugSnapshot = onDebugSnapshot,
        )
    } catch (error: Throwable) {
        session.close()
        throw error
    }
}

internal class ManagedPlateRecognitionHandle(
    private val unregister: () -> Unit,
) : PlateRecognitionHandle {
    private val closed = AtomicBoolean(false)
    private val closedCompletion = CompletableDeferred<Unit>()

    override fun close() {
        if (closed.compareAndSet(false, true)) {
            unregister()
        }
    }

    override suspend fun closeAndAwait() {
        close()
        closedCompletion.await()
    }

    fun markClosed() {
        closedCompletion.complete(Unit)
    }

    fun deliver(
        result: PlateRecognitionResult,
        onPlateRecognized: (PlateRecognitionResult) -> Unit,
        releaseAfterDelivery: Boolean,
    ) {
        try {
            onPlateRecognized(result)
        } finally {
            if (releaseAfterDelivery) close()
        }
    }
}
