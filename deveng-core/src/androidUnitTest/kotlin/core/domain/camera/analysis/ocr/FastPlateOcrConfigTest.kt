package core.domain.camera.analysis.ocr

import core.domain.camera.analysis.onnx.OnnxExecutionProvider
import kotlin.test.Test
import kotlin.test.assertEquals

class FastPlateOcrConfigTest {

    @Test
    fun defaultsMatchExpectedConfiguration() {
        val config = FastPlateOcrConfig()

        assertEquals(0.90f, config.confidenceThreshold)
        assertEquals(OnnxExecutionProvider.CPU, config.executionProvider)
        assertEquals(1, config.xnnpackThreads)
        assertEquals(2, config.ortIntraOpThreads)
        assertEquals(1, config.ortInterOpThreads)
    }

    @Test
    fun customParametersAreRetained() {
        val config = FastPlateOcrConfig(
            confidenceThreshold = 0.85f,
            executionProvider = OnnxExecutionProvider.XNNPACK,
            xnnpackThreads = 3,
            ortIntraOpThreads = 4,
            ortInterOpThreads = 2,
        )

        assertEquals(0.85f, config.confidenceThreshold)
        assertEquals(OnnxExecutionProvider.XNNPACK, config.executionProvider)
        assertEquals(3, config.xnnpackThreads)
        assertEquals(4, config.ortIntraOpThreads)
        assertEquals(2, config.ortInterOpThreads)
    }
}
