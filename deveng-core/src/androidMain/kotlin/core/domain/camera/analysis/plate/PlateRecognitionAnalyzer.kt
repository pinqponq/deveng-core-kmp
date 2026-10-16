package core.domain.camera.analysis.plate

import core.domain.camera.analysis.AndroidCameraFrame
import core.domain.camera.analysis.CameraFrameAnalyzer
import core.domain.camera.analysis.FrameAnalysisOutputFormat
import core.domain.camera.analysis.nanodet.model.PlateDetectionResult
import core.domain.camera.analysis.ocr.model.FastPlateOcrResult
import core.domain.camera.analysis.ocr.roi.OcrBboxPadding
import core.domain.camera.analysis.plate.engine.PlateRecognitionEngines
import core.domain.camera.analysis.plate.manager.PlateRecognitionModelSession
import core.domain.camera.analysis.plate.validation.PlateQualityGate
import core.domain.camera.analysis.plate.validation.PlateResultValidator
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/**
 * Runs the complete source-frame plate recognition pipeline.
 *
 * The camera analysis use case must provide [requiredOutputImageFormat]. With the current
 * NanoDet and OCR preprocessors this is [FrameAnalysisOutputFormat.RGBA_8888].
 */
class PlateRecognitionAnalyzer internal constructor(
    private val engines: PlateRecognitionEngines,
    private val config: PlateRecognitionConfig,
    private val onDebugSnapshot: ((PlateRecognitionDebugSnapshot) -> Unit)? = null,
    private val modelSession: PlateRecognitionModelSession? = null
) : CameraFrameAnalyzer<PlateRecognitionResult> {

    /**
     * Creates an analyzer from model files stored by the consumer application.
     *
     * Model files are opened once and the sessions are reused for every frame.
     */
    constructor(
        detectorModelFile: File,
        ocrModelFile: File,
        config: PlateRecognitionConfig = PlateRecognitionConfig(),
        onDebugSnapshot: ((PlateRecognitionDebugSnapshot) -> Unit)? = null
    ) : this(
        engines = PlateRecognitionEngines.open(
            detectorModelFile = detectorModelFile,
            ocrModelFile = ocrModelFile,
            config = config
        ),
        config = config,
        onDebugSnapshot = onDebugSnapshot
    )

    /**
     * Creates an analyzer from manager-owned model resources.
     *
     * Closing the analyzer closes its session only. The model manager keeps the
     * detector and OCR sessions alive for a later camera registration.
     */
    internal constructor(
        modelSession: PlateRecognitionModelSession,
        config: PlateRecognitionConfig = PlateRecognitionConfig(),
        onDebugSnapshot: ((PlateRecognitionDebugSnapshot) -> Unit)? = null,
    ) : this(
        engines = PlateRecognitionEngines(
            detector = modelSession.detector,
            ocr = modelSession.ocr
        ),
        config = config,
        onDebugSnapshot = onDebugSnapshot,
        modelSession = modelSession
    )

    /**
     * The output format to pass to [core.domain.camera.controller.CameraController.registerFrameAnalyzer].
     */
    val requiredOutputImageFormat: FrameAnalysisOutputFormat =
        FrameAnalysisOutputFormat.RGBA_8888

    private val qualityGate = PlateQualityGate(
        detectionConfidenceThreshold = config.detectorConfig.confidenceThreshold,
        minimumBoundingBoxWidthPx = config.minimumBoundingBoxWidthPx,
        minimumBoundingBoxHeightPx = config.minimumBoundingBoxHeightPx
    )
    private val resultValidator = PlateResultValidator(
        requiredMatchingReads = config.requiredMatchingReads,
        validationWindowSize = config.validationWindowSize,
        confidenceThreshold = config.ocrConfig.confidenceThreshold
    )
    private val ocrPadding = OcrBboxPadding(
        horizontalRatio = config.bboxPaddingHorizontalRatio,
        verticalRatio = config.bboxPaddingVerticalRatio
    )
    private val isAcceptingFrames = AtomicBoolean(false)
    private val isPlateRecognized = AtomicBoolean(false)
    private val isClosed = AtomicBoolean(false)
    private val isOcrRunning = AtomicBoolean(false)
    private val sessionLock = Any()
    private val debugLock = Any()
    private val sessionGeneration = AtomicLong(0)

    /**
     * Starts a new camera analysis session and clears validation history.
     */
    override fun start() {
        check(!isClosed.get()) { "PlateRecognitionAnalyzer is already closed." }
        synchronized(sessionLock) {
            sessionGeneration.incrementAndGet()
            resultValidator.reset()
            isPlateRecognized.set(false)
            isAcceptingFrames.set(true)
        }
    }

    /**
     * Stops accepting new inference work without disposing model sessions.
     */
    override fun stop() {
        if (isClosed.get()) return
        isAcceptingFrames.set(false)
        synchronized(sessionLock) {
            sessionGeneration.incrementAndGet()
            resultValidator.reset()
        }
    }

    override suspend fun analyze(frame: AndroidCameraFrame): PlateRecognitionResult? {
        val session = getActiveSession() ?: return null

        val processStartNanos = System.nanoTime()
        val detectedPlate = detectAcceptablePlate(frame, session)  // ilgili frame'deki plaka adayı

        publishDebugSnapshot(
            frame = frame,
            detection = detectedPlate.detection,
            acceptedDetection = detectedPlate.detection,
            detectorNanos = detectedPlate.detectorNanos,
            processNanos = System.nanoTime() - processStartNanos,
        )

        val detection = detectedPlate.detection ?: return null  // resimdeki plaka

        if (!isSessionActive(session)) return null

        if (!isOcrRunning.compareAndSet(false, true)) {
            publishDebugSnapshot(
                frame = frame,
                detection = detectedPlate.detection,
                acceptedDetection = detection,
                detectorNanos = detectedPlate.detectorNanos,
                processNanos = System.nanoTime() - processStartNanos,
                ocr = PlateRecognitionDebugOcr.skippedInFlight(),
            )
            frame.releaseAfterRead()
            return null
        }

        try {
            if (!isSessionActive(session)) return null
            return recognizePlate(
                frame = frame,
                session = session,
                candidate = detectedPlate,
                detection = detection,
                processStartNanos = processStartNanos,
            )
        } finally {
            isOcrRunning.set(false)
        }
    }

    private fun recognizePlate(
        frame: AndroidCameraFrame,
        session: Long,
        candidate: FrameCandidate,
        detection: PlateDetectionResult,
        processStartNanos: Long,
    ): PlateRecognitionResult? {
        val ocrStartNanos = System.nanoTime()
        val ocrResult = try {
            engines.ocr.recognize(
                frame = frame,
                boundingBox = detection.boundingBox,
                padding = ocrPadding
            )
        } catch (error: Throwable) {
            publishDebugSnapshot(
                frame = frame,
                detection = candidate.detection,
                acceptedDetection = detection,
                detectorNanos = candidate.detectorNanos,
                processNanos = System.nanoTime() - processStartNanos,
                ocr = PlateRecognitionDebugOcr.failed(
                    durationNanos = System.nanoTime() - ocrStartNanos,
                    error = error.message ?: error::class.simpleName.orEmpty(),
                ),
            )
            throw error
        }

        val consensusResult = validateConsensus(session, ocrResult)

        val ocrTotalNanos = System.nanoTime() - ocrStartNanos
        val inferenceNanos = ocrResult.inferenceNanos.takeIf { it > 0L } ?: ocrTotalNanos
        publishDebugSnapshot(
            frame = frame,
            detection = candidate.detection,
            acceptedDetection = detection,
            detectorNanos = candidate.detectorNanos,
            processNanos = System.nanoTime() - processStartNanos,
            ocr = PlateRecognitionDebugOcr.completed(
                text = ocrResult.plateText,
                confidence = ocrResult.confidence,
                durationNanos = inferenceNanos,
                frameCopyNanos = ocrResult.frameCopyNanos,
                preprocessNanos = ocrResult.preprocessNanos,
                totalNanos = ocrTotalNanos,
                acceptedByConsensus = consensusResult != null,
            ),
        )
        return consensusResult
    }

    /**
     * Disposes both model sessions. This operation is idempotent.
     */
    override fun close() {
        if (!isClosed.compareAndSet(false, true)) return
        isAcceptingFrames.set(false)

        synchronized(sessionLock) {
            sessionGeneration.incrementAndGet()
            resultValidator.reset()
        }

        if (modelSession == null) {
            engines.close()
        } else {
            modelSession.close()
        }
    }

    private fun canFrameAnalyze(): Boolean {
        return isAcceptingFrames.get() && !isPlateRecognized.get() && !isClosed.get()
    }

    private fun getActiveSession(): Long? {
        return synchronized(sessionLock) {
            if (canFrameAnalyze()) sessionGeneration.get() else null
        }
    }

    private fun isSessionActive(session: Long): Boolean {
        return synchronized(sessionLock) {
            sessionGeneration.get() == session && canFrameAnalyze()
        }
    }

    private suspend fun detectAcceptablePlate(
        frame: AndroidCameraFrame,
        session: Long
    ): FrameCandidate {
        if (!isSessionActive(session)) return FrameCandidate.empty()

        val detectorStartNanos = System.nanoTime()
        val rawDetection = engines.detector.detect(frame)
        val detectorNanos = System.nanoTime() - detectorStartNanos
        val detection = qualityGate.selectAcceptableDetection(
            detection = rawDetection,
            frameWidth = frame.width,
            frameHeight = frame.height
        )

        return FrameCandidate(
            detection = detection,
            detectorNanos = detectorNanos
        )
    }


    private fun validateConsensus(
        session: Long,
        ocrResult: FastPlateOcrResult
    ): PlateRecognitionResult? {
        return synchronized(sessionLock) {
            if (!isSessionActive(session)) return@synchronized null

            val validatedResult = resultValidator.validate(ocrResult) ?: return@synchronized null

            if (config.lockAfterSuccessfulRead) {
                if (!isPlateRecognized.compareAndSet(false, true)) {
                    return@synchronized null
                }
                isAcceptingFrames.set(false)
            } else {
                resultValidator.reset()
            }
            validatedResult
        }
    }

    private fun publishDebugSnapshot(
        frame: AndroidCameraFrame,
        detection: PlateDetectionResult?,
        acceptedDetection: PlateDetectionResult?,
        detectorNanos: Long = 0L,
        processNanos: Long = detectorNanos,
        ocr: PlateRecognitionDebugOcr? = null,
    ) {
        val listener = onDebugSnapshot ?: return
        val snapshot = PlateRecognitionDebugSnapshot(
            frameWidth = frame.width,
            frameHeight = frame.height,
            rotationDegrees = frame.rotationDegrees,
            cameraTimestampNanos = frame.timestamp,
            detectorNanos = detectorNanos,
            processNanos = processNanos,
            ocr = ocr,
            detections = listOfNotNull(detection).map { item ->
                val box = item.boundingBox
                PlateRecognitionDebugDetection(
                    left = box.left,
                    top = box.top,
                    right = box.right,
                    bottom = box.bottom,
                    confidence = item.confidence,
                    passesQualityGate = item === acceptedDetection,
                )
            },
        )
        try {
            synchronized(debugLock) {
                listener(snapshot)
            }
        } catch (_: Throwable) {
            // Debug reporting must not break frame ownership or inference.
        }
    }

    private data class FrameCandidate(
        val detection: PlateDetectionResult?,
        val detectorNanos: Long,
    ) {
        companion object {
            fun empty() = FrameCandidate(
                detection = null,
                detectorNanos = 0L,
            )
        }
    }

}


/** Source-frame detection data exposed for the sample validation overlay. */
data class PlateRecognitionDebugSnapshot(
    val frameWidth: Int,
    val frameHeight: Int,
    val rotationDegrees: Int,
    /** CameraX sensor timestamp for correlating a completed result with its source frame. */
    val cameraTimestampNanos: Long = 0L,
    val detections: List<PlateRecognitionDebugDetection>,
    val detectorNanos: Long = 0L,
    val processNanos: Long = detectorNanos,
    val ocr: PlateRecognitionDebugOcr? = null,
)

/** Raw OCR execution state for validation only; it never changes production result rules. */
data class PlateRecognitionDebugOcr(
    val status: PlateRecognitionDebugOcrStatus,
    val text: String? = null,
    val confidence: Float? = null,
    val durationNanos: Long? = null,
    val frameCopyNanos: Long? = null,
    val preprocessNanos: Long? = null,
    val totalNanos: Long? = null,
    val acceptedByConsensus: Boolean = false,
    val error: String? = null,
) {
    companion object {
        fun completed(
            text: String,
            confidence: Float,
            durationNanos: Long,
            frameCopyNanos: Long,
            preprocessNanos: Long,
            totalNanos: Long,
            acceptedByConsensus: Boolean,
        ) = PlateRecognitionDebugOcr(
            status = PlateRecognitionDebugOcrStatus.COMPLETED,
            text = text,
            confidence = confidence,
            durationNanos = durationNanos,
            frameCopyNanos = frameCopyNanos,
            preprocessNanos = preprocessNanos,
            totalNanos = totalNanos,
            acceptedByConsensus = acceptedByConsensus,
        )

        fun failed(durationNanos: Long, error: String) = PlateRecognitionDebugOcr(
            status = PlateRecognitionDebugOcrStatus.FAILED,
            durationNanos = durationNanos,
            error = error,
        )

        fun skippedInFlight() = PlateRecognitionDebugOcr(
            status = PlateRecognitionDebugOcrStatus.SKIPPED_IN_FLIGHT,
        )
    }
}

enum class PlateRecognitionDebugOcrStatus {
    COMPLETED,
    FAILED,
    SKIPPED_IN_FLIGHT,
}

data class PlateRecognitionDebugDetection(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
    val confidence: Float,
    val passesQualityGate: Boolean,
)