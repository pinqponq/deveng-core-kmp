package core.domain.camera.controller

import android.graphics.ImageFormat
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue
import java.lang.reflect.Proxy

class CameraImageAnalysisTest {

    @Test
    fun emptyMultiplexerClosesTheIncomingProxy() {
        val recordingProxy = RecordingImageProxy()

        MultiplexingAnalyzer(emptyList()).analyze(recordingProxy.proxy)

        assertEquals(1, recordingProxy.closeCount)
    }

    @Test
    fun singleAnalyzerReceivesTheOriginalProxy() {
        val recordingProxy = RecordingImageProxy()
        var received: ImageProxy? = null
        val analyzer = recordingAnalyzer { received = it }

        MultiplexingAnalyzer(listOf(analyzer)).analyze(recordingProxy.proxy)

        assertSame(recordingProxy.proxy, received)
        assertEquals(0, recordingProxy.closeCount)

        received?.close()
        assertEquals(1, recordingProxy.closeCount)
    }

    @Test
    fun multipleAnalyzersReleaseTheUnderlyingProxyAfterTheLastClose() {
        val recordingProxy = RecordingImageProxy()
        val received = mutableListOf<ImageProxy>()
        val analyzers = listOf(
            recordingAnalyzer { received += it },
            recordingAnalyzer { received += it },
        )

        MultiplexingAnalyzer(analyzers).analyze(recordingProxy.proxy)

        assertEquals(2, received.size)
        assertEquals(0, recordingProxy.closeCount)

        received[0].close()
        assertEquals(0, recordingProxy.closeCount)

        received[1].close()
        assertEquals(1, recordingProxy.closeCount)

        received[1].close()
        assertEquals(1, recordingProxy.closeCount)
    }

    @Test
    fun failingAnalyzerDoesNotBlockRemainingAnalyzersOrLeakTheFrame() {
        val recordingProxy = RecordingImageProxy()
        var remainingAnalyzerCalled = false
        val failingAnalyzer = object : ImageAnalysis.Analyzer {
            override fun analyze(imageProxy: ImageProxy) {
                throw IllegalStateException("synthetic analyzer failure")
            }
        }
        val remainingAnalyzer = recordingAnalyzer {
            remainingAnalyzerCalled = true
            it.close()
        }

        MultiplexingAnalyzer(listOf(failingAnalyzer, remainingAnalyzer))
            .analyze(recordingProxy.proxy)

        assertTrue(remainingAnalyzerCalled)
        assertEquals(1, recordingProxy.closeCount)
    }

    @Test
    fun failingSingleAnalyzerReleasesTheFrame() {
        val recordingProxy = RecordingImageProxy()
        val failingAnalyzer = object : ImageAnalysis.Analyzer {
            override fun analyze(imageProxy: ImageProxy) {
                throw IllegalStateException("synthetic analyzer failure")
            }
        }

        MultiplexingAnalyzer(listOf(failingAnalyzer)).analyze(recordingProxy.proxy)

        assertEquals(1, recordingProxy.closeCount)
    }

    private fun recordingAnalyzer(onAnalyze: (ImageProxy) -> Unit): ImageAnalysis.Analyzer =
        object : ImageAnalysis.Analyzer {
            override fun analyze(imageProxy: ImageProxy) = onAnalyze(imageProxy)
        }

    private class RecordingImageProxy {
        var closeCount: Int = 0
            private set

            val proxy: ImageProxy

        init {
            proxy = Proxy.newProxyInstance(
                ImageProxy::class.java.classLoader,
                arrayOf(ImageProxy::class.java),
            ) { _, method, _ ->
                when (method.name) {
                    "close" -> {
                        closeCount++
                        null
                    }
                    "getFormat" -> ImageFormat.YUV_420_888
                    "getWidth", "getHeight" -> 1
                    "getCropRect" -> null
                    "toString" -> "RecordingImageProxy"
                    else -> defaultValue(method.returnType)
                }
            } as ImageProxy
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
    }
}
