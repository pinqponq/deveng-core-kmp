package core.domain.camera.analysis

import android.graphics.ImageFormat
import android.graphics.Rect
import androidx.camera.core.ImageInfo
import androidx.camera.core.ImageProxy
import java.lang.reflect.Proxy
import java.util.concurrent.CountDownLatch
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CameraFrameAnalyzerTest {

    @Test
    fun dummyAnalyzerProducesResultAndFrameIsClosedOnce() {
        val executor = Executors.newSingleThreadExecutor()
        try {
            val recordingProxy = RecordingImageProxy(rotationDegrees = 90)
            val completed = CountDownLatch(1)
            var result: Int? = null
            var error: Throwable? = null
            val analyzer = object : CameraFrameAnalyzer<Int> {
                override suspend fun analyze(frame: AndroidCameraFrame): Int {
                    assertEquals(1, frame.width)
                    assertEquals(1, frame.height)
                    assertEquals(90, frame.rotationDegrees)
                    return frame.width + frame.height
                }
            }
            val adapter = CameraFrameAnalyzerAdapter(
                analyzer = analyzer,
                executor = executor,
                onResult = {
                    result = it
                    completed.countDown()
                },
                onError = {
                    error = it
                    completed.countDown()
                },
            )

            adapter.start()
            adapter.analyze(recordingProxy.proxy)

            assertTrue(completed.await(2, TimeUnit.SECONDS))
            assertTrue(recordingProxy.awaitClosed())
            assertEquals(2, result)
            assertNull(error)
            assertEquals(1, recordingProxy.closeCount)

            adapter.close()
        } finally {
            executor.shutdownNow()
        }
    }

    @Test
    fun analyzerFailureReportsErrorAndClosesFrame() {
        val executor = Executors.newSingleThreadExecutor()
        try {
            val recordingProxy = RecordingImageProxy()
            val completed = CountDownLatch(1)
            var error: Throwable? = null
            val adapter = CameraFrameAnalyzerAdapter(
                analyzer = object : CameraFrameAnalyzer<Unit> {
                    override suspend fun analyze(frame: AndroidCameraFrame): Unit {
                        throw IllegalStateException("synthetic analyzer failure")
                    }
                },
                executor = executor,
                onResult = { completed.countDown() },
                onError = {
                    error = it
                    completed.countDown()
                },
            )

            adapter.analyze(recordingProxy.proxy)

            assertTrue(completed.await(2, TimeUnit.SECONDS))
            assertTrue(recordingProxy.awaitClosed())
            assertTrue(error is IllegalStateException)
            assertEquals(1, recordingProxy.closeCount)

            adapter.close()
        } finally {
            executor.shutdownNow()
        }
    }

    @Test
    fun busyAnalyzerDropsNewFrameAndReleasesIt() {
        val executor = Executors.newSingleThreadExecutor()
        try {
            val firstProxy = RecordingImageProxy()
            val secondProxy = RecordingImageProxy()
            val started = CountDownLatch(1)
            val release = CountDownLatch(1)
            val finished = CountDownLatch(1)
            val adapter = CameraFrameAnalyzerAdapter(
                analyzer = object : CameraFrameAnalyzer<Unit> {
                    override suspend fun analyze(frame: AndroidCameraFrame) {
                        started.countDown()
                        release.await(2, TimeUnit.SECONDS)
                        finished.countDown()
                    }
                },
                executor = executor,
                onResult = {},
                onError = { throw it },
            )

            adapter.analyze(firstProxy.proxy)
            assertTrue(started.await(2, TimeUnit.SECONDS))

            adapter.analyze(secondProxy.proxy)
            assertEquals(1, secondProxy.closeCount)

            release.countDown()
            assertTrue(finished.await(2, TimeUnit.SECONDS))
            assertTrue(firstProxy.awaitClosed())
            assertEquals(1, firstProxy.closeCount)

            adapter.close()
        } finally {
            executor.shutdownNow()
        }
    }

    @Test
    fun diagnosticsReportDroppedFramesAtNextAnalysisStart() {
        val executor = Executors.newSingleThreadExecutor()
        try {
            val firstProxy = RecordingImageProxy()
            val droppedProxy = RecordingImageProxy()
            val nextProxy = RecordingImageProxy()
            val firstStarted = CountDownLatch(1)
            val releaseFirst = CountDownLatch(1)
            val secondFinished = CountDownLatch(1)
            val diagnostics = RecordingFrameAnalysisDiagnostics()
            var analysisCalls = 0
            val adapter = CameraFrameAnalyzerAdapter(
                analyzer = object : CameraFrameAnalyzer<Unit> {
                    override suspend fun analyze(frame: AndroidCameraFrame) {
                        analysisCalls += 1
                        if (analysisCalls == 1) {
                            firstStarted.countDown()
                            releaseFirst.await(2, TimeUnit.SECONDS)
                        } else {
                            secondFinished.countDown()
                        }
                    }
                },
                executor = executor,
                onResult = {},
                onError = { throw it },
                diagnostics = diagnostics,
            )

            adapter.analyze(firstProxy.proxy)
            assertTrue(firstStarted.await(2, TimeUnit.SECONDS))
            adapter.analyze(droppedProxy.proxy)
            assertEquals(1, droppedProxy.closeCount)

            releaseFirst.countDown()
            assertTrue(firstProxy.awaitClosed())
            executor.submit {}.get(2, TimeUnit.SECONDS)

            adapter.analyze(nextProxy.proxy)
            assertTrue(secondFinished.await(2, TimeUnit.SECONDS))
            assertTrue(nextProxy.awaitClosed())

            assertEquals(2, diagnostics.started.size)
            assertEquals(1L, diagnostics.started[0].analysisSequence)
            assertEquals(1L, diagnostics.started[0].adapterFrameSequence)
            assertEquals(0L, diagnostics.started[0].droppedBusySincePrevious)
            assertEquals(0L, diagnostics.started[0].skippedCadenceSincePrevious)
            assertEquals(0L, diagnostics.started[0].skippedIntervalSincePrevious)
            assertEquals(2L, diagnostics.started[1].analysisSequence)
            assertEquals(3L, diagnostics.started[1].adapterFrameSequence)
            assertEquals(1L, diagnostics.started[1].droppedBusySincePrevious)
            assertEquals(0L, diagnostics.started[1].skippedCadenceSincePrevious)
            assertEquals(0L, diagnostics.started[1].skippedIntervalSincePrevious)
            assertEquals(2, diagnostics.finished.size)

            adapter.close()
        } finally {
            executor.shutdownNow()
        }
    }

    @Test
    fun cadenceAnalyzesFirstAndEveryFifthFrameAndClosesAllFrames() {
        val diagnostics = RecordingFrameAnalysisDiagnostics()
        val results = mutableListOf<Int>()
        var analysisCalls = 0
        val adapter = CameraFrameAnalyzerAdapter(
            analyzer = object : CameraFrameAnalyzer<Int> {
                override suspend fun analyze(frame: AndroidCameraFrame): Int {
                    analysisCalls += 1
                    return analysisCalls
                }
            },
            executor = { command -> command.run() },
            onResult = { results += it },
            onError = { throw it },
            analyzeEveryNthFrame = 5,
            diagnostics = diagnostics,
        )
        val frames = List(11) { RecordingImageProxy() }

        frames.forEach { adapter.analyze(it.proxy) }

        assertEquals(3, analysisCalls)
        assertEquals(listOf(1, 2, 3), results)
        assertTrue(frames.all { it.closeCount == 1 })
        assertEquals(listOf(1L, 6L, 11L), diagnostics.started.map { it.adapterFrameSequence })
        assertEquals(listOf(0L, 4L, 4L), diagnostics.started.map { it.skippedCadenceSincePrevious })
        adapter.close()
    }

    @Test
    fun minimumIntervalSkipsCandidatesUntilEnoughTimeHasElapsed() {
        var nowNanos = 1_000_000_000L
        val diagnostics = RecordingFrameAnalysisDiagnostics()
        var analysisCalls = 0
        val adapter = CameraFrameAnalyzerAdapter(
            analyzer = object : CameraFrameAnalyzer<Unit> {
                override suspend fun analyze(frame: AndroidCameraFrame) {
                    analysisCalls += 1
                }
            },
            executor = { command -> command.run() },
            onResult = {},
            onError = { throw it },
            minAnalysisIntervalMs = 750,
            diagnostics = diagnostics,
            nanoTime = { nowNanos },
        )
        val first = RecordingImageProxy()
        val tooEarly = RecordingImageProxy()
        val accepted = RecordingImageProxy()

        adapter.analyze(first.proxy)
        nowNanos += 100_000_000L
        adapter.analyze(tooEarly.proxy)
        nowNanos += 650_000_000L
        adapter.analyze(accepted.proxy)

        assertEquals(2, analysisCalls)
        assertEquals(listOf(1L, 3L), diagnostics.started.map { it.adapterFrameSequence })
        assertEquals(1L, diagnostics.started[1].skippedIntervalSincePrevious)
        assertEquals(1, first.closeCount)
        assertEquals(1, tooEarly.closeCount)
        assertEquals(1, accepted.closeCount)
        adapter.close()
    }

    @Test
    fun diagnosticsSeparateQueueWaitExecutionAndCallbackDuration() {
        var nowNanos = 100L
        val diagnostics = RecordingFrameAnalysisDiagnostics()
        val adapter = CameraFrameAnalyzerAdapter(
            analyzer = object : CameraFrameAnalyzer<Unit> {
                override suspend fun analyze(frame: AndroidCameraFrame) {
                    nowNanos = 250L
                }
            },
            executor = { command ->
                nowNanos = 150L
                command.run()
            },
            onResult = {},
            onError = { throw it },
            diagnostics = diagnostics,
            nanoTime = { nowNanos },
        )

        adapter.analyze(RecordingImageProxy().proxy)

        assertEquals(1, diagnostics.started.size)
        assertEquals(99L, diagnostics.started.single().callbackFrameAgeNanos)
        val finished = diagnostics.finished.single()
        assertEquals(50L, finished.queueWaitNanos)
        assertEquals(100L, finished.executionNanos)
        assertEquals(150L, finished.callbackToCloseNanos)
        adapter.close()
    }

    @Test
    fun closeStopsAndClosesAnalyzerOnce() {
        val calls = mutableListOf<String>()
        val executor = Executors.newSingleThreadExecutor()
        try {
            val adapter = CameraFrameAnalyzerAdapter(
                analyzer = object : CameraFrameAnalyzer<Unit> {
                    override suspend fun analyze(frame: AndroidCameraFrame) = Unit
                    override fun start() {
                        calls += "start"
                    }

                    override fun stop() {
                        calls += "stop"
                    }

                    override fun close() {
                        calls += "close"
                    }
                },
                executor = executor,
                onResult = {},
                onError = {},
            )

            adapter.start()
            adapter.close()
            adapter.close()

            assertEquals(listOf("start", "stop", "close"), calls)
        } finally {
            executor.shutdownNow()
        }
    }

    @Test
    fun rejectedExecutionReportsErrorAndReleasesFrame() {
        val recordingProxy = RecordingImageProxy()
        var reportedError: Throwable? = null
        val adapter = CameraFrameAnalyzerAdapter(
            analyzer = object : CameraFrameAnalyzer<Unit> {
                override suspend fun analyze(frame: AndroidCameraFrame) = Unit
            },
            executor = { throw RejectedExecutionException("synthetic rejection") },
            onResult = {},
            onError = { reportedError = it },
        )

        adapter.analyze(recordingProxy.proxy)

        assertTrue(reportedError is RejectedExecutionException)
        assertEquals(1, recordingProxy.closeCount)
        adapter.close()
    }

    private class RecordingImageProxy(
        rotationDegrees: Int = 0,
    ) {
        private val closeCounter = AtomicInteger(0)
        private val closed = CountDownLatch(1)
        private val imageInfo = Proxy.newProxyInstance(
            ImageInfo::class.java.classLoader,
            arrayOf(ImageInfo::class.java),
        ) { _, method, _ ->
            when (method.name) {
                "getRotationDegrees" -> rotationDegrees
                "getTimestamp" -> 1L
                else -> defaultValue(method.returnType)
            }
        } as ImageInfo

        val proxy: ImageProxy = Proxy.newProxyInstance(
            ImageProxy::class.java.classLoader,
            arrayOf(ImageProxy::class.java),
        ) { _, method, _ ->
            when (method.name) {
                "close" -> {
                    closeCounter.incrementAndGet()
                    closed.countDown()
                    null
                }
                "getFormat" -> ImageFormat.YUV_420_888
                "getWidth", "getHeight" -> 1
                "getCropRect" -> Rect(0, 0, 1, 1)
                "getImageInfo" -> imageInfo
                "getImage" -> null
                "toString" -> "RecordingImageProxy"
                else -> defaultValue(method.returnType)
            }
        } as ImageProxy

        val closeCount: Int
            get() = closeCounter.get()

        fun awaitClosed(): Boolean = closed.await(2, TimeUnit.SECONDS)

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
    }

    private class RecordingFrameAnalysisDiagnostics : FrameAnalysisDiagnostics {
        val started = CopyOnWriteArrayList<FrameAnalysisStarted>()
        val finished = CopyOnWriteArrayList<FrameAnalysisFinished>()

        override fun onAnalysisStarted(event: FrameAnalysisStarted) {
            started += event
        }

        override fun onAnalysisFinished(event: FrameAnalysisFinished) {
            finished += event
        }
    }
}
