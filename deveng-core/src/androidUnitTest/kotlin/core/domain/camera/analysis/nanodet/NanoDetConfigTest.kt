package core.domain.camera.analysis.nanodet

import core.domain.camera.analysis.nanodet.model.NanoDetModelContract
import core.domain.camera.analysis.onnx.OnnxExecutionProvider
import kotlin.test.Test
import kotlin.test.assertEquals

class NanoDetConfigTest {
    @Test
    fun androidReferenceContractIsExplicit() {
        assertEquals(33, NanoDetModelContract.OUTPUT_CHANNELS)
        assertEquals(7, NanoDetModelContract.REG_MAX)
        assertEquals(listOf(8, 16, 32, 64), NanoDetModelContract.strides)
    }

    @Test
    fun defaultsMatchReferenceDetector() {
        val config = NanoDetConfig()

        assertEquals(0.50f, config.confidenceThreshold)
        assertEquals(OnnxExecutionProvider.XNNPACK, config.executionProvider)
        assertEquals(3, config.xnnpackThreads)
    }
}
