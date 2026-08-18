package core.domain.camera.analysis.nanodet

import android.os.Process
import android.os.Trace
import android.util.Log
import core.domain.camera.analysis.AndroidCameraFrame
import core.domain.camera.analysis.CameraFrameAnalyzer
import java.io.File
import kotlin.math.roundToInt

/**
 * Android-only analyzer for visually validating the NanoDet integration in the sample app.
 *
 * This exposes both detector-space debug data and source-frame coordinates after phase 4 mapping.
 */
class NanoDetValidationAnalyzer(
    modelFile: File,
    confidenceThreshold: Float = 0.50f,
    nmsIouThreshold: Float = 0.60f,
    private val workerPriority: NanoDetWorkerPriority = NanoDetWorkerPriority.BACKGROUND,
    executionProvider: NanoDetValidationExecutionProvider =
        NanoDetValidationExecutionProvider.XNNPACK,
) : CameraFrameAnalyzer<NanoDetValidationResult> {
    private val detector = NanoDetDetector(
        modelFile = modelFile,
        config = NanoDetConfig(
            confidenceThreshold = confidenceThreshold,
            nmsIouThreshold = nmsIouThreshold,
            executionProvider = executionProvider.detectorProvider,
        ),
    )

    override suspend fun analyze(frame: AndroidCameraFrame): NanoDetValidationResult {
        val threadId = Process.myTid()
        val originalPriority = Process.getThreadPriority(threadId)
        val requestedPriority = workerPriority.androidPriority
        if (originalPriority != requestedPriority) Process.setThreadPriority(requestedPriority)
        return try {
            Trace.beginSection("NanoDet/pipeline")
            analyzeFrame(frame)
        } finally {
            Trace.endSection()
            if (Process.getThreadPriority(threadId) != originalPriority) {
                Process.setThreadPriority(originalPriority)
            }
        }
    }

    private fun analyzeFrame(frame: AndroidCameraFrame): NanoDetValidationResult {
        val totalStartNanos = System.nanoTime()
        val frameRun = detector.analyzeFrameWithTimings(frame, includePreview = false)
        val preprocessResult = frameRun.preprocess
        val inferenceRun = frameRun.inference
        val detections = inferenceRun.detections.map { detection ->
            NanoDetValidationDetection(
                left = detection.boundingBox.left,
                top = detection.boundingBox.top,
                right = detection.boundingBox.right,
                bottom = detection.boundingBox.bottom,
                confidence = detection.confidence,
            )
        }
        val sourceDetections = frameRun.sourceDetections.map { detection ->
            NanoDetValidationSourceDetection(
                left = detection.boundingBox.left,
                top = detection.boundingBox.top,
                right = detection.boundingBox.right,
                bottom = detection.boundingBox.bottom,
                confidence = detection.confidence,
            )
        }
        val totalNanos = elapsedNanoseconds(totalStartNanos)
        val timings = NanoDetValidationTimings(
            frameTransferNanos = preprocessResult.timings.frameTransferNanos,
            rotateScaleNanos = preprocessResult.timings.rotateScaleNanos,
            pixelReadNanos = preprocessResult.timings.pixelReadNanos,
            normalizationNanos = preprocessResult.timings.normalizationNanos,
            previewSnapshotNanos = preprocessResult.timings.previewSnapshotNanos,
            preprocessNanos = frameRun.preprocessNanos,
            onnxRuntimeNanos = inferenceRun.onnxRuntimeNanos,
            postprocessNanos = inferenceRun.postprocessNanos,
            totalNanos = totalNanos,
        )

        Log.d(
            DEBUG_LOG_TAG,
            "PIPELINE cameraTimestampNs=${frame.timestamp} " +
                "frame=${frame.width}x${frame.height} format=${frame.format} " +
                "rotation=${frame.rotationDegrees} " +
                "preprocessing=RGBA_NATIVE_BITMAP normalization=LUT_FLOAT_ARRAY " +
                "priority=$workerPriority " +
                "frameTransferNs=${timings.frameTransferNanos} " +
                "rotateScaleNs=${timings.rotateScaleNanos} " +
                "pixelReadNs=${timings.pixelReadNanos} " +
                "normalizationNs=${timings.normalizationNanos} " +
                "previewSnapshotNs=${timings.previewSnapshotNanos} " +
                "preprocessNs=${timings.preprocessNanos} " +
                "onnxNs=${timings.onnxRuntimeNanos} " +
                "postprocessNs=${timings.postprocessNanos} " +
                "totalNs=$totalNanos detections=${detections.size} " +
                "sourceDetections=${sourceDetections.size} " +
                "provider=${detector.executionProviderName}",
        )
        return NanoDetValidationResult(
            detections = detections,
            sourceDetections = sourceDetections,
            timings = timings,
            workerPriority = workerPriority,
            executionProvider = detector.executionProviderName,
            frameWidth = frame.width,
            frameHeight = frame.height,
            rotationDegrees = frame.rotationDegrees,
        )
    }

    override fun close() {
        detector.close()
    }

    private fun elapsedNanoseconds(startNanos: Long): Long = System.nanoTime() - startNanos

    private companion object {
        const val DEBUG_LOG_TAG = "NanoDetDebug"
    }
}

data class NanoDetValidationResult(
    val detections: List<NanoDetValidationDetection>,
    val sourceDetections: List<NanoDetValidationSourceDetection>,
    val timings: NanoDetValidationTimings,
    val workerPriority: NanoDetWorkerPriority,
    val executionProvider: String,
    val frameWidth: Int,
    val frameHeight: Int,
    val rotationDegrees: Int,
) {
    val processingTimeMs: Double
        get() = timings.totalNanos.toMilliseconds()
}

data class NanoDetValidationTimings(
    val frameTransferNanos: Long,
    val rotateScaleNanos: Long,
    val pixelReadNanos: Long,
    val normalizationNanos: Long,
    val previewSnapshotNanos: Long,
    val preprocessNanos: Long,
    val onnxRuntimeNanos: Long,
    val postprocessNanos: Long,
    val totalNanos: Long,
)

enum class NanoDetValidationExecutionProvider(
    internal val detectorProvider: NanoDetExecutionProvider,
) {
    CPU(NanoDetExecutionProvider.CPU),
    XNNPACK(NanoDetExecutionProvider.XNNPACK),
}

enum class NanoDetWorkerPriority(
    internal val androidPriority: Int,
) {
    BACKGROUND(Process.THREAD_PRIORITY_BACKGROUND),
    NORMAL(Process.THREAD_PRIORITY_DEFAULT),
}

data class NanoDetValidationDetection(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
    val confidence: Float,
)

data class NanoDetValidationSourceDetection(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
    val confidence: Float,
)

internal fun normalizedBgrChwToArgb(
    input: FloatArray,
    inputSize: Int = NanoDetModelContract.INPUT_SIZE,
): IntArray {
    require(inputSize > 0) { "inputSize must be positive." }
    val planeSize = inputSize * inputSize
    require(input.size == 3 * planeSize) {
        "Normalized detector input contains ${input.size} values; expected ${3 * planeSize}."
    }

    return IntArray(planeSize) { index ->
        val blue = denormalizeChannel(input[index], channel = 0)
        val green = denormalizeChannel(input[planeSize + index], channel = 1)
        val red = denormalizeChannel(input[2 * planeSize + index], channel = 2)
        (0xFF shl 24) or (red shl 16) or (green shl 8) or blue
    }
}

private fun denormalizeChannel(value: Float, channel: Int): Int =
    (value * NanoDetModelContract.standardDeviation[channel] + NanoDetModelContract.mean[channel])
        .roundToInt()
        .coerceIn(0, 255)

internal fun Long.toMilliseconds(): Double = this / NANOS_PER_MILLISECOND

private const val NANOS_PER_MILLISECOND = 1_000_000.0
