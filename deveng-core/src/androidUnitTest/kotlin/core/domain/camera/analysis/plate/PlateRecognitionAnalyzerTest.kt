package core.domain.camera.analysis.plate

import android.graphics.PixelFormat
import android.graphics.Rect
import androidx.camera.core.ImageInfo
import androidx.camera.core.ImageProxy
import core.domain.camera.analysis.AndroidCameraFrame
import core.domain.camera.analysis.nanodet.model.PlateBoundingBox
import core.domain.camera.analysis.nanodet.model.PlateDetectionResult
import core.domain.camera.analysis.ocr.model.FastPlateOcrResult
import core.domain.camera.analysis.ocr.roi.OcrBboxPadding
import core.domain.camera.analysis.plate.engine.PlateDetectorEngine
import core.domain.camera.analysis.plate.engine.PlateOcrEngine
import core.domain.camera.analysis.plate.engine.PlateRecognitionEngines
import core.domain.camera.analysis.plate.manager.ModelSessionReleasePolicy
import java.lang.reflect.Proxy
import java.util.ArrayDeque
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking

class PlateRecognitionAnalyzerTest {
    @Test
    fun aNewAnalyzerStartsWithFreshOcrConsensusState() {
        val detection = detection(confidence = 0.90f)
        val detector = FakeDetector(detections = listOf(detection, detection))
        val ocr = FakeOcr(results = listOf(read("34ABC123"), read("34ABC123")))
        val config = PlateRecognitionConfig(
            minimumBoundingBoxWidthPx = 1f,
            minimumBoundingBoxHeightPx = 1f,
            requiredMatchingReads = 2,
            validationWindowSize = 2,
        )
        val firstAnalyzer = analyzer(detector, ocr, config)
        firstAnalyzer.start()
        assertNull(runBlocking { firstAnalyzer.analyze(testFrame()) })
        firstAnalyzer.close()

        val secondDetector = FakeDetector(detections = listOf(detection, detection))
        val secondOcr = FakeOcr(results = listOf(read("34ABC123"), read("34ABC123")))
        val secondAnalyzer = analyzer(secondDetector, secondOcr, config)
        secondAnalyzer.start()
        assertNull(runBlocking { secondAnalyzer.analyze(testFrame()) })
        secondAnalyzer.close()
    }

    @Test
    fun rejectsAutomaticSessionReleaseForContinuousRecognition() {
        assertFailsWith<IllegalArgumentException> {
            PlateRecognitionConfig(
                lockAfterSuccessfulRead = false,
                modelSessionReleasePolicy = ModelSessionReleasePolicy.AFTER_SUCCESS,
            )
        }
    }

    @Test
    fun emitsValidatedResultAndLocksFurtherInference() {
        val detection = detection(confidence = 0.90f)
        val detector = FakeDetector(
            detections = listOf(
                detection,
                detection,
                detection,
                detection,
            ),
        )
        val ocr = FakeOcr(
            results = listOf(
                read("34ABC123", 0.95f),
                read("34ABC124", 0.93f),
                read("34ABC123", 0.97f),
                read("35XYZ789", 0.95f),
            ),
        )
        val analyzer = analyzer(detector, ocr)

        analyzer.start()
        try {
            assertNull(runBlocking { analyzer.analyze(testFrame()) })
            assertNull(runBlocking { analyzer.analyze(testFrame()) })
            val result = runBlocking { analyzer.analyze(testFrame()) }

            assertEquals("34ABC123", result?.plate)
            assertEquals(0.96f, result?.confidence ?: 0f, absoluteTolerance = 0.0001f)
            assertEquals(3, detector.callCount)
            assertEquals(3, ocr.callCount)
            assertNull(runBlocking { analyzer.analyze(testFrame()) })
            assertEquals(3, detector.callCount)
            assertEquals(3, ocr.callCount)
        } finally {
            analyzer.close()
        }

        assertEquals(true, detector.closed)
        assertEquals(true, ocr.closed)
    }

    @Test
    fun continuousModeEmitsAResultForEachMatchingReadSequence() {
        val detection = detection(confidence = 0.90f)
        val detector = FakeDetector(
            detections = List(6) { detection },
        )
        val ocr = FakeOcr(
            results = List(6) { read("34ABC123") },
        )
        val snapshots = mutableListOf<PlateRecognitionDebugSnapshot>()
        val analyzer = analyzer(
            detector = detector,
            ocr = ocr,
            config = PlateRecognitionConfig(
                minimumBoundingBoxWidthPx = 1f,
                minimumBoundingBoxHeightPx = 1f,
                lockAfterSuccessfulRead = false,
            ),
            onDebugSnapshot = { snapshots += it },
        )

        analyzer.start()
        try {
            assertNull(runBlocking { analyzer.analyze(testFrame()) })
            assertNull(runBlocking { analyzer.analyze(testFrame()) })
            assertEquals(
                "34ABC123",
                runBlocking { analyzer.analyze(testFrame()) }?.plate,
            )
            assertNull(runBlocking { analyzer.analyze(testFrame()) })
            assertNull(runBlocking { analyzer.analyze(testFrame()) })
            assertEquals(
                "34ABC123",
                runBlocking { analyzer.analyze(testFrame()) }?.plate,
            )
        } finally {
            analyzer.close()
        }

        assertEquals(6, detector.callCount)
        assertEquals(6, ocr.callCount)
        assertEquals(12, snapshots.size)
        assertEquals(6, snapshots.count { it.ocr?.status == PlateRecognitionDebugOcrStatus.COMPLETED })
    }

    @Test
    fun qualityGatePreventsOcrForInvalidDetection() {
        val detector = FakeDetector(
            detections = listOf(
                detection(confidence = 0.40f),
            ),
        )
        val ocr = FakeOcr(results = listOf(read("34ABC123")))
        val analyzer = analyzer(detector, ocr)

        analyzer.start()
        try {
            assertNull(runBlocking { analyzer.analyze(testFrame()) })
        } finally {
            analyzer.close()
        }

        assertEquals(1, detector.callCount)
        assertEquals(0, ocr.callCount)
    }

    @Test
    fun framesSkippedWhileOcrIsInFlightWithoutStartingConcurrentOcr() {
        val detection = detection(confidence = 0.90f)
        val detector = FakeDetector(detections = List(4) { detection })
        val firstOcrStarted = CountDownLatch(1)
        val releaseFirstOcr = CountDownLatch(1)
        val ocr = BlockingFirstOcr(
            results = List(4) { read("34ABC123") },
            firstOcrStarted = firstOcrStarted,
            releaseFirstOcr = releaseFirstOcr,
        )
        val snapshots = CopyOnWriteArrayList<PlateRecognitionDebugSnapshot>()
        val analyzer = analyzer(
            detector = detector,
            ocr = ocr,
            config = PlateRecognitionConfig(
                minimumBoundingBoxWidthPx = 1f,
                minimumBoundingBoxHeightPx = 1f,
                requiredMatchingReads = 2,
                validationWindowSize = 2,
            ),
            onDebugSnapshot = { snapshots += it },
        )
        val executor = Executors.newFixedThreadPool(2)

        analyzer.start()
        try {
            val first = executor.submit<PlateRecognitionResult?> {
                runBlocking { analyzer.analyze(testFrame()) }
            }
            assertTrue(firstOcrStarted.await(2, TimeUnit.SECONDS))

            val skippedFrame = executor.submit<PlateRecognitionResult?> {
                runBlocking { analyzer.analyze(testFrame()) }
            }
            assertNull(skippedFrame.get(2, TimeUnit.SECONDS))
            assertEquals(1, ocr.callCount)
            assertTrue(
                snapshots.any {
                    it.ocr?.status == PlateRecognitionDebugOcrStatus.SKIPPED_IN_FLIGHT
                },
            )

            releaseFirstOcr.countDown()
            assertNull(first.get(2, TimeUnit.SECONDS))

            val second = runBlocking { analyzer.analyze(testFrame()) }
            assertEquals("34ABC123", second?.plate)
            assertEquals(3, detector.callCount)
            assertEquals(2, ocr.callCount)
        } finally {
            releaseFirstOcr.countDown()
            executor.shutdownNow()
            analyzer.close()
        }
    }

    @Test
    fun publishesSourceFrameDetectionSnapshotBeforeOcrValidation() {
        val detection = detection(confidence = 0.90f)
        val detector = FakeDetector(detections = listOf(detection))
        val ocr = FakeOcr(results = listOf(read("34ABC123")))
        val snapshots = mutableListOf<PlateRecognitionDebugSnapshot>()
        val analyzer = analyzer(
            detector = detector,
            ocr = ocr,
            onDebugSnapshot = { snapshots += it },
        )

        analyzer.start()
        try {
            assertNull(runBlocking { analyzer.analyze(testFrame()) })
        } finally {
            analyzer.close()
        }

        assertEquals(2, snapshots.size)
        assertEquals(
            PlateRecognitionDebugSnapshot(
                frameWidth = 640,
                frameHeight = 360,
                rotationDegrees = 0,
                cameraTimestampNanos = 1L,
                detections = listOf(
                    PlateRecognitionDebugDetection(
                        left = 10f,
                        top = 10f,
                        right = 100f,
                        bottom = 40f,
                        confidence = 0.90f,
                        passesQualityGate = true,
                    ),
                ),
            ),
            snapshots.first().copy(detectorNanos = 0L, processNanos = 0L),
        )
        val rawOcr = requireNotNull(snapshots.last().ocr)
        assertEquals(PlateRecognitionDebugOcrStatus.COMPLETED, rawOcr.status)
        assertEquals("34ABC123", rawOcr.text)
        assertEquals(0.95f, rawOcr.confidence)
        assertTrue(snapshots.last().processNanos >= snapshots.last().detectorNanos)
        assertTrue(snapshots.last().processNanos >= requireNotNull(rawOcr.durationNanos))
    }

    @Test
    fun stopClearsPendingValidationAndStartBeginsANewSession() {
        val detection = detection(confidence = 0.90f)
        val detector = FakeDetector(
            detections = listOf(
                detection,
                detection,
                detection,
                detection,
            ),
        )
        val ocr = FakeOcr(
            results = listOf(
                read("34ABC123"),
                read("34ABC123"),
                read("34ABC123"),
                read("34ABC123"),
            ),
        )
        val analyzer = analyzer(detector, ocr)

        analyzer.start()
        try {
            assertNull(runBlocking { analyzer.analyze(testFrame()) })
            analyzer.stop()
            analyzer.start()

            assertNull(runBlocking { analyzer.analyze(testFrame()) })
            assertNull(runBlocking { analyzer.analyze(testFrame()) })
            assertEquals(
                "34ABC123",
                runBlocking { analyzer.analyze(testFrame()) }?.plate,
            )
        } finally {
            analyzer.close()
        }
    }

    private fun analyzer(
        detector: FakeDetector,
        ocr: PlateOcrEngine,
        config: PlateRecognitionConfig = PlateRecognitionConfig(
            minimumBoundingBoxWidthPx = 1f,
            minimumBoundingBoxHeightPx = 1f,
        ),
        onDebugSnapshot: ((PlateRecognitionDebugSnapshot) -> Unit)? = null,
    ): PlateRecognitionAnalyzer = PlateRecognitionAnalyzer(
        engines = PlateRecognitionEngines(
            detector = detector,
            ocr = ocr,
        ),
        config = config,
        onDebugSnapshot = onDebugSnapshot,
    )

    private fun detection(confidence: Float): PlateDetectionResult = PlateDetectionResult(
        boundingBox = PlateBoundingBox(
            left = 10f,
            top = 10f,
            right = 100f,
            bottom = 40f,
        ),
        confidence = confidence,
    )

    private fun read(
        text: String,
        confidence: Float = 0.95f,
    ): FastPlateOcrResult = FastPlateOcrResult(
        plateText = text,
        characterConfidences = emptyList(),
        confidence = confidence,
    )

    private fun testFrame(
        width: Int = 640,
        height: Int = 360,
    ): AndroidCameraFrame {
        val imageInfo = Proxy.newProxyInstance(
            ImageInfo::class.java.classLoader,
            arrayOf(ImageInfo::class.java),
        ) { _, method, _ ->
            when (method.name) {
                "getRotationDegrees" -> 0
                "getTimestamp" -> 1L
                else -> defaultValue(method.returnType)
            }
        } as ImageInfo
        val imageProxy = Proxy.newProxyInstance(
            ImageProxy::class.java.classLoader,
            arrayOf(ImageProxy::class.java),
        ) { _, method, _ ->
            when (method.name) {
                "getWidth" -> width
                "getHeight" -> height
                "getFormat" -> PixelFormat.RGBA_8888
                "getImageInfo" -> imageInfo
                "getCropRect" -> Rect(0, 0, width, height)
                "close" -> null
                else -> defaultValue(method.returnType)
            }
        } as ImageProxy
        return AndroidCameraFrame(imageProxy)
    }

    private fun defaultValue(type: Class<*>): Any? = when (type) {
        Boolean::class.javaPrimitiveType -> false
        Byte::class.javaPrimitiveType -> 0.toByte()
        Short::class.javaPrimitiveType -> 0.toShort()
        Int::class.javaPrimitiveType -> 0
        Long::class.javaPrimitiveType -> 0L
        Float::class.javaPrimitiveType -> 0f
        Double::class.javaPrimitiveType -> 0.0
        Char::class.javaPrimitiveType -> '\u0000'
        else -> null
    }

    private class FakeDetector(
        detections: List<PlateDetectionResult?>,
    ) : PlateDetectorEngine {
        private val pendingDetections = ArrayDeque(detections)
        var callCount: Int = 0
            private set
        var closed: Boolean = false
            private set
        var closeCount: Int = 0
            private set

        override suspend fun detect(frame: AndroidCameraFrame): PlateDetectionResult? {
            callCount++
            return if (pendingDetections.isEmpty()) {
                null
            } else {
                pendingDetections.removeFirst()
            }
        }

        override fun close() {
            closeCount++
            closed = true
        }
    }

    private class FakeOcr(
        results: List<FastPlateOcrResult>,
    ) : PlateOcrEngine {
        private val pendingResults = ArrayDeque(results)
        var callCount: Int = 0
            private set
        var closed: Boolean = false
            private set
        var closeCount: Int = 0
            private set

        override fun recognize(
            frame: AndroidCameraFrame,
            boundingBox: PlateBoundingBox,
            padding: OcrBboxPadding,
        ): FastPlateOcrResult {
            callCount++
            return pendingResults.removeFirst()
        }

        override fun close() {
            closeCount++
            closed = true
        }
    }

    private class BlockingFirstOcr(
        results: List<FastPlateOcrResult>,
        private val firstOcrStarted: CountDownLatch,
        private val releaseFirstOcr: CountDownLatch,
    ) : PlateOcrEngine {
        private val pendingResults = ArrayDeque(results)
        var callCount: Int = 0
            private set

        override fun recognize(
            frame: AndroidCameraFrame,
            boundingBox: PlateBoundingBox,
            padding: OcrBboxPadding,
        ): FastPlateOcrResult {
            callCount++
            if (callCount == 1) {
                frame.releaseAfterRead()
                firstOcrStarted.countDown()
                check(releaseFirstOcr.await(2, TimeUnit.SECONDS)) {
                    "Timed out while waiting to release the first OCR call."
                }
            }
            return pendingResults.removeFirst()
        }

        override fun close() = Unit
    }
}
