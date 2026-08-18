package core.domain.camera.analysis

import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import kotlinx.coroutines.runBlocking
import java.util.concurrent.Executor
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/**
 * Bridges the suspend-based analyzer contract to CameraX's callback-based analyzer API.
 *
 * The adapter keeps at most one generic analysis in flight. Incoming frames are released when the
 * analyzer is busy, while CameraX itself is configured with KEEP_ONLY_LATEST by the controller.
 */
internal class CameraFrameAnalyzerAdapter<R>(
    private val analyzer: CameraFrameAnalyzer<R>,
    private val executor: Executor,
    private val onResult: (R) -> Unit,
    private val onError: (Throwable) -> Unit,
    private val analyzeEveryNthFrame: Int = 1,
    private val minAnalysisIntervalMs: Long = 0,
    private val diagnostics: FrameAnalysisDiagnostics? = null,
    private val nanoTime: () -> Long = System::nanoTime,
) : ImageAnalysis.Analyzer {
    private val busy = AtomicBoolean(false)
    private val stopRequested = AtomicBoolean(false)
    private val closed = AtomicBoolean(false)
    private val receivedFrameCount = AtomicLong(0)
    private val analysisCount = AtomicLong(0)
    private val droppedBusyCount = AtomicLong(0)
    private val skippedCadenceCount = AtomicLong(0)
    private val skippedIntervalCount = AtomicLong(0)
    private val lastAnalysisStartNanos = AtomicLong(0)
    private val previousAnalysisStartNanos = AtomicLong(0)

    init {
        require(analyzeEveryNthFrame > 0) { "analyzeEveryNthFrame must be positive." }
        require(minAnalysisIntervalMs >= 0) { "minAnalysisIntervalMs cannot be negative." }
    }

    fun start() {
        if (!stopRequested.get() && !closed.get()) {
            analyzer.start()
        }
    }

    fun stop() {
        if (stopRequested.compareAndSet(false, true)) {
            analyzer.stop()
        }
    }

    fun close() {
        stop()
        if (!busy.get()) {
            closeAnalyzer()
        }
    }

    override fun analyze(imageProxy: ImageProxy) {
        if (stopRequested.get() || closed.get()) {
            imageProxy.close()
            return
        }
        val frameSequence = receivedFrameCount.incrementAndGet()
        if ((frameSequence - 1L) % analyzeEveryNthFrame != 0L) {
            skippedCadenceCount.incrementAndGet()
            imageProxy.close()
            return
        }
        val candidateStartNanos = nanoTime()
        val previousAcceptedStart = lastAnalysisStartNanos.get()
        if (
            previousAcceptedStart != 0L &&
            candidateStartNanos - previousAcceptedStart <
            minAnalysisIntervalMs * NANOS_PER_MILLISECOND
        ) {
            skippedIntervalCount.incrementAndGet()
            imageProxy.close()
            return
        }
        if (!busy.compareAndSet(false, true)) {
            if (diagnostics != null) droppedBusyCount.incrementAndGet()
            imageProxy.close()
            return
        }

        val analysisSequence = diagnostics?.let { analysisCount.incrementAndGet() }
        val analysisStartedNanos = candidateStartNanos
        lastAnalysisStartNanos.set(analysisStartedNanos)
        if (diagnostics != null && analysisSequence != null) {
            val previousStart = previousAnalysisStartNanos.getAndSet(analysisStartedNanos)
            reportAnalysisStarted(
                FrameAnalysisStarted(
                    analysisSequence = analysisSequence,
                    adapterFrameSequence = frameSequence,
                    droppedBusySincePrevious = droppedBusyCount.getAndSet(0),
                    skippedCadenceSincePrevious = skippedCadenceCount.getAndSet(0),
                    skippedIntervalSincePrevious = skippedIntervalCount.getAndSet(0),
                    startGapMs = previousStart
                        .takeIf { it != 0L }
                        ?.let { (analysisStartedNanos - it) / NANOS_PER_MILLISECOND },
                    cameraTimestampNanos = imageProxy.imageInfo.timestamp,
                    callbackFrameAgeNanos = imageProxy.imageInfo.timestamp
                        .takeIf { it > 0L && analysisStartedNanos >= it }
                        ?.let { analysisStartedNanos - it },
                ),
            )
        }

        try {
            executor.execute {
                val executionStartedNanos = nanoTime()
                var outcome = "no_result"
                try {
                    if (!stopRequested.get() && !closed.get()) {
                        val result = runBlocking {
                            analyzer.analyze(AndroidCameraFrame(imageProxy))
                        }
                        if (!stopRequested.get() && !closed.get()) {
                            result?.let {
                                outcome = "result"
                                onResult(it)
                            }
                        }
                    }
                } catch (error: Exception) {
                    outcome = "error:${error::class.simpleName}"
                    if (!stopRequested.get() && !closed.get()) {
                        onError(error)
                    }
                } finally {
                    val finishedNanos = nanoTime()
                    analysisSequence?.let {
                        reportAnalysisFinished(
                            FrameAnalysisFinished(
                                analysisSequence = it,
                                queueWaitNanos = executionStartedNanos - analysisStartedNanos,
                                executionNanos = finishedNanos - executionStartedNanos,
                                callbackToCloseNanos = finishedNanos - analysisStartedNanos,
                                outcome = outcome,
                            ),
                        )
                    }
                    imageProxy.close()
                    busy.set(false)
                    if (stopRequested.get()) {
                        closeAnalyzer()
                    }
                }
            }
        } catch (error: RejectedExecutionException) {
            analysisSequence?.let {
                reportAnalysisFinished(
                    FrameAnalysisFinished(
                        analysisSequence = it,
                        queueWaitNanos = 0,
                        executionNanos = 0,
                        callbackToCloseNanos = nanoTime() - analysisStartedNanos,
                        outcome = "rejected",
                    ),
                )
            }
            imageProxy.close()
            busy.set(false)
            if (stopRequested.get()) {
                closeAnalyzer()
            } else if (!closed.get()) {
                onError(error)
            }
        }
    }

    private fun closeAnalyzer() {
        if (closed.compareAndSet(false, true)) {
            analyzer.close()
        }
    }

    private fun reportAnalysisStarted(event: FrameAnalysisStarted) {
        try {
            diagnostics?.onAnalysisStarted(event)
        } catch (_: Exception) {
            // Debug diagnostics must never break camera frame ownership.
        }
    }

    private fun reportAnalysisFinished(event: FrameAnalysisFinished) {
        try {
            diagnostics?.onAnalysisFinished(event)
        } catch (_: Exception) {
            // Debug diagnostics must never break camera frame ownership.
        }
    }

    private companion object {
        const val NANOS_PER_MILLISECOND = 1_000_000L
    }
}

internal interface FrameAnalysisDiagnostics {
    fun onAnalysisStarted(event: FrameAnalysisStarted)
    fun onAnalysisFinished(event: FrameAnalysisFinished)
}

internal data class FrameAnalysisStarted(
    val analysisSequence: Long,
    val adapterFrameSequence: Long,
    val droppedBusySincePrevious: Long,
    val skippedCadenceSincePrevious: Long,
    val skippedIntervalSincePrevious: Long,
    val startGapMs: Long?,
    val cameraTimestampNanos: Long,
    val callbackFrameAgeNanos: Long?,
)

internal data class FrameAnalysisFinished(
    val analysisSequence: Long,
    val queueWaitNanos: Long,
    val executionNanos: Long,
    val callbackToCloseNanos: Long,
    val outcome: String,
)

internal class LogcatFrameAnalysisDiagnostics(
    private val tag: String,
) : FrameAnalysisDiagnostics {
    override fun onAnalysisStarted(event: FrameAnalysisStarted) {
        Log.d(
            tag,
            "FRAME_START analysis=${event.analysisSequence} " +
                "adapterFrame=${event.adapterFrameSequence} " +
                "droppedBusy=${event.droppedBusySincePrevious} " +
                "skippedCadence=${event.skippedCadenceSincePrevious} " +
                "skippedInterval=${event.skippedIntervalSincePrevious} " +
                "gapMs=${event.startGapMs ?: "first"} " +
                "cameraTimestampNs=${event.cameraTimestampNanos} " +
                "callbackFrameAgeNs=${event.callbackFrameAgeNanos ?: "unknown"}",
        )
    }

    override fun onAnalysisFinished(event: FrameAnalysisFinished) {
        Log.d(
            tag,
            "FRAME_DONE analysis=${event.analysisSequence} " +
                "queueWaitNs=${event.queueWaitNanos} " +
                "executionNs=${event.executionNanos} " +
                "callbackToCloseNs=${event.callbackToCloseNanos} outcome=${event.outcome}",
        )
    }
}
