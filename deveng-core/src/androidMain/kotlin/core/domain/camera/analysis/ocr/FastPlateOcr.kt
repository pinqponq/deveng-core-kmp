package core.domain.camera.analysis.ocr

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import core.domain.camera.analysis.AndroidCameraFrame
import core.domain.camera.analysis.nanodet.model.PlateBoundingBox
import core.domain.camera.analysis.ocr.model.FastPlateOcrModelProfile
import core.domain.camera.analysis.ocr.model.FastPlateOcrResult
import core.domain.camera.analysis.ocr.pipeline.OcrDecoder
import core.domain.camera.analysis.ocr.pipeline.OcrPreprocessor
import core.domain.camera.analysis.ocr.roi.CapturedCameraFrame
import core.domain.camera.analysis.ocr.roi.FullFrameCapture
import core.domain.camera.analysis.ocr.roi.OcrBboxPadding
import core.domain.camera.analysis.onnx.OnnxExecutionProvider
import core.domain.camera.analysis.onnx.OnnxModelSession
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.util.concurrent.atomic.AtomicBoolean

/** File-backed fast-plate-ocr v5 wrapper with source-ROI preprocessing. */
internal class FastPlateOcr(
    private val modelFile: File,
    environment: OrtEnvironment? = null,
    private val config: FastPlateOcrConfig = FastPlateOcrConfig()
) : AutoCloseable {
    private val runtimeEnvironment = environment ?: OrtEnvironment.getEnvironment()
    private val modelSession = openModelSession()
    private val preprocessor: OcrPreprocessor
    private val inputBuffer: FloatBuffer
    private val outputBuffer: FloatBuffer
    private val inputTensor: OnnxTensor
    private val outputTensor: OnnxTensor
    private val inputFeed: Map<String, OnnxTensor>
    private val pinnedOutput: Map<String, OnnxTensor>
    private val isOcrClosed = AtomicBoolean(false)

    init {
        var createdPreprocessor: OcrPreprocessor? = null
        var createdInputTensor: OnnxTensor? = null
        var createdOutputTensor: OnnxTensor? = null

        try {
            val resolvedModelContract = FastPlateOcrModelProfile.resolve(modelSession.metadata)
            val inputShape = resolvedModelContract.input.resolvedShape
            createdPreprocessor = OcrPreprocessor(
                inputWidth = inputShape[2].toInt(),
                inputHeight = inputShape[1].toInt()
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

    /**
     * Reads and prepares the source ROI, releases the camera frame, then runs OCR on the detached
     * tensor. The caller must not use [frame] after this method returns.
     */
    fun recognize(
        frame: AndroidCameraFrame,
        boundingBox: PlateBoundingBox,
        padding: OcrBboxPadding = OcrBboxPadding()
    ): FastPlateOcrResult {
        lateinit var fullFrame: CapturedCameraFrame
        var frameCopyNanos = 0L
        var preprocessNanos = 0L

        try {
            checkModelSessionNotClosed()
            val frameCopyStartedNanos = System.nanoTime()
            fullFrame = FullFrameCapture.capture(frame)
            frameCopyNanos = System.nanoTime() - frameCopyStartedNanos
            val preprocessStartedNanos = System.nanoTime()
            preprocessor.preprocessInto(
                frame = frame,
                boundingBox = boundingBox,
                destination = inputBuffer,
                padding = padding
            )
            preprocessNanos = System.nanoTime() - preprocessStartedNanos
        } finally {
            frame.releaseAfterRead()
        }

        val inferenceStartedNanos = System.nanoTime()
        val result = runInference().copy(
            fullFrame = fullFrame,
            frameCopyNanos = frameCopyNanos,
            preprocessNanos = preprocessNanos,
            inferenceNanos = System.nanoTime() - inferenceStartedNanos
        )

        return result
    }

    override fun close() {
        if (!isOcrClosed.compareAndSet(false, true)) return
        outputTensor.close()
        inputTensor.close()
        preprocessor.close()
        modelSession.close()
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

    private fun runInference(): FastPlateOcrResult {
        modelSession.session.run(inputFeed, pinnedOutput).use {
            return OcrDecoder.decode(outputBuffer)
        }
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
        check(!isOcrClosed.get()) { "FastPlateOCR is already closed." }
    }

    private fun allocateFloatBuffer(floatCount: Int): FloatBuffer {
        return ByteBuffer
            .allocateDirect(floatCount * Float.SIZE_BYTES)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
    }
}
