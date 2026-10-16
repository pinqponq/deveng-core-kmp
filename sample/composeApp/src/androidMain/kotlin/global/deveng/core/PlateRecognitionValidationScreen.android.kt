package global.deveng.core

import android.content.Context
import android.graphics.Paint
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.Size as AndroidSize
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.Image
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import core.domain.camera.analysis.plate.PlateRecognitionConfig
import core.domain.camera.analysis.plate.PlateRecognitionDebugDetection
import core.domain.camera.analysis.plate.PlateRecognitionDebugOcr
import core.domain.camera.analysis.plate.PlateRecognitionDebugOcrStatus
import core.domain.camera.analysis.plate.PlateRecognitionDebugSnapshot
import core.domain.camera.analysis.plate.PlateRecognitionResult
import core.domain.camera.analysis.plate.manager.ModelSessionReleasePolicy
import core.domain.camera.analysis.plate.manager.PlateRecognitionModelManager
import core.domain.camera.analysis.plate.manager.PlateRecognitionPreparationState
import core.domain.camera.compose.CameraPreviewView
import core.domain.camera.compose.rememberCameraKState
import core.domain.camera.controller.CameraController
import core.domain.camera.controller.PlateRecognitionHandle
import core.domain.camera.controller.registerPlateRecognition
import core.domain.camera.permissions.providePermissions
import core.domain.camera.state.CameraConfiguration
import core.domain.camera.state.CameraKState
import java.io.File
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min

private const val PHASE0_LOG_TAG = "PlateRuntimePhase0"
private const val PHASE0_MAX_FRAME_SAMPLES = 30
private val phase0RunSequence = AtomicLong(0)

@Composable
internal fun PlateRecognitionModelPreparationScreen(
    modelsViewModel: PlateRecognitionModelsViewModel,
    onBack: () -> Unit,
    onOpenValidation: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val preparationState by modelsViewModel.preparationState.collectAsState()
    val preparationRequestError by modelsViewModel.requestError.collectAsState()
    val storedDetectorModel = remember {
        plateModelDestination(context.filesDir, PLATE_DETECTOR_MODEL_NAME)
            .takeIf { it.isFile && it.length() > 0L }
            ?.let { StoredPlateModel(it, it.lastModified()) }
    }
    val storedOcrModel = remember {
        plateModelDestination(context.filesDir, FAST_PLATE_OCR_MODEL_NAME)
            .takeIf { it.isFile && it.length() > 0L }
            ?.let { StoredPlateModel(it, it.lastModified()) }
    }
    var detectorModel by remember { mutableStateOf(storedDetectorModel) }
    var ocrModel by remember { mutableStateOf(storedOcrModel) }
    var copyInProgress by remember { mutableStateOf<PlateModelKind?>(null) }
    var copyError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(detectorModel?.revision, ocrModel?.revision) {
        val detector = detectorModel
        val ocr = ocrModel
        if (detector != null && ocr != null) {
            modelsViewModel.prepareModels(
                detectorModelFile = detector.file,
                ocrModelFile = ocr.file,
            )
        }
    }

    val detectorPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        copyInProgress = PlateModelKind.DETECTOR
        copyError = null
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    copyPlateModelToPrivateStorage(
                        context = context,
                        source = uri,
                        destination = plateModelDestination(context.filesDir, PLATE_DETECTOR_MODEL_NAME),
                    )
                }
            }.onSuccess { copiedModel ->
                detectorModel = StoredPlateModel(copiedModel, System.nanoTime())
            }.onFailure { error ->
                copyError = error.message ?: "Detector model could not be copied."
            }
            copyInProgress = null
        }
    }
    val ocrPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        copyInProgress = PlateModelKind.OCR
        copyError = null
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    copyPlateModelToPrivateStorage(
                        context = context,
                        source = uri,
                        destination = plateModelDestination(context.filesDir, FAST_PLATE_OCR_MODEL_NAME),
                    )
                }
            }.onSuccess { copiedModel ->
                ocrModel = StoredPlateModel(copiedModel, System.nanoTime())
            }.onFailure { error ->
                copyError = error.message ?: "OCR model could not be copied."
            }
            copyInProgress = null
        }
    }

    BackHandler(onBack = onBack)

    val selectDetectorModel = { detectorPicker.launch(arrayOf("*/*")) }
    val selectOcrModel = { ocrPicker.launch(arrayOf("*/*")) }
    PlateModelPreparationContent(
        detectorModel = detectorModel,
        ocrModel = ocrModel,
        preparationState = preparationState,
        copyInProgress = copyInProgress,
        errorMessage = copyError ?: preparationRequestError,
        onBack = onBack,
        onSelectDetectorModel = selectDetectorModel,
        onSelectOcrModel = selectOcrModel,
        onRetryPreparation = modelsViewModel::retryPreparation,
        onOpenValidation = onOpenValidation,
    )
}

@Composable
internal fun PlateRecognitionValidationScreen(
    modelsViewModel: PlateRecognitionModelsViewModel,
    onBack: () -> Unit,
) {
    val preparationState by modelsViewModel.preparationState.collectAsState()
    val preparationRequestError by modelsViewModel.requestError.collectAsState()
    BackHandler(onBack = onBack)

    if (preparationState !is PlateRecognitionPreparationState.Ready) {
        PlateMessageScreen(
            message = preparationState.validationUnavailableMessage(preparationRequestError),
            onBack = onBack,
            showProgress = preparationState is PlateRecognitionPreparationState.Loading ||
                preparationState is PlateRecognitionPreparationState.Warming,
        )
        return
    }

    PlateCameraPermissionGate(
        onBack = onBack,
        onGranted = {
            PlateCameraContent(
                modelManager = modelsViewModel.modelManager,
                preparationState = preparationState,
                preparationError = preparationRequestError,
                onBack = onBack,
                onRetryPreparation = modelsViewModel::retryPreparation,
            )
        },
    )
}

@Composable
private fun PlateModelPreparationContent(
    detectorModel: StoredPlateModel?,
    ocrModel: StoredPlateModel?,
    preparationState: PlateRecognitionPreparationState,
    copyInProgress: PlateModelKind?,
    errorMessage: String?,
    onBack: () -> Unit,
    onSelectDetectorModel: () -> Unit,
    onSelectOcrModel: () -> Unit,
    onRetryPreparation: () -> Unit,
    onOpenValidation: () -> Unit,
) {
    val isCopying = copyInProgress != null
    val canSelectModels = !isCopying &&
        (preparationState is PlateRecognitionPreparationState.Idle ||
            preparationState is PlateRecognitionPreparationState.Failed)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF080B10))
            .systemBarsPadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "Plate OCR Model Preparation",
            color = Color.White,
            style = MaterialTheme.typography.headlineSmall,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "Select the NanoDet detector and fast-plate-ocr ONNX models. " +
                "They are loaded and warmed before the camera screen opens.",
            color = Color.White.copy(alpha = 0.75f),
        )
        Spacer(Modifier.height(20.dp))
        ModelSelectionRow(
            label = "Detector: ${detectorModel?.file?.name ?: "not selected"}",
            buttonText = if (copyInProgress == PlateModelKind.DETECTOR) "Copying..." else "Select detector",
            enabled = canSelectModels,
            onClick = onSelectDetectorModel,
        )
        Spacer(Modifier.height(12.dp))
        ModelSelectionRow(
            label = "OCR: ${ocrModel?.file?.name ?: "not selected"}",
            buttonText = if (copyInProgress == PlateModelKind.OCR) "Copying..." else "Select OCR",
            enabled = canSelectModels,
            onClick = onSelectOcrModel,
        )
        errorMessage?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, color = Color(0xFFFF8A80))
        }
        when (preparationState) {
            PlateRecognitionPreparationState.Idle -> Text(
                "Select both models to start preparation.",
                color = Color.White.copy(alpha = 0.75f),
            )

            PlateRecognitionPreparationState.Loading,
            PlateRecognitionPreparationState.Warming,
            -> {
                Spacer(Modifier.height(16.dp))
                CircularProgressIndicator()
                Spacer(Modifier.height(8.dp))
                Text(
                    if (preparationState is PlateRecognitionPreparationState.Loading) {
                        "Loading model sessions..."
                    } else {
                        "Warming model sessions..."
                    },
                    color = Color(0xFFB8F2D0),
                )
            }

            is PlateRecognitionPreparationState.Ready -> Text(
                "Models are ready. OCR Validation will use warmed sessions.",
                color = Color(0xFFB8F2D0),
            )

            is PlateRecognitionPreparationState.Failed -> Button(onClick = onRetryPreparation) {
                Text("Retry preparation")
            }

            PlateRecognitionPreparationState.Closed -> Text(
                "Model manager is closed.",
                color = Color(0xFFFF8A80),
            )
        }
        if (isCopying) {
            Spacer(Modifier.height(16.dp))
            CircularProgressIndicator()
        }
        Spacer(Modifier.height(20.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(modifier = Modifier.weight(1f), onClick = onBack) { Text("Back") }
            Button(
                modifier = Modifier.weight(1f),
                enabled = preparationState is PlateRecognitionPreparationState.Ready,
                onClick = onOpenValidation,
            ) { Text("Open OCR Validation") }
        }
    }
}

@Composable
private fun ModelSelectionRow(
    label: String,
    buttonText: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(label, color = Color.White.copy(alpha = 0.8f))
        Spacer(Modifier.height(6.dp))
        Button(onClick = onClick, enabled = enabled) {
            Text(buttonText)
        }
    }
}

@Composable
private fun PlateCameraPermissionGate(
    onBack: () -> Unit,
    onGranted: @Composable () -> Unit,
) {
    val permissions = providePermissions()
    var hasPermission by remember { mutableStateOf(permissions.hasCameraPermission()) }
    var permissionDenied by remember { mutableStateOf(false) }

    if (!hasPermission && !permissionDenied) {
        permissions.RequestCameraPermission(
            onGranted = { hasPermission = true },
            onDenied = { permissionDenied = true },
        )
    }

    when {
        permissionDenied -> PlateMessageScreen(
            message = "Camera permission was denied.",
            onBack = onBack,
        )

        !hasPermission -> PlateMessageScreen(
            message = "Waiting for camera permission...",
            onBack = onBack,
            showProgress = true,
        )

        else -> onGranted()
    }
}

@Composable
private fun PlateCameraContent(
    modelManager: PlateRecognitionModelManager,
    preparationState: PlateRecognitionPreparationState,
    preparationError: String?,
    onBack: () -> Unit,
    onRetryPreparation: () -> Unit,
) {
    val cameraState by rememberCameraKState(config = CameraConfiguration())
    val cameraPhase = when (cameraState) {
        CameraKState.Initializing -> "initializing"
        is CameraKState.Ready -> "ready"
        is CameraKState.Error -> "error"
    }
    var cameraPreparationStartedNanos by remember { mutableStateOf<Long?>(null) }
    LaunchedEffect(cameraPhase) {
        when (cameraPhase) {
            "initializing" -> {
                if (cameraPreparationStartedNanos == null) {
                    cameraPreparationStartedNanos = System.nanoTime()
                    Log.i(
                        PHASE0_LOG_TAG,
                        "event=camera_preparation_start pid=${android.os.Process.myPid()} " +
                            "timestampNanos=${cameraPreparationStartedNanos}",
                    )
                }
            }

            "ready" -> {
                val startedNanos = cameraPreparationStartedNanos
                Log.i(
                    PHASE0_LOG_TAG,
                    "event=camera_preparation_ready " +
                        "durationMs=${startedNanos?.let { (System.nanoTime() - it) / 1_000_000.0 } ?: -1.0} " +
                        "timestampNanos=${System.nanoTime()}",
                )
            }

            "error" -> {
                Log.i(
                    PHASE0_LOG_TAG,
                    "event=camera_preparation_failed timestampNanos=${System.nanoTime()}",
                )
            }
        }
    }

    when (val state = cameraState) {
        CameraKState.Initializing -> PlateMessageScreen(
            message = "Initializing camera...",
            onBack = onBack,
            showProgress = true,
        )

        is CameraKState.Error -> PlateMessageScreen(
            message = state.message,
            onBack = onBack,
        )

        is CameraKState.Ready -> PlateReadyContent(
            modelManager = modelManager,
            preparationState = preparationState,
            preparationError = preparationError,
            controller = state.controller,
            onBack = onBack,
            onRetryPreparation = onRetryPreparation,
        )
    }
}

@Composable
private fun PlateReadyContent(
    modelManager: PlateRecognitionModelManager,
    preparationState: PlateRecognitionPreparationState,
    preparationError: String?,
    controller: CameraController,
    onBack: () -> Unit,
    onRetryPreparation: () -> Unit,
) {
    val mainHandler = remember { Handler(Looper.getMainLooper()) }
    val scope = rememberCoroutineScope()
    var validationMode by remember(modelManager) {
        mutableStateOf(PlateValidationMode.THREE_READS_TWO_MATCHES)
    }
    var activeHandle by remember(controller, modelManager) {
        mutableStateOf<PlateRecognitionHandle?>(null)
    }
    var activeCycleTimer by remember(controller, modelManager) {
        mutableStateOf<AtomicLong?>(null)
    }
    var readingPopup by remember(modelManager) {
        mutableStateOf<PlateReadingPopupData?>(null)
    }
    var retryToken by remember(modelManager) {
        mutableStateOf(0L)
    }
    var latestResult by remember(
        modelManager,
        retryToken,
        validationMode,
    ) {
        mutableStateOf<PlateRecognitionResult?>(null)
    }
    var analyzerStatus by remember(
        modelManager,
        retryToken,
        validationMode,
    ) {
        mutableStateOf("Loading detector and OCR models...")
    }
    var analyzerError by remember(
        modelManager,
        retryToken,
        validationMode,
    ) { mutableStateOf<String?>(null) }
    var latestDebugSnapshot by remember(
        modelManager,
        retryToken,
        validationMode,
    ) { mutableStateOf<PlateRecognitionDebugSnapshot?>(null) }
    val phase6Metrics = remember(modelManager, validationMode) {
        PlateRecognitionPhase6Metrics()
    }
    var phase6Summary by remember(modelManager, validationMode) {
        mutableStateOf(phase6Metrics.summary())
    }

    LaunchedEffect(
        controller,
        modelManager,
        preparationState,
        preparationError,
        retryToken,
        validationMode,
    ) {
        if (preparationError != null) {
            analyzerStatus = "Model preparation request failed"
            analyzerError = preparationError
            return@LaunchedEffect
        }
        when (val state = preparationState) {
            PlateRecognitionPreparationState.Idle -> {
                analyzerStatus = "Waiting for detector and OCR models..."
                return@LaunchedEffect
            }

            PlateRecognitionPreparationState.Loading -> {
                analyzerStatus = "Loading detector and OCR models..."
                return@LaunchedEffect
            }

            PlateRecognitionPreparationState.Warming -> {
                analyzerStatus = "Warming detector and OCR models..."
                return@LaunchedEffect
            }

            is PlateRecognitionPreparationState.Failed -> {
                analyzerStatus = "Model preparation failed"
                analyzerError = state.error.message ?: state.error::class.simpleName
                return@LaunchedEffect
            }

            PlateRecognitionPreparationState.Closed -> {
                analyzerStatus = "Model manager is closed"
                return@LaunchedEffect
            }

            is PlateRecognitionPreparationState.Ready -> Unit
        }

        var active = true
        val phase0RunId = phase0RunSequence.incrementAndGet()
        val frameSequence = AtomicLong(0)
        val firstNonEmptyDetectionLogged = AtomicBoolean(false)
        Log.i(
            PHASE0_LOG_TAG,
            "event=model_manager_ready run=$phase0RunId " +
                "thread=${Thread.currentThread().name}",
        )
        var latestSnapshotForResult: PlateRecognitionDebugSnapshot? = null
        val validAttemptSnapshots = ArrayList<PlateRecognitionDebugSnapshot>()
        val onDebugSnapshot: (PlateRecognitionDebugSnapshot) -> Unit = { snapshot ->
            phase6Metrics.recordSnapshot(snapshot)
            val frameIndex = frameSequence.incrementAndGet()
            if (frameIndex <= PHASE0_MAX_FRAME_SAMPLES) {
                Log.i(
                    PHASE0_LOG_TAG,
                    "event=frame run=$phase0RunId index=$frameIndex " +
                        "detections=${snapshot.detections.size} " +
                        "accepted=${snapshot.detections.count { it.passesQualityGate }} " +
                        "detectorMs=${snapshot.detectorNanos / 1_000_000.0} " +
                        "frameCopyMs=${snapshot.ocr?.frameCopyNanos?.div(1_000_000.0) ?: -1.0} " +
                        "ocrPreprocessMs=${snapshot.ocr?.preprocessNanos?.div(1_000_000.0) ?: -1.0} " +
                        "ocrMs=${snapshot.ocr?.durationNanos?.div(1_000_000.0) ?: -1.0} " +
                        "ocrTotalMs=${snapshot.ocr?.totalNanos?.div(1_000_000.0) ?: -1.0} " +
                        "processMs=${snapshot.processNanos / 1_000_000.0}",
                )
            }
            if (
                snapshot.detections.isNotEmpty() &&
                firstNonEmptyDetectionLogged.compareAndSet(false, true)
            ) {
                Log.i(
                    PHASE0_LOG_TAG,
                    "event=first_non_empty_detection run=$phase0RunId " +
                        "index=$frameIndex detectorMs=${snapshot.detectorNanos / 1_000_000.0} " +
                        "timestampNanos=${System.nanoTime()}",
                )
            }
            val rawOcr = snapshot.ocr
            if (
                rawOcr?.status == PlateRecognitionDebugOcrStatus.COMPLETED &&
                !rawOcr.text.isNullOrBlank() &&
                (rawOcr.confidence ?: 0f) >= 0.90f
            ) {
                latestSnapshotForResult = snapshot
                if (validAttemptSnapshots.size == validationMode.validationWindowSize) {
                    validAttemptSnapshots.removeAt(0)
                }
                validAttemptSnapshots += snapshot
            }
            mainHandler.post {
                if (active) {
                    latestDebugSnapshot = snapshot
                    snapshot.ocr?.let { ocr ->
                        Log.d(
                            "PlateRecognitionDebug",
                            "OCR status=${ocr.status} text=${ocr.text.orEmpty()} " +
                                "confidence=${ocr.confidence ?: -1f} " +
                                "frameCopyMs=${ocr.frameCopyNanos?.div(1_000_000.0) ?: -1.0} " +
                                "preprocessMs=${ocr.preprocessNanos?.div(1_000_000.0) ?: -1.0} " +
                                "durationMs=${ocr.durationNanos?.div(1_000_000.0) ?: -1.0} " +
                                "totalMs=${ocr.totalNanos?.div(1_000_000.0) ?: -1.0} " +
                                "consensus=${ocr.acceptedByConsensus} " +
                                "error=${ocr.error.orEmpty()}",
                        )
                    }
                }
            }
        }
        val cycleStartedAtNanos = AtomicLong(System.nanoTime())
        activeCycleTimer = cycleStartedAtNanos

        var registered = false
        var handle: PlateRecognitionHandle? = null
        try {
            val registrationStartedNanos = System.nanoTime()
            Log.i(
                PHASE0_LOG_TAG,
                "event=analyzer_registration_start run=$phase0RunId " +
                    "timestampNanos=$registrationStartedNanos",
            )
            handle = controller.registerPlateRecognition(
                modelManager = modelManager,
                config = PlateRecognitionConfig(
                    lockAfterSuccessfulRead = true,
                    modelSessionReleasePolicy = ModelSessionReleasePolicy.MANUAL,
                    requiredMatchingReads = validationMode.requiredMatchingReads,
                    validationWindowSize = validationMode.validationWindowSize,
                ),
                onPlateRecognized = { result ->
                    val callbackNanos = System.nanoTime()
                    phase6Metrics.recordResult(latestSnapshotForResult, callbackNanos)
                    result.fullFrame?.bitmap
                    val popup = PlateReadingPopupData(
                        result = result,
                        snapshot = latestSnapshotForResult,
                        attempts = validAttemptSnapshots.toList(),
                        totalElapsedNanos = callbackNanos - cycleStartedAtNanos.get(),
                        validationMode = validationMode,
                    )
                    mainHandler.post {
                        if (active) {
                            latestResult = result
                            readingPopup = popup
                            phase6Summary = phase6Metrics.summary()
                            analyzerStatus = "Paused: validated plate result"
                            analyzerError = null
                        }
                    }
                },
                onError = { error ->
                    mainHandler.post {
                        if (active) {
                            analyzerError = error.message ?: error::class.simpleName
                            analyzerStatus = "Analysis error"
                        }
                    }
                },
                debugLogTag = "PlateRecognitionDebug",
                analyzeEveryNthFrame = 1,
                minAnalysisIntervalMs = 0,
                targetResolution = AndroidSize(640, 360),
                onDebugSnapshot = onDebugSnapshot,
            )
            registered = true
            activeHandle = handle
            Log.i(
                PHASE0_LOG_TAG,
                "event=analyzer_registration_ready run=$phase0RunId " +
                    "durationMs=${(System.nanoTime() - registrationStartedNanos) / 1_000_000.0} " +
                    "timestampNanos=${System.nanoTime()}",
            )
            analyzerStatus = "Continuous mode: ${validationMode.description} (confidence >= 90%)..."
            awaitCancellation()
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            analyzerError = error.message ?: "Plate analyzer could not be registered."
            analyzerStatus = "Analyzer registration failed"
        } finally {
            active = false
            Log.i(
                PHASE0_LOG_TAG,
                "event=analyzer_scope_exit run=$phase0RunId registered=$registered " +
                    "timestampNanos=${System.nanoTime()}",
            )
            if (activeHandle === handle) activeHandle = null
            if (activeCycleTimer === cycleStartedAtNanos) activeCycleTimer = null
            handle?.close()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF080B10))
            .systemBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(
                onClick = {
                    val handle = activeHandle
                    activeHandle = null
                    scope.launch {
                        handle?.closeAndAwait()
                        onBack()
                    }
                },
            ) { Text("Back") }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Plate OCR Validation",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    "Detector: $PLATE_DETECTOR_MODEL_NAME | OCR: $FAST_PLATE_OCR_MODEL_NAME",
                    color = Color.White.copy(alpha = 0.65f),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        Text(
            text = analyzerStatus,
            color = Color(0xFFB8F2D0),
            style = MaterialTheme.typography.bodyMedium,
        )
        analyzerError?.let { error ->
            Text(error, color = Color(0xFFFF8A80), style = MaterialTheme.typography.bodySmall)
        }
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                val handle = activeHandle
                activeHandle = null
                scope.launch {
                    handle?.closeAndAwait()
                    readingPopup = null
                    validationMode = validationMode.next()
                }
            },
        ) {
            Text("Mode: ${validationMode.shortLabel}")
        }

        latestDebugSnapshot?.let { snapshot ->
            val acceptedCount = snapshot.detections.count { it.passesQualityGate }
            Text(
                text = "Detector bbox: ${snapshot.detections.size} | " +
                    "quality gate: $acceptedCount | " +
                    "frame: ${snapshot.frameWidth}x${snapshot.frameHeight} r=${snapshot.rotationDegrees}",
                color = if (acceptedCount > 0) Color(0xFF80CBC4) else Color(0xFFFFCC80),
                style = MaterialTheme.typography.bodySmall,
            )
            snapshot.ocr?.let { ocr ->
                Text(
                    text = buildOcrDebugText(ocr),
                    color = when (ocr.status) {
                        PlateRecognitionDebugOcrStatus.COMPLETED -> Color(0xFFB3E5FC)
                        PlateRecognitionDebugOcrStatus.FAILED -> Color(0xFFFF8A80)
                        PlateRecognitionDebugOcrStatus.SKIPPED_IN_FLIGHT -> Color(0xFFFFCC80)
                    },
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        latestResult?.let { result ->
            Text(
                text = "Plate: ${result.plate}",
                color = Color.White,
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = "OCR confidence: ${formatPlateConfidence(result.confidence)}",
                color = Color(0xFFB3E5FC),
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                val handle = activeHandle
                activeHandle = null
                scope.launch {
                    handle?.closeAndAwait()
                    onRetryPreparation()
                    retryToken = System.nanoTime()
                }
            },
        ) { Text("Restart validation cycle") }

        Text(
            "Continuous test mode — ${validationMode.description}; sonuç popup'ı açıkken analiz durur.",
            color = Color.White,
            style = MaterialTheme.typography.labelLarge,
        )
        PlateRecognitionPhase6MetricsPanel(
            summary = phase6Summary,
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color.Black),
        ) {
            CameraPreviewView(controller = controller, modifier = Modifier.fillMaxSize())
            PlateRecognitionDetectionOverlay(snapshot = latestDebugSnapshot)
        }
    }

    readingPopup?.let { popup ->
        val dismissAndResume = {
            val handle = activeHandle
            activeHandle = null
            scope.launch {
                handle?.closeAndAwait()
                readingPopup = null
                latestResult = null
                latestDebugSnapshot = null
                analyzerError = null
                analyzerStatus = "Continuous mode: ${validationMode.description} (confidence >= 90%)..."
                activeCycleTimer?.set(System.nanoTime())
                retryToken = System.nanoTime()
            }
            Unit
        }
        PlateReadingResultDialog(
            data = popup,
            onDismiss = dismissAndResume,
        )
    }
}

@Composable
private fun PlateRecognitionDetectionOverlay(
    snapshot: PlateRecognitionDebugSnapshot?,
) {
    val currentSnapshot = snapshot ?: return
    if (currentSnapshot.detections.isEmpty()) return

    Canvas(modifier = Modifier.fillMaxSize()) {
        val sourceWidth = currentSnapshot.frameWidth
        val sourceHeight = currentSnapshot.frameHeight
        if (size.width <= 0f || size.height <= 0f || sourceWidth <= 0 || sourceHeight <= 0) {
            return@Canvas
        }

        val displayWidth = if (
            currentSnapshot.rotationDegrees == 90 || currentSnapshot.rotationDegrees == 270
        ) {
            sourceHeight.toFloat()
        } else {
            sourceWidth.toFloat()
        }
        val displayHeight = if (
            currentSnapshot.rotationDegrees == 90 || currentSnapshot.rotationDegrees == 270
        ) {
            sourceWidth.toFloat()
        } else {
            sourceHeight.toFloat()
        }
        val scale = min(size.width / displayWidth, size.height / displayHeight)
        val contentWidth = displayWidth * scale
        val contentHeight = displayHeight * scale
        // CameraPreviewView uses PreviewView.ScaleType.FIT_END.
        val offsetX = size.width - contentWidth
        val offsetY = size.height - contentHeight
        val strokeWidth = 2.dp.toPx()
        val labelPaint = Paint().apply {
            textSize = 13.dp.toPx()
            isAntiAlias = true
            style = Paint.Style.FILL
            setShadowLayer(3f, 0f, 0f, android.graphics.Color.BLACK)
        }

        currentSnapshot.detections.forEach { detection ->
            val displayBox = detection.toDisplayBox(
                sourceWidth = sourceWidth,
                sourceHeight = sourceHeight,
                rotationDegrees = currentSnapshot.rotationDegrees,
            )
            val topLeft = Offset(
                x = offsetX + displayBox.left * scale,
                y = offsetY + displayBox.top * scale,
            )
            val boxSize = Size(
                width = max(0f, displayBox.right - displayBox.left) * scale,
                height = max(0f, displayBox.bottom - displayBox.top) * scale,
            )
            val color = if (detection.passesQualityGate) {
                Color(0xFF00E676)
            } else {
                Color(0xFFFF9800)
            }
            labelPaint.color = if (detection.passesQualityGate) {
                android.graphics.Color.rgb(0, 230, 118)
            } else {
                android.graphics.Color.rgb(255, 152, 0)
            }
            drawRect(
                color = color,
                topLeft = topLeft,
                size = boxSize,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth),
            )
            drawIntoCanvas { canvas ->
                canvas.nativeCanvas.drawText(
                    "${(detection.confidence.coerceIn(0f, 1f) * 100).toInt()}%",
                    topLeft.x,
                    (topLeft.y - 4.dp.toPx()).coerceAtLeast(labelPaint.textSize),
                    labelPaint,
                )
            }
        }
    }
}

private data class PlatePreviewDetectionBox(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
)

private fun PlateRecognitionDebugDetection.toDisplayBox(
    sourceWidth: Int,
    sourceHeight: Int,
    rotationDegrees: Int,
): PlatePreviewDetectionBox {
    val sourceBox = PlatePreviewDetectionBox(
        left = min(left, right).coerceIn(0f, (sourceWidth - 1).toFloat()),
        top = min(top, bottom).coerceIn(0f, (sourceHeight - 1).toFloat()),
        right = max(left, right).coerceIn(0f, (sourceWidth - 1).toFloat()),
        bottom = max(top, bottom).coerceIn(0f, (sourceHeight - 1).toFloat()),
    )
    val displayWidth = if (rotationDegrees == 90 || rotationDegrees == 270) {
        sourceHeight
    } else {
        sourceWidth
    }
    val displayHeight = if (rotationDegrees == 90 || rotationDegrees == 270) {
        sourceWidth
    } else {
        sourceHeight
    }
    val rotatedBox = when (rotationDegrees) {
        0 -> sourceBox
        90 -> PlatePreviewDetectionBox(
            left = (sourceHeight - 1).toFloat() - sourceBox.bottom,
            top = sourceBox.left,
            right = (sourceHeight - 1).toFloat() - sourceBox.top,
            bottom = sourceBox.right,
        )
        180 -> PlatePreviewDetectionBox(
            left = (sourceWidth - 1).toFloat() - sourceBox.right,
            top = (sourceHeight - 1).toFloat() - sourceBox.bottom,
            right = (sourceWidth - 1).toFloat() - sourceBox.left,
            bottom = (sourceHeight - 1).toFloat() - sourceBox.top,
        )
        270 -> PlatePreviewDetectionBox(
            left = sourceBox.top,
            top = (sourceWidth - 1).toFloat() - sourceBox.right,
            right = sourceBox.bottom,
            bottom = (sourceWidth - 1).toFloat() - sourceBox.left,
        )
        else -> error("Unsupported frame rotation: $rotationDegrees")
    }
    return PlatePreviewDetectionBox(
        left = min(rotatedBox.left, rotatedBox.right).coerceIn(0f, (displayWidth - 1).toFloat()),
        top = min(rotatedBox.top, rotatedBox.bottom).coerceIn(0f, (displayHeight - 1).toFloat()),
        right = max(rotatedBox.left, rotatedBox.right).coerceIn(0f, (displayWidth - 1).toFloat()),
        bottom = max(rotatedBox.top, rotatedBox.bottom).coerceIn(0f, (displayHeight - 1).toFloat()),
    )
}

@Composable
private fun PlateReadingResultDialog(
    data: PlateReadingPopupData,
    onDismiss: () -> Unit,
) {
    val snapshot = data.snapshot
    val selectedDetection = snapshot?.detections?.firstOrNull { it.passesQualityGate }
    val rawOcr = snapshot?.ocr
    val accumulatedDetectionNanos = data.attempts.sumOf { it.detectorNanos }
    val accumulatedOcrNanos = data.attempts.sumOf { it.ocr?.durationNanos ?: 0L }
    val accumulatedFrameCopyNanos = data.attempts.sumOf { it.ocr?.frameCopyNanos ?: 0L }
    val accumulatedOcrPreprocessNanos = data.attempts.sumOf { it.ocr?.preprocessNanos ?: 0L }
    val accumulatedProcessNanos = data.attempts.sumOf { it.processNanos }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Plate recognized") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text("Plate: ${data.result.plate}", style = MaterialTheme.typography.titleLarge)
                val bestFrame = data.result.fullFrame
                bestFrame?.let { frame ->
                    Text("Best matching frame", style = MaterialTheme.typography.titleMedium)
                    Image(
                        bitmap = frame.bitmap.asImageBitmap(),
                        contentDescription = "Full camera frame supporting the recognized plate",
                        modifier = Modifier.fillMaxWidth().height(220.dp),
                        contentScale = ContentScale.Fit,
                    )
                    MetricText("Frame timestamp", bestFrame.timestampNanos.toString())
                    MetricText("Bitmap render time", bestFrame.bitmapRenderNanos.formatDurationMs())
                }
                MetricText("Mode", data.validationMode.description)
                MetricText("Final confidence", formatPlateConfidence(data.result.confidence))
                MetricText("Mode total elapsed time", data.totalElapsedNanos.formatDurationSeconds())
                MetricText("Validated read count", data.attempts.size.toString())
                MetricText("Accumulated detection time", accumulatedDetectionNanos.formatDurationMs())
                MetricText("Accumulated frame copy time", accumulatedFrameCopyNanos.formatDurationMs())
                MetricText("Accumulated OCR preprocess time", accumulatedOcrPreprocessNanos.formatDurationMs())
                MetricText("Accumulated OCR time", accumulatedOcrNanos.formatDurationMs())
                MetricText("Accumulated process time", accumulatedProcessNanos.formatDurationMs())
                MetricText("Raw OCR", rawOcr?.text.orEmpty().ifEmpty { "-" })
                MetricText(
                    "Raw OCR confidence",
                    rawOcr?.confidence?.let(::formatPlateConfidence) ?: "-",
                )
                MetricText("Detection count", snapshot?.detections?.size?.toString() ?: "-")
                MetricText(
                    "Selected detection confidence",
                    selectedDetection?.confidence?.let(::formatPlateConfidence) ?: "-",
                )
                MetricText("Selected bbox", selectedDetection?.formatBoundingBox() ?: "-")
                MetricText(
                    "Frame",
                    snapshot?.let { "${it.frameWidth}x${it.frameHeight}, rotation=${it.rotationDegrees}" }
                        ?: "-",
                )
                MetricText("Detection time", snapshot?.detectorNanos.formatDurationMs())
                MetricText("Frame copy time", rawOcr?.frameCopyNanos.formatDurationMs())
                MetricText("OCR preprocess time", rawOcr?.preprocessNanos.formatDurationMs())
                MetricText("OCR inference time", rawOcr?.durationNanos.formatDurationMs())
                MetricText("OCR total time", rawOcr?.totalNanos.formatDurationMs())
                MetricText("Total process time", snapshot?.processNanos.formatDurationMs())
                data.attempts.forEachIndexed { index, attempt ->
                    val attemptOcr = attempt.ocr
                    Text(
                        "Read ${index + 1}: '${attemptOcr?.text.orEmpty()}' | " +
                            "confidence=${attemptOcr?.confidence?.let(::formatPlateConfidence) ?: "-"} | " +
                            "detection=${attempt.detectorNanos.formatDurationMs()} | " +
                            "copy=${attemptOcr?.frameCopyNanos.formatDurationMs()} | " +
                            "preprocess=${attemptOcr?.preprocessNanos.formatDurationMs()} | " +
                            "inference=${attemptOcr?.durationNanos.formatDurationMs()} | " +
                            "ocrTotal=${attemptOcr?.totalNanos.formatDurationMs()} | " +
                            "total=${attempt.processNanos.formatDurationMs()}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text("Continue") }
        },
    )
}

@Composable
private fun MetricText(label: String, value: String) {
    Text("$label: $value", style = MaterialTheme.typography.bodyMedium)
}

@Composable
private fun PlateRecognitionPhase6MetricsPanel(
    summary: PlateRecognitionPhase6Summary,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF111820))
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            "Phase 6 device metrics",
            color = Color(0xFFB3E5FC),
            style = MaterialTheme.typography.labelLarge,
        )
        Text(
            "Frames=${summary.analyzedFrameCount}, results=${summary.resultCount}, " +
                "OCR failures=${summary.ocrFailureCount}",
            color = Color.White,
            style = MaterialTheme.typography.bodySmall,
        )
        Text(
            "Detector ${summary.detector.formatPhase6()} | OCR ${summary.ocr.formatPhase6()}",
            color = Color.White,
            style = MaterialTheme.typography.bodySmall,
        )
        Text(
            "Total ${summary.total.formatPhase6()} | frame-to-callback ${summary.callbackLatency.formatPhase6()}",
            color = Color.White,
            style = MaterialTheme.typography.bodySmall,
        )
        Text(
            "Analysis frame gap ${summary.analysisFrameGap.formatPhase6()} " +
                "(CameraX analysis stream; not Preview FPS)",
            color = Color(0xFFFFCC80),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

private data class PlateReadingPopupData(
    val result: PlateRecognitionResult,
    val snapshot: PlateRecognitionDebugSnapshot?,
    val attempts: List<PlateRecognitionDebugSnapshot>,
    val totalElapsedNanos: Long,
    val validationMode: PlateValidationMode,
)

private enum class PlateValidationMode(
    val shortLabel: String,
    val description: String,
    val requiredMatchingReads: Int,
    val validationWindowSize: Int,
) {
    THREE_READS_TWO_MATCHES(
        shortLabel = "2/3",
        description = "3 okumada 2 eşleşme",
        requiredMatchingReads = 2,
        validationWindowSize = 3,
    ),
    TWO_READS_TWO_MATCHES(
        shortLabel = "2/2",
        description = "2 okumada 2 eşleşme",
        requiredMatchingReads = 2,
        validationWindowSize = 2,
    ),
    SINGLE_READ(
        shortLabel = "1/1",
        description = "tek okuma",
        requiredMatchingReads = 1,
        validationWindowSize = 1,
    ),
    ;

    fun next(): PlateValidationMode {
        val values = entries
        return values[(ordinal + 1) % values.size]
    }
}

private fun PlateRecognitionDebugDetection.formatBoundingBox(): String = String.format(
    Locale.US,
    "[%.1f, %.1f, %.1f, %.1f]",
    left,
    top,
    right,
    bottom,
)

private fun Long?.formatDurationMs(): String = this
    ?.div(1_000_000.0)
    ?.let { String.format(Locale.US, "%.2f ms", it) }
    ?: "-"

private fun Long.formatDurationSeconds(): String =
    String.format(Locale.US, "%.3f s", this / 1_000_000_000.0)

private fun PlateRecognitionPreparationState.validationUnavailableMessage(
    requestError: String?,
): String = when {
    requestError != null -> requestError
    this is PlateRecognitionPreparationState.Idle ->
        "Plate OCR models are not prepared. Return to the model preparation screen."
    this is PlateRecognitionPreparationState.Loading ->
        "Model sessions are loading on the preparation screen."
    this is PlateRecognitionPreparationState.Warming ->
        "Model sessions are warming on the preparation screen."
    this is PlateRecognitionPreparationState.Failed ->
        error.message ?: "Model preparation failed. Return to the preparation screen."
    this is PlateRecognitionPreparationState.Closed ->
        "Plate OCR model manager is closed."
    this is PlateRecognitionPreparationState.Ready ->
        error("A ready model manager must not show the unavailable screen.")
    else -> "Plate OCR model preparation state is unavailable."
}

@Composable
private fun PlateMessageScreen(
    message: String,
    onBack: () -> Unit,
    showProgress: Boolean = false,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF080B10))
            .systemBarsPadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (showProgress) {
            CircularProgressIndicator()
            Spacer(Modifier.height(16.dp))
        }
        Text(message, color = Color.White)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onBack) { Text("Back") }
    }
}

private enum class PlateModelKind {
    DETECTOR,
    OCR,
}

private data class StoredPlateModel(
    val file: File,
    val revision: Long,
)

private fun copyPlateModelToPrivateStorage(
    context: Context,
    source: Uri,
    destination: File,
): File {
    val directory = destination.parentFile
        ?: error("Model destination has no parent directory.")
    check(directory.exists() || directory.mkdirs()) {
        "Model directory could not be created: ${directory.absolutePath}"
    }

    val temporary = File(directory, "${destination.name}.part")
    val backup = File(directory, "${destination.name}.bak")
    try {
        context.contentResolver.openInputStream(source).use { input ->
            requireNotNull(input) { "Selected model could not be opened." }
            temporary.outputStream().use { output ->
                input.copyTo(output)
                output.flush()
            }
        }
        require(temporary.length() > 0L) { "Selected model is empty." }

        if (backup.exists() && !backup.delete()) {
            error("Previous model backup could not be removed.")
        }
        if (destination.exists() && !destination.renameTo(backup)) {
            error("Existing private model copy could not be backed up.")
        }
        if (!temporary.renameTo(destination)) {
            if (backup.exists()) backup.renameTo(destination)
            error("Selected model could not be moved into private storage.")
        }
        if (backup.exists()) backup.delete()
        return destination
    } finally {
        if (temporary.exists()) temporary.delete()
    }
}

private fun formatPlateConfidence(confidence: Float): String =
    String.format(Locale.US, "%.1f%%", confidence.coerceIn(0f, 1f) * 100f)

private fun buildOcrDebugText(ocr: PlateRecognitionDebugOcr): String = when (ocr.status) {
    PlateRecognitionDebugOcrStatus.COMPLETED ->
        "Raw OCR: '${ocr.text.orEmpty()}' | confidence=${ocr.confidence?.let(::formatPlateConfidence)} " +
            "| copy=${ocr.frameCopyNanos.formatDurationMs()} " +
            "| preprocess=${ocr.preprocessNanos.formatDurationMs()} " +
            "| inference=${ocr.durationNanos.formatDurationMs()} " +
            "| total=${ocr.totalNanos.formatDurationMs()} " +
            "| consensus=${ocr.acceptedByConsensus}"
    PlateRecognitionDebugOcrStatus.FAILED -> "OCR error: ${ocr.error.orEmpty()}"
    PlateRecognitionDebugOcrStatus.SKIPPED_IN_FLIGHT ->
        "OCR: in-flight, this frame updated tracking only"
}
