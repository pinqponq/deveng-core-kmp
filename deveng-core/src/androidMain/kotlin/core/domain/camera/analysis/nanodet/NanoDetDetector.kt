package core.domain.camera.analysis.nanodet

import ai.onnxruntime.OnnxJavaType
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.TensorInfo
import android.os.Trace
import android.util.Log
import core.domain.camera.analysis.AndroidCameraFrame
import core.domain.camera.analysis.CameraFrameAnalyzer
import core.domain.camera.analysis.onnx.OnnxExecutionProvider
import core.domain.camera.analysis.onnx.OnnxModelErrorCode
import core.domain.camera.analysis.onnx.OnnxModelException
import core.domain.camera.analysis.onnx.OnnxModelSession
import core.domain.camera.analysis.onnx.validateModelFile
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.util.concurrent.atomic.AtomicBoolean

internal class NanoDetDetector(
    modelFile: File,
    config: NanoDetConfig = NanoDetConfig(),
    environment: OrtEnvironment? = null,
) : CameraFrameAnalyzer<List<SourcePlateDetection>> {
    private val validatedModelFile = validateModelFile(modelFile)
    private val runtimeEnvironment = environment ?: OrtEnvironment.getEnvironment()
    private val sessionSelection = openPreferredSession(config)
    private val modelSession = sessionSelection.session
    private val preprocessor = NanoDetPreprocessor()
    private val postprocessor = NanoDetPostprocessor(config)
    private val inputBuffer = allocateFloatBuffer(NanoDetModelContract.INPUT_VALUE_COUNT)
    private val outputBuffer = allocateFloatBuffer(NanoDetModelContract.OUTPUT_VALUE_COUNT)
    private lateinit var inputTensor: OnnxTensor
    private lateinit var outputTensor: OnnxTensor
    private lateinit var inputFeed: Map<String, OnnxTensor>
    private lateinit var pinnedOutput: Map<String, OnnxTensor>
    private val closed = AtomicBoolean(false)

    val executionProviderName: String = sessionSelection.provider.name

    init {
        try {
            validateModelContract()
            inputTensor = OnnxTensor.createTensor(
                runtimeEnvironment,
                inputBuffer,
                NanoDetModelContract.inputShape,
            )
            outputTensor = OnnxTensor.createTensor(
                runtimeEnvironment,
                outputBuffer,
                NanoDetModelContract.outputShape,
            )
            inputFeed = mapOf(NanoDetModelContract.INPUT_NAME to inputTensor)
            pinnedOutput = mapOf(NanoDetModelContract.OUTPUT_NAME to outputTensor)
        } catch (error: Throwable) {
            if (::outputTensor.isInitialized) outputTensor.close()
            if (::inputTensor.isInitialized) inputTensor.close()
            modelSession.close()
            throw error
        }
    }

    override suspend fun analyze(frame: AndroidCameraFrame): List<SourcePlateDetection>? =
        analyzeFrameWithTimings(frame, includePreview = false)
            .sourceDetections
            .ifEmpty { null }

    internal fun analyzeFrameWithTimings(
        frame: AndroidCameraFrame,
        includePreview: Boolean,
    ): NanoDetFrameRun {
        checkOpen()
        val preprocessStartNanos = System.nanoTime()
        val preprocess = traced("NanoDet/preprocess") {
            preprocessor.preprocessInto(
                frame = frame,
                destination = inputBuffer,
                includePreview = includePreview,
            )
        }
        val preprocessNanos = elapsedNanoseconds(preprocessStartNanos)
        return try {
            val inference = runPreparedInference()
            NanoDetFrameRun(
                preprocess = preprocess,
                preprocessNanos = preprocessNanos,
                inference = inference,
                sourceDetections = FrameCoordinateMapper.map(
                    detections = inference.detections,
                    transform = preprocess.transform,
                ),
            )
        } catch (error: Throwable) {
            preprocess.detectorInput?.recycle()
            throw error
        }
    }

    private fun runPreparedInference(): NanoDetInferenceRun {
        checkOpen()
        try {
            val onnxRuntimeStartNanos = System.nanoTime()
            val inferenceResult = traced("NanoDet/ort-run") {
                modelSession.session.run(inputFeed, pinnedOutput)
            }
            val onnxRuntimeNanos = elapsedNanoseconds(onnxRuntimeStartNanos)
            inferenceResult.use {
                val postprocessStartNanos = System.nanoTime()
                val detections = traced("NanoDet/postprocess") {
                    postprocessor.decode(outputBuffer)
                }
                val postprocessNanos = elapsedNanoseconds(postprocessStartNanos)
                return NanoDetInferenceRun(
                    detections = detections,
                    onnxRuntimeNanos = onnxRuntimeNanos,
                    postprocessNanos = postprocessNanos,
                )
            }
        } catch (error: OnnxModelException) {
            throw error
        } catch (error: Exception) {
            throw OnnxModelException(
                code = OnnxModelErrorCode.DETECTOR_INFERENCE_FAILED,
                message = "NanoDet inference failed.",
                cause = error,
            )
        }
    }

    override fun close() {
        if (!closed.compareAndSet(false, true)) return
        if (::outputTensor.isInitialized) outputTensor.close()
        if (::inputTensor.isInitialized) inputTensor.close()
        preprocessor.close()
        modelSession.close()
    }

    private fun checkOpen() {
        check(!closed.get()) { "NanoDet detector is already closed." }
    }

    private fun openPreferredSession(config: NanoDetConfig): SessionSelection {
        if (config.executionProvider == NanoDetExecutionProvider.CPU) {
            return SessionSelection(openCpuSession(), OnnxExecutionProvider.CPU)
        }
        return try {
            SessionSelection(
                session = OnnxModelSession.open(
                    modelFile = validatedModelFile,
                    environment = runtimeEnvironment,
                    intraOpNumThreads = XNNPACK_ORT_INTRA_OP_THREADS,
                    interOpNumThreads = INTER_OP_NUM_THREADS,
                    executionProvider = OnnxExecutionProvider.XNNPACK,
                    xnnpackIntraOpNumThreads = config.xnnpackThreads,
                    disableIntraOpSpinning = true,
                ),
                provider = OnnxExecutionProvider.XNNPACK,
            )
        } catch (error: Throwable) {
            Log.w(
                LOG_TAG,
                "XNNPACK initialization failed; falling back to CPU EP: ${error.message}",
            )
            SessionSelection(openCpuSession(), OnnxExecutionProvider.CPU)
        }
    }

    private fun openCpuSession(): OnnxModelSession = OnnxModelSession.open(
        modelFile = validatedModelFile,
        environment = runtimeEnvironment,
        executionProvider = OnnxExecutionProvider.CPU,
    )

    private fun validateModelContract() {
        val session = modelSession.session
        validateTensor(
            nodeName = NanoDetModelContract.INPUT_NAME,
            actualNodes = session.inputInfo,
            expectedShape = NanoDetModelContract.inputShape,
            errorCode = OnnxModelErrorCode.INVALID_MODEL_INPUT,
        )
        validateTensor(
            nodeName = NanoDetModelContract.OUTPUT_NAME,
            actualNodes = session.outputInfo,
            expectedShape = NanoDetModelContract.outputShape,
            errorCode = OnnxModelErrorCode.INVALID_MODEL_OUTPUT,
        )
    }

    private fun validateTensor(
        nodeName: String,
        actualNodes: Map<String, ai.onnxruntime.NodeInfo>,
        expectedShape: LongArray,
        errorCode: OnnxModelErrorCode,
    ) {
        if (actualNodes.size != 1 || nodeName !in actualNodes) {
            invalidContract(
                errorCode,
                "Expected only NanoDet node '$nodeName', found ${actualNodes.keys}.",
            )
        }
        val tensorInfo = actualNodes.getValue(nodeName).info as? TensorInfo
            ?: invalidContract(errorCode, "NanoDet node '$nodeName' is not a tensor.")
        if (tensorInfo.type != OnnxJavaType.FLOAT || !tensorInfo.shape.contentEquals(expectedShape)) {
            invalidContract(
                errorCode,
                "NanoDet node '$nodeName' must be FLOAT${expectedShape.contentToString()}, " +
                    "found ${tensorInfo.type}${tensorInfo.shape.contentToString()}.",
            )
        }
    }

    private fun invalidContract(code: OnnxModelErrorCode, message: String): Nothing =
        throw OnnxModelException(code = code, message = message)

    private fun elapsedNanoseconds(startNanos: Long): Long = System.nanoTime() - startNanos

    private inline fun <T> traced(sectionName: String, block: () -> T): T {
        Trace.beginSection(sectionName)
        return try {
            block()
        } finally {
            Trace.endSection()
        }
    }

    private fun allocateFloatBuffer(floatCount: Int): FloatBuffer = ByteBuffer
        .allocateDirect(floatCount * Float.SIZE_BYTES)
        .order(ByteOrder.nativeOrder())
        .asFloatBuffer()

    private companion object {
        const val LOG_TAG = "NanoDetDebug"
        const val XNNPACK_ORT_INTRA_OP_THREADS = 1
        const val INTER_OP_NUM_THREADS = 1
    }
}

private data class SessionSelection(
    val session: OnnxModelSession,
    val provider: OnnxExecutionProvider,
)

internal data class NanoDetFrameRun(
    val preprocess: NanoDetPreprocessResult,
    val preprocessNanos: Long,
    val inference: NanoDetInferenceRun,
    val sourceDetections: List<SourcePlateDetection>,
)

internal data class NanoDetInferenceRun(
    val detections: List<PlateDetection>,
    val onnxRuntimeNanos: Long,
    val postprocessNanos: Long,
)
