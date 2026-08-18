package global.deveng.core

import android.content.Context
import android.graphics.Paint
import android.net.Uri
import android.os.Debug
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.Size as AndroidSize
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import core.domain.camera.analysis.nanodet.NanoDetValidationAnalyzer
import core.domain.camera.analysis.nanodet.NanoDetValidationExecutionProvider
import core.domain.camera.analysis.nanodet.NanoDetValidationResult
import core.domain.camera.analysis.nanodet.NanoDetValidationSourceDetection
import core.domain.camera.analysis.nanodet.NanoDetValidationTimings
import core.domain.camera.analysis.nanodet.NanoDetWorkerPriority
import core.domain.camera.analysis.FrameAnalysisOutputFormat
import core.domain.camera.compose.CameraPreviewView
import core.domain.camera.compose.rememberCameraKState
import core.domain.camera.permissions.providePermissions
import core.domain.camera.state.CameraConfiguration
import core.domain.camera.state.CameraKState
import java.io.File
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min

@Composable
internal fun NanoDetValidationScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val storedModel = remember {
        modelDestination(context).takeIf { it.isFile && it.length() > 0L }
    }
    var selectedModel by remember {
        mutableStateOf(storedModel?.let { SelectedModel(it, it.lastModified()) })
    }
    var modelCopyInProgress by remember { mutableStateOf(false) }
    var modelCopyError by remember { mutableStateOf<String?>(null) }
    val modelPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        modelCopyInProgress = true
        modelCopyError = null
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    copyModelToPrivateStorage(context, uri)
                }
            }.onSuccess { copiedModel ->
                selectedModel = SelectedModel(copiedModel, System.nanoTime())
            }.onFailure { error ->
                modelCopyError = error.message ?: "Model could not be copied."
            }
            modelCopyInProgress = false
        }
    }

    BackHandler(onBack = onBack)

    val selectModel = { modelPicker.launch(arrayOf("*/*")) }
    val model = selectedModel
    if (model == null) {
        ModelSelectionContent(
            copyInProgress = modelCopyInProgress,
            errorMessage = modelCopyError,
            onBack = onBack,
            onSelectModel = selectModel,
        )
        return
    }

    CameraPermissionGate(
        onBack = onBack,
        onGranted = {
            NanoDetCameraContent(
                selectedModel = model,
                modelCopyInProgress = modelCopyInProgress,
                modelCopyError = modelCopyError,
                onBack = onBack,
                onSelectModel = selectModel,
            )
        },
    )
}

@Composable
private fun CameraPermissionGate(
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
        permissionDenied -> MessageScreen(
            message = "Camera permission was denied.",
            onBack = onBack,
        )

        !hasPermission -> MessageScreen(
            message = "Waiting for camera permission...",
            onBack = onBack,
            showProgress = true,
        )

        else -> onGranted()
    }
}

@Composable
private fun NanoDetCameraContent(
    selectedModel: SelectedModel,
    modelCopyInProgress: Boolean,
    modelCopyError: String?,
    onBack: () -> Unit,
    onSelectModel: () -> Unit,
) {
    val cameraState by rememberCameraKState(config = CameraConfiguration())

    when (val state = cameraState) {
        CameraKState.Initializing -> MessageScreen(
            message = "Initializing camera...",
            onBack = onBack,
            showProgress = true,
        )

        is CameraKState.Error -> MessageScreen(
            message = state.message,
            onBack = onBack,
        )

        is CameraKState.Ready -> NanoDetReadyContent(
            selectedModel = selectedModel,
            controller = state.controller,
            modelCopyInProgress = modelCopyInProgress,
            modelCopyError = modelCopyError,
            onBack = onBack,
            onSelectModel = onSelectModel,
        )
    }
}

@Composable
private fun NanoDetReadyContent(
    selectedModel: SelectedModel,
    controller: core.domain.camera.controller.CameraController,
    modelCopyInProgress: Boolean,
    modelCopyError: String?,
    onBack: () -> Unit,
    onSelectModel: () -> Unit,
) {
    val mainHandler = remember { Handler(Looper.getMainLooper()) }
    var executionProvider by remember(selectedModel.revision) {
        mutableStateOf(NanoDetValidationExecutionProvider.XNNPACK)
    }
    var workerPriority by remember(selectedModel.revision) {
        mutableStateOf(NanoDetWorkerPriority.BACKGROUND)
    }
    var latestResult by remember(
        selectedModel.revision,
        executionProvider,
        workerPriority,
    ) {
        mutableStateOf<NanoDetValidationResult?>(null)
    }
    val benchmarkAccumulator = remember(
        selectedModel.revision,
        executionProvider,
        workerPriority,
    ) { NanoDetBenchmarkAccumulator() }
    var benchmarkSummary by remember(
        selectedModel.revision,
        executionProvider,
        workerPriority,
    ) { mutableStateOf<NanoDetBenchmarkSummary?>(null) }
    var analyzerStatus by remember(selectedModel.revision) { mutableStateOf("Loading model...") }
    var analyzerError by remember(selectedModel.revision) { mutableStateOf<String?>(null) }

    LaunchedEffect(
        controller,
        selectedModel.revision,
        executionProvider,
        workerPriority,
    ) {
        val analyzer = runCatching {
            withContext(Dispatchers.Default) {
                NanoDetValidationAnalyzer(
                    modelFile = selectedModel.file,
                    executionProvider = executionProvider,
                    workerPriority = workerPriority,
                )
            }
        }.getOrElse { error ->
            analyzerError = error.message ?: "NanoDet model could not be initialized."
            analyzerStatus = "Model initialization failed"
            return@LaunchedEffect
        }

        var active = true
        var registered = false
        try {
            controller.registerFrameAnalyzer(
                analyzer = analyzer,
                onResult = { result ->
                    mainHandler.post {
                        if (active) {
                            latestResult = result
                            benchmarkAccumulator.add(result)?.let { benchmarkSummary = it }
                            analyzerStatus = benchmarkAccumulator.status
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
                debugLogTag = "NanoDetDebug",
                analyzeEveryNthFrame = 1,
                minAnalysisIntervalMs = 0,
                outputImageFormat = FrameAnalysisOutputFormat.RGBA_8888,
                // Match the 16:9 Preview stream so source-frame overlays use the same geometry.
                // The detector still receives its fixed 416 x 416 model input after preprocessing.
                targetResolution = AndroidSize(640, 360),
            )
            registered = true
            analyzerStatus = "Waiting for camera frame..."
            awaitCancellation()
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            analyzerError = error.message ?: "Analyzer could not be registered."
            analyzerStatus = "Analyzer registration failed"
        } finally {
            active = false
            if (registered) {
                controller.unregisterFrameAnalyzer(analyzer)
            } else {
                analyzer.close()
            }
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
            Button(onClick = onBack) { Text("Back") }
            Column(modifier = Modifier.weight(1f)) {
                Text("NanoDet Validation", color = Color.White, style = MaterialTheme.typography.titleMedium)
                Text(
                    selectedModel.file.name,
                    color = Color.White.copy(alpha = 0.65f),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Button(onClick = onSelectModel, enabled = !modelCopyInProgress) {
                Text(if (modelCopyInProgress) "Copying..." else "Model")
            }
        }

        Text(
            text = buildStatusText(analyzerStatus, latestResult),
            color = Color(0xFFB8F2D0),
            style = MaterialTheme.typography.bodyMedium,
        )
        (modelCopyError ?: analyzerError)?.let { error ->
            Text(error, color = Color(0xFFFF8A80), style = MaterialTheme.typography.bodySmall)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(
                modifier = Modifier.weight(1f),
                onClick = { executionProvider = executionProvider.next() },
            ) {
                Text("EP: ${executionProvider.shortLabel}")
            }
            Button(
                modifier = Modifier.weight(1f),
                onClick = { workerPriority = workerPriority.next() },
            ) {
                Text("Priority: ${workerPriority.name}")
            }
        }
        benchmarkSummary?.let { summary ->
            Text(
                text = summary.displayText,
                color = Color(0xFFB3E5FC),
                style = MaterialTheme.typography.bodySmall,
            )
        }

        Text("Live camera", color = Color.White, style = MaterialTheme.typography.labelLarge)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color.Black),
        ) {
            CameraPreviewView(controller = controller, modifier = Modifier.fillMaxSize())
            DetectionOverlay(result = latestResult)
        }
    }
}

@Composable
private fun DetectionOverlay(result: NanoDetValidationResult?) {
    val validationResult = result ?: return
    if (validationResult.sourceDetections.isEmpty()) return

    Canvas(modifier = Modifier.fillMaxSize()) {
        val sourceWidth = validationResult.frameWidth
        val sourceHeight = validationResult.frameHeight
        if (size.width <= 0f || size.height <= 0f || sourceWidth <= 0 || sourceHeight <= 0) {
            return@Canvas
        }

        val displayWidth = if (validationResult.rotationDegrees == 90 ||
            validationResult.rotationDegrees == 270
        ) {
            sourceHeight.toFloat()
        } else {
            sourceWidth.toFloat()
        }
        val displayHeight = if (validationResult.rotationDegrees == 90 ||
            validationResult.rotationDegrees == 270
        ) {
            sourceWidth.toFloat()
        } else {
            sourceHeight.toFloat()
        }
        val scale = min(size.width / displayWidth, size.height / displayHeight)
        val contentWidth = displayWidth * scale
        val contentHeight = displayHeight * scale
        // CameraPreviewView uses PreviewView.ScaleType.FIT_END, so letterbox space is end-aligned.
        val offsetX = size.width - contentWidth
        val offsetY = size.height - contentHeight
        val strokeWidth = 2.dp.toPx()
        val labelPaint = Paint().apply {
            color = android.graphics.Color.rgb(255, 152, 0)
            textSize = 13.dp.toPx()
            isAntiAlias = true
            style = Paint.Style.FILL
            setShadowLayer(3f, 0f, 0f, android.graphics.Color.BLACK)
        }

        validationResult.sourceDetections.forEach { detection ->
            val displayBox = detection.toDisplayBox(
                sourceWidth = sourceWidth,
                sourceHeight = sourceHeight,
                rotationDegrees = validationResult.rotationDegrees,
            )
            val topLeft = Offset(
                x = offsetX + displayBox.left * scale,
                y = offsetY + displayBox.top * scale,
            )
            val boxSize = Size(
                width = max(0f, displayBox.right - displayBox.left) * scale,
                height = max(0f, displayBox.bottom - displayBox.top) * scale,
            )
            drawRect(
                color = DetectionOverlayColor,
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

private data class PreviewDetectionBox(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
)

private fun NanoDetValidationSourceDetection.toDisplayBox(
    sourceWidth: Int,
    sourceHeight: Int,
    rotationDegrees: Int,
): PreviewDetectionBox {
    val sourceBox = PreviewDetectionBox(
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
        0 -> PreviewDetectionBox(
            left = sourceBox.left,
            top = sourceBox.top,
            right = sourceBox.right,
            bottom = sourceBox.bottom,
        )
        90 -> PreviewDetectionBox(
            left = (sourceHeight - 1).toFloat() - sourceBox.bottom,
            top = sourceBox.left,
            right = (sourceHeight - 1).toFloat() - sourceBox.top,
            bottom = sourceBox.right,
        )
        180 -> PreviewDetectionBox(
            left = (sourceWidth - 1).toFloat() - sourceBox.right,
            top = (sourceHeight - 1).toFloat() - sourceBox.bottom,
            right = (sourceWidth - 1).toFloat() - sourceBox.left,
            bottom = (sourceHeight - 1).toFloat() - sourceBox.top,
        )
        270 -> PreviewDetectionBox(
            left = sourceBox.top,
            top = (sourceWidth - 1).toFloat() - sourceBox.right,
            right = sourceBox.bottom,
            bottom = (sourceWidth - 1).toFloat() - sourceBox.left,
        )
        else -> error("Unsupported frame rotation: $rotationDegrees")
    }
    return PreviewDetectionBox(
        left = min(rotatedBox.left, rotatedBox.right).coerceIn(0f, (displayWidth - 1).toFloat()),
        top = min(rotatedBox.top, rotatedBox.bottom).coerceIn(0f, (displayHeight - 1).toFloat()),
        right = max(rotatedBox.left, rotatedBox.right).coerceIn(0f, (displayWidth - 1).toFloat()),
        bottom = max(rotatedBox.top, rotatedBox.bottom).coerceIn(0f, (displayHeight - 1).toFloat()),
    )
}

private val DetectionOverlayColor = Color(0xFFFF9800)

@Composable
private fun ModelSelectionContent(
    copyInProgress: Boolean,
    errorMessage: String?,
    onBack: () -> Unit,
    onSelectModel: () -> Unit,
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
        Text("NanoDet Validation", color = Color.White, style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(12.dp))
        Text(
            "Select the NanoDet ONNX model. It will be copied into the app's private storage.",
            color = Color.White.copy(alpha = 0.75f),
        )
        errorMessage?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, color = Color(0xFFFF8A80))
        }
        Spacer(Modifier.height(20.dp))
        if (copyInProgress) {
            CircularProgressIndicator()
        } else {
            Button(onClick = onSelectModel) { Text("Select ONNX model") }
        }
        Spacer(Modifier.height(8.dp))
        Button(onClick = onBack) { Text("Back") }
    }
}

@Composable
private fun MessageScreen(
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

private fun buildStatusText(
    analyzerStatus: String,
    result: NanoDetValidationResult?,
): String = if (result == null) {
    analyzerStatus
} else {
    "$analyzerStatus | detections=${result.sourceDetections.size} | " +
        "${formatMilliseconds(result.processingTimeMs)} ms | " +
        "${result.frameWidth}x${result.frameHeight} r=${result.rotationDegrees} " +
        "${result.executionProvider} RGBA native / LUT + Array"
}

private fun NanoDetValidationExecutionProvider.next(): NanoDetValidationExecutionProvider {
    val values = NanoDetValidationExecutionProvider.entries
    return values[(ordinal + 1) % values.size]
}

private val NanoDetValidationExecutionProvider.shortLabel: String
    get() = when (this) {
        NanoDetValidationExecutionProvider.CPU -> "CPU"
        NanoDetValidationExecutionProvider.XNNPACK -> "XNN"
    }

private fun NanoDetWorkerPriority.next(): NanoDetWorkerPriority {
    val values = NanoDetWorkerPriority.entries
    return values[(ordinal + 1) % values.size]
}

private class NanoDetBenchmarkAccumulator {
    private var receivedCount = 0
    private val samples = ArrayList<NanoDetValidationTimings>(BENCHMARK_SAMPLE_COUNT)
    private var runtimeStart: RuntimeSnapshot? = null

    val status: String
        get() = when {
            receivedCount < BENCHMARK_WARM_UP_COUNT ->
                "Warm-up ${receivedCount}/$BENCHMARK_WARM_UP_COUNT"
            samples.size < BENCHMARK_SAMPLE_COUNT ->
                "Measuring ${samples.size}/$BENCHMARK_SAMPLE_COUNT"
            else -> "Benchmark complete"
        }

    fun add(result: NanoDetValidationResult): NanoDetBenchmarkSummary? {
        if (receivedCount < BENCHMARK_WARM_UP_COUNT) {
            receivedCount += 1
            if (receivedCount == BENCHMARK_WARM_UP_COUNT) runtimeStart = RuntimeSnapshot.capture()
            return null
        }
        if (samples.size >= BENCHMARK_SAMPLE_COUNT) return null
        samples += result.timings
        receivedCount += 1
        if (samples.size != BENCHMARK_SAMPLE_COUNT) return null
        return NanoDetBenchmarkSummary.create(
            timings = samples,
            runtimeDelta = runtimeStart?.deltaTo(RuntimeSnapshot.capture()),
            result = result,
        )
    }
}

private data class NanoDetBenchmarkSummary(
    val displayText: String,
) {
    companion object {
        fun create(
            timings: List<NanoDetValidationTimings>,
            runtimeDelta: RuntimeDelta?,
            result: NanoDetValidationResult,
        ): NanoDetBenchmarkSummary {
            val preprocess = timings.map { it.preprocessNanos }
            val onnx = timings.map { it.onnxRuntimeNanos }
            val total = timings.map { it.totalNanos }
            val allocationText = runtimeDelta?.let {
                " | alloc=${it.allocatedBytes} B gc=${it.gcCount} gcTime=${it.gcTimeMs} ms"
            }.orEmpty()
            val stageText = "preprocessing=RGBA_NATIVE_BITMAP normalization=LUT_FLOAT_ARRAY " +
                "priority=${result.workerPriority} " +
                "frame=${result.frameWidth}x${result.frameHeight} rotation=${result.rotationDegrees} " +
                "provider=${result.executionProvider} samples=${timings.size} " +
                "frameTransfer=${timings.map { it.frameTransferNanos }.percentiles()} " +
                "rotateScale=${timings.map { it.rotateScaleNanos }.percentiles()} " +
                "pixelRead=${timings.map { it.pixelReadNanos }.percentiles()} " +
                "normalization=${timings.map { it.normalizationNanos }.percentiles()} " +
                "preprocess=${preprocess.percentiles()} onnx=${onnx.percentiles()} " +
                "postprocess=${timings.map { it.postprocessNanos }.percentiles()} " +
                "total=${total.percentiles()}$allocationText"
            Log.d("NanoDetBenchmark", "SUMMARY_P50_P95_MAX_MS $stageText")
            return NanoDetBenchmarkSummary(
                displayText = "RGBA native / LUT + Array | 100 samples | " +
                    "pre p50/p95/max=${preprocess.percentiles()} ms | " +
                    "onnx=${onnx.percentiles()} ms | total=${total.percentiles()} ms" +
                    allocationText,
            )
        }
    }
}

private data class RuntimeSnapshot(
    val allocatedBytes: Long,
    val gcCount: Long,
    val gcTimeMs: Long,
) {
    fun deltaTo(end: RuntimeSnapshot) = RuntimeDelta(
        allocatedBytes = end.allocatedBytes - allocatedBytes,
        gcCount = end.gcCount - gcCount,
        gcTimeMs = end.gcTimeMs - gcTimeMs,
    )

    companion object {
        fun capture(): RuntimeSnapshot {
            val stats = Debug.getRuntimeStats()
            return RuntimeSnapshot(
                allocatedBytes = stats.longValue("art.gc.bytes-allocated"),
                gcCount = stats.longValue("art.gc.gc-count"),
                gcTimeMs = stats.longValue("art.gc.gc-time"),
            )
        }
    }
}

private data class RuntimeDelta(
    val allocatedBytes: Long,
    val gcCount: Long,
    val gcTimeMs: Long,
)

private fun Map<String, String>.longValue(key: String): Long = get(key)?.toLongOrNull() ?: 0L

private fun List<Long>.percentiles(): String {
    val sorted = sorted()
    fun percentile(fraction: Double): Double {
        val index = ((sorted.size - 1) * fraction).toInt().coerceIn(sorted.indices)
        return sorted[index] / NANOS_PER_MILLISECOND
    }
    return "${formatMilliseconds(percentile(0.50))}/" +
        "${formatMilliseconds(percentile(0.95))}/" +
        formatMilliseconds(sorted.last() / NANOS_PER_MILLISECOND)
}

private fun formatMilliseconds(value: Double): String =
    String.format(Locale.US, "%.2f", value)

private fun modelDestination(context: Context): File =
    File(context.filesDir, "models/plate_detector.onnx")

private fun copyModelToPrivateStorage(context: Context, source: Uri): File {
    val destination = modelDestination(context)
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

private data class SelectedModel(
    val file: File,
    val revision: Long,
)

private const val BENCHMARK_WARM_UP_COUNT = 5
private const val BENCHMARK_SAMPLE_COUNT = 100
private const val NANOS_PER_MILLISECOND = 1_000_000.0
