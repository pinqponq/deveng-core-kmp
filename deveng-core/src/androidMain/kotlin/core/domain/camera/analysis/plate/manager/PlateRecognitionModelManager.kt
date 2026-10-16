package core.domain.camera.analysis.plate.manager

import core.domain.camera.analysis.plate.PlateRecognitionConfig
import core.domain.camera.analysis.plate.engine.FastPlateOcrEngine
import core.domain.camera.analysis.plate.engine.NanoDetPlateDetectorEngine
import core.domain.camera.analysis.plate.engine.PlateDetectorEngine
import core.domain.camera.analysis.plate.engine.PlateOcrEngine
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Owns application-scoped detector and OCR preparation work.
 *
 * The manager deliberately does not depend on a camera screen or analyzer lifecycle. Calling
 * [prepareModels] schedules one preparation job and returns immediately. A consumer that needs to
 * wait for the result can observe [state] or call [waitUntilReady].
 */
class PlateRecognitionModelManager : AutoCloseable {
    private val lifecycleLock = Any()
    private val isClosed = AtomicBoolean(false)
    private val _state = MutableStateFlow<PlateRecognitionPreparationState>(
        PlateRecognitionPreparationState.Idle
    )
    private val preparationScope = CoroutineScope(
        SupervisorJob() + Dispatchers.Default.limitedParallelism(1) + preparationExceptionHandler()
    )  // modelin yükleneceği ve ana thread'i engellemeyecek izole thread
    private var currentSpec: PlateRecognitionModelSpec? = null
    private var preparedModelResources: PlateRecognitionModelResources? = null  // modellerin ısıtılması
    private var activeSession: PlateRecognitionModelSession? = null  // aktif kamera oturumu
    private var activePreparation: PlateRecognitionModelPreparation? = null  // model yükleme coroutine takibi

    /** Current preparation state. The flow remains [PlateRecognitionPreparationState.Closed] after close. */
    val state: StateFlow<PlateRecognitionPreparationState> = _state.asStateFlow()

    /**
     * Starts preparation without blocking the caller.
     *
     * A repeated request for the active or already-ready spec is a no-op.
     */
    fun prepareModels(spec: PlateRecognitionModelSpec) {
        synchronized(lifecycleLock) {
            check(!isClosed.get()) { "PlateRecognitionModelManager is already closed." }

            if (isModelsReady(spec) || activePreparation != null) {
                return
            }

            currentSpec = spec
            _state.value = PlateRecognitionPreparationState.Loading

            val preparation = PlateRecognitionModelPreparation(
                spec = spec,
                completion = CompletableDeferred()
            )

            activePreparation = preparation
            preparationScope.launch(start = CoroutineStart.DEFAULT) {
                executePreparation(preparation)
            }
        }
    }

    /** Returns true if the models for the given [spec] are prepared and ready for use. */
    fun isModelsReady(spec: PlateRecognitionModelSpec): Boolean {
        return synchronized(lifecycleLock) {
            currentSpec == spec && _state.value is PlateRecognitionPreparationState.Ready
        }
    }

    /**
     * Grants one analyzer temporary access to the prepared model resources.
     *
     * The returned session must be closed when the analyzer stops. Closing it does not close native
     * sessions; those remain owned by this manager until [close].
     */
    internal fun acquireResources(): PlateRecognitionModelSession {
        synchronized(lifecycleLock) {
            val resources = requireReadyResources()
            var newSession: PlateRecognitionModelSession? = null
            newSession = resources.openSession {
                synchronized(lifecycleLock) {
                    if (activeSession === newSession) {
                        activeSession = null
                    }
                }
            }
            activeSession = newSession
            return newSession
        }
    }

    private fun requireReadyResources(): PlateRecognitionModelResources {
        check(!isClosed.get() && _state.value is PlateRecognitionPreparationState.Ready && activePreparation == null) {
            "Plate recognition models are not ready."
        }
        return requireNotNull(preparedModelResources) { "Plate recognition models are not ready." }
    }

    /**
     * Waits until the models are prepared and ready for use.
     *
     * This suspends only the caller's coroutine. If the models are already ready, this returns immediately.
     */
    suspend fun waitUntilReady() {
        val pendingCompletion = synchronized(lifecycleLock) {
            when (val currentState = _state.value) {
                PlateRecognitionPreparationState.Ready -> return
                is PlateRecognitionPreparationState.Failed -> throw currentState.error
                PlateRecognitionPreparationState.Idle -> {
                    throw IllegalStateException("No plate-recognition model preparation was requested.")
                }
                PlateRecognitionPreparationState.Closed -> {
                    throw IllegalStateException("PlateRecognitionModelManager is already closed.")
                }
                PlateRecognitionPreparationState.Loading,
                PlateRecognitionPreparationState.Warming -> activePreparation?.completion
            }
        }
        pendingCompletion?.await()
    }


    /**
     * Cancels pending work and releases all currently owned model sessions. This operation is
     * idempotent. A manager that has been closed cannot be reused.
     */
    override fun close() {
        val preparation: PlateRecognitionModelPreparation?
        val models: PlateRecognitionModelResources?
        val session: PlateRecognitionModelSession?

        synchronized(lifecycleLock) {
            if (!isClosed.compareAndSet(false, true)) return

            preparation = activePreparation
            models = preparedModelResources
            session = activeSession
            activePreparation = null
            preparedModelResources = null
            activeSession = null
            currentSpec = null
            _state.value = PlateRecognitionPreparationState.Closed
        }

        preparation?.completion?.cancel()
        session?.close()
        models?.close()
        preparationScope.cancel()
    }

    private fun executePreparation(preparation: PlateRecognitionModelPreparation) {
        try {
            val loadedModels = loadResources(preparation.spec) {
                updateStateToWarming(preparation)
            }

            synchronized(lifecycleLock) {
                if (isClosed.get()) {
                    loadedModels.close()
                    preparation.completion.cancel()
                    return
                }

                preparedModelResources = loadedModels
                activePreparation = null
                _state.value = PlateRecognitionPreparationState.Ready
            }

            preparation.completion.complete(Unit)
        } catch (error: Throwable) {
            if (error is CancellationException) {
                preparation.completion.cancel(error)
            } else {
                synchronized(lifecycleLock) {
                    if (!isClosed.get()) {
                        activePreparation = null
                        _state.value = PlateRecognitionPreparationState.Failed(error)
                    }
                }
                preparation.completion.completeExceptionally(error)
            }
        }
    }

    private fun updateStateToWarming(preparation: PlateRecognitionModelPreparation) {
        synchronized(lifecycleLock) {
            if (
                !isClosed.get() &&
                activePreparation === preparation &&
                _state.value is PlateRecognitionPreparationState.Loading
            ) {
                _state.value = PlateRecognitionPreparationState.Warming
            }
        }
    }

    private fun preparationExceptionHandler(): CoroutineExceptionHandler {
        return CoroutineExceptionHandler { _, error ->
            // executePreparation converts expected preparation failures to Failed and completes its
            // waiter. This handler is a last line of defense for an unexpected coroutine failure.
            synchronized(lifecycleLock) {
                if (!isClosed.get() && activePreparation != null) {
                    _state.value = PlateRecognitionPreparationState.Failed(error)
                }
            }
        }
    }

    private fun loadResources(
        spec: PlateRecognitionModelSpec,
        onWarming: () -> Unit
    ): PlateRecognitionModelResources {
        val plateDetector = NanoDetPlateDetectorEngine(
            modelFile = spec.detectorModelFile,
            config = spec.config.detectorConfig,
        )
        var ocrEngine: PlateOcrEngine? = null

        return try {
            onWarming()
            plateDetector.warmUp()

            val preparedOcrEngine = FastPlateOcrEngine(
                modelFile = spec.ocrModelFile,
                config = spec.config.ocrConfig

            )
            ocrEngine = preparedOcrEngine
            preparedOcrEngine.warmUp()

            PlateRecognitionModelResources(
                detector = plateDetector,
                ocr = preparedOcrEngine
            )
        } catch (error: Throwable) {
            ocrEngine?.close()
            plateDetector.close()
            throw error
        }
    }
}