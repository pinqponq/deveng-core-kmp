package global.deveng.core

import core.domain.camera.analysis.plate.PlateRecognitionDebugOcrStatus
import core.domain.camera.analysis.plate.PlateRecognitionDebugSnapshot
import java.util.Locale
import kotlin.math.ceil

/**
 * Validation-screen-only aggregation for Phase 6 device runs.
 *
 * A snapshot is published twice for an OCR frame (before and after OCR), so the latest snapshot
 * for each CameraX timestamp replaces the preliminary one. This prevents detector/OCR percentiles
 * from counting the same frame twice.
 */
internal class PlateRecognitionPhase6Metrics {
    private val snapshotsByTimestamp = linkedMapOf<Long, PlateRecognitionDebugSnapshot>()
    private val callbackLatenciesNanos = mutableListOf<Long>()
    private var resultCount = 0

    @Synchronized
    fun recordSnapshot(snapshot: PlateRecognitionDebugSnapshot) {
        val timestamp = snapshot.cameraTimestampNanos
        if (timestamp > 0L) {
            snapshotsByTimestamp[timestamp] = snapshot
        }
    }

    @Synchronized
    fun recordResult(snapshot: PlateRecognitionDebugSnapshot?, callbackNanos: Long) {
        resultCount++
        val frameTimestamp = snapshot?.cameraTimestampNanos ?: return
        if (frameTimestamp > 0L && callbackNanos >= frameTimestamp) {
            callbackLatenciesNanos += callbackNanos - frameTimestamp
        }
    }

    @Synchronized
    fun summary(): PlateRecognitionPhase6Summary {
        val frames = snapshotsByTimestamp.values.toList()
        val timestamps = snapshotsByTimestamp.keys.sorted()
        val frameGaps = timestamps.zipWithNext { previous, next -> next - previous }
            .filter { it > 0L }
        val ocr = frames.mapNotNull { it.ocr }
        return PlateRecognitionPhase6Summary(
            analyzedFrameCount = frames.size,
            resultCount = resultCount,
            detector = percentileSummary(frames.map { it.detectorNanos }.filter { it > 0L }),
            ocr = percentileSummary(ocr.mapNotNull { it.durationNanos }.filter { it > 0L }),
            total = percentileSummary(frames.map { it.processNanos }.filter { it > 0L }),
            callbackLatency = percentileSummary(callbackLatenciesNanos),
            analysisFrameGap = percentileSummary(frameGaps),
            ocrFailureCount = ocr.count { it.status == PlateRecognitionDebugOcrStatus.FAILED },
        )
    }

    private fun percentileSummary(values: List<Long>): PlateRecognitionPercentiles? {
        if (values.isEmpty()) return null
        val sorted = values.sorted()
        return PlateRecognitionPercentiles(
            p50Nanos = percentile(sorted, 0.50),
            p95Nanos = percentile(sorted, 0.95),
        )
    }

    private fun percentile(sorted: List<Long>, percentile: Double): Long =
        sorted[(ceil(percentile * sorted.size).toInt() - 1).coerceIn(0, sorted.lastIndex)]
}

internal data class PlateRecognitionPhase6Summary(
    val analyzedFrameCount: Int,
    val resultCount: Int,
    val detector: PlateRecognitionPercentiles?,
    val ocr: PlateRecognitionPercentiles?,
    val total: PlateRecognitionPercentiles?,
    val callbackLatency: PlateRecognitionPercentiles?,
    val analysisFrameGap: PlateRecognitionPercentiles?,
    val ocrFailureCount: Int,
)

internal data class PlateRecognitionPercentiles(
    val p50Nanos: Long,
    val p95Nanos: Long,
)

internal fun PlateRecognitionPercentiles?.formatPhase6(): String = when (this) {
    null -> "-"
    else -> String.format(
        Locale.US,
        "p50 %.1f ms / p95 %.1f ms",
        p50Nanos / 1_000_000.0,
        p95Nanos / 1_000_000.0,
    )
}
