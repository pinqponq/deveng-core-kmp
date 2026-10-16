package core.domain.camera.analysis.nanodet

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import core.domain.camera.analysis.AndroidCameraFrame
import core.domain.camera.analysis.CameraFrameAnalyzer
import core.domain.camera.analysis.nanodet.geometry.FrameCoordinateMapper
import core.domain.camera.analysis.nanodet.model.NanoDetModelProfile
import core.domain.camera.analysis.nanodet.model.PlateDetectionResult
import core.domain.camera.analysis.nanodet.pipeline.NanoDetPostprocessor
import core.domain.camera.analysis.nanodet.pipeline.NanoDetPreprocessor
import core.domain.camera.analysis.onnx.OnnxExecutionProvider
import core.domain.camera.analysis.onnx.OnnxModelSession
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.util.concurrent.atomic.AtomicBoolean

internal class NanoDetDetector(
    private val modelFile: File,
    private val config: NanoDetConfig = NanoDetConfig(),
    environment: OrtEnvironment? = null
) : CameraFrameAnalyzer<PlateDetectionResult?> {
    private val runtimeEnvironment = environment ?: OrtEnvironment.getEnvironment()
    private val modelSession = openModelSession()
    private val preprocessor: NanoDetPreprocessor
    private val postprocessor: NanoDetPostprocessor
    private val inputBuffer: FloatBuffer
    private val outputBuffer: FloatBuffer
    private val inputTensor: OnnxTensor
    private val outputTensor: OnnxTensor
    private val inputFeed: Map<String, OnnxTensor>
    private val pinnedOutput: Map<String, OnnxTensor>
    private val isDetectorClosed = AtomicBoolean(false)

    init {
        var createdPreprocessor: NanoDetPreprocessor? = null
        var createdInputTensor: OnnxTensor? = null
        var createdOutputTensor: OnnxTensor? = null

        try {
            val resolvedModelContract = NanoDetModelProfile.resolve(modelSession.metadata)
            val inputShape = resolvedModelContract.input.resolvedShape
            val inputHeight = inputShape[2].toInt()
            val inputWidth = inputShape[3].toInt()

            createdPreprocessor = NanoDetPreprocessor(
                inputWidth = inputWidth,
                inputHeight = inputHeight
            )
            val createdPostprocessor = NanoDetPostprocessor(
                config = config,
                inputWidth = inputWidth,
                inputHeight = inputHeight
            )

            val createdInputBuffer = allocateFloatBuffer(resolvedModelContract.input.valueCount)
            val createdOutputBuffer = allocateFloatBuffer(resolvedModelContract.output.valueCount)

            createdInputTensor = OnnxTensor.createTensor(
                runtimeEnvironment,
                createdInputBuffer,
                resolvedModelContract.input.resolvedShape.toLongArray()
            )
            createdOutputTensor = OnnxTensor.createTensor(
                runtimeEnvironment,
                createdOutputBuffer,
                resolvedModelContract.output.resolvedShape.toLongArray()
            )

            preprocessor = createdPreprocessor
            postprocessor = createdPostprocessor
            inputBuffer = createdInputBuffer
            outputBuffer = createdOutputBuffer
            inputTensor = createdInputTensor
            outputTensor = createdOutputTensor
            inputFeed = mapOf(resolvedModelContract.input.name to createdInputTensor)
            pinnedOutput = mapOf(resolvedModelContract.output.name to createdOutputTensor)
        } catch (error: Throwable) {
            createdOutputTensor?.close()
            createdInputTensor?.close()
            createdPreprocessor?.close()
            modelSession.close()
            throw error
        }
    }

    override suspend fun analyze(frame: AndroidCameraFrame): PlateDetectionResult? {
        checkModelSessionNotClosed()

        val transform = preprocessor.preprocessInto(
            frame = frame,
            destination = inputBuffer
        )

        val detection = runInference()
        val sourceDetection = FrameCoordinateMapper.map(
            detection = detection,
            transform = transform
        )

        return sourceDetection
    }

    /** Runs one contract-shaped dummy inference so ONNX Runtime initializes before camera use. */
    internal fun warmUp() {
        checkModelSessionNotClosed()

        inputBuffer.clear()
        while (inputBuffer.hasRemaining()) {
            inputBuffer.put(0f)
        }
        inputBuffer.rewind()

        modelSession.session.run(inputFeed, pinnedOutput).close()
    }

    private fun runInference(): PlateDetectionResult? {
        modelSession.session.run(inputFeed, pinnedOutput).use {
            return postprocessor.decode(predictions = outputBuffer)
        }
    }

    override fun close() {  // işlem bittikten sonra model parametrelerini kapatarak bellek sızıntısının önüne geçer
        if (!isDetectorClosed.compareAndSet(false, true)) return
        outputTensor.close()
        inputTensor.close()
        preprocessor.close()
        modelSession.close()
    }

    private fun openModelSession(): OnnxModelSession {
        if (config.executionProvider == OnnxExecutionProvider.CPU) {
            return openCpuSession()
        }
        return try {
            OnnxModelSession.open(
                modelFile = modelFile,
                environment = runtimeEnvironment,
                intraOpNumThreads = config.ortIntraOpThreads,
                interOpNumThreads = config.ortInterOpThreads,
                executionProvider = OnnxExecutionProvider.XNNPACK,
                xnnpackIntraOpNumThreads = config.xnnpackThreads,
                disableIntraOpSpinning = true
            )
        } catch (_: Throwable) {
            openCpuSession()
        }
    }

    private fun openCpuSession(): OnnxModelSession {
        return OnnxModelSession.open(
            modelFile = modelFile,
            environment = runtimeEnvironment,
            intraOpNumThreads = config.ortIntraOpThreads,
            interOpNumThreads = config.ortInterOpThreads,
            executionProvider = OnnxExecutionProvider.CPU,
            disableIntraOpSpinning = true
        )
    }

    private fun checkModelSessionNotClosed() {
        check(!isDetectorClosed.get()) { "NanoDet detector is already closed." }
    }

    // Allocates a direct native FloatBuffer for zero-copy ONNX inference.
    private fun allocateFloatBuffer(floatCount: Int): FloatBuffer {
        return ByteBuffer
            .allocateDirect(floatCount * Float.SIZE_BYTES)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
    }
}
