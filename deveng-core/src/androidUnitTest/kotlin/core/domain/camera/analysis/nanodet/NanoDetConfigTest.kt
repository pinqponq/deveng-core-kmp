package core.domain.camera.analysis.nanodet

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class NanoDetConfigTest {
    @Test
    fun defaultsMatchReferenceDetector() {
        val config = NanoDetConfig()

        assertEquals(0.50f, config.confidenceThreshold)
        assertEquals(0.60f, config.nmsIouThreshold)
        assertEquals(NanoDetExecutionProvider.XNNPACK, config.executionProvider)
    }

    @Test
    fun thresholdsOutsideProbabilityRangeAreRejected() {
        assertFailsWith<IllegalArgumentException> {
            NanoDetConfig(confidenceThreshold = -0.01f)
        }
        assertFailsWith<IllegalArgumentException> {
            NanoDetConfig(nmsIouThreshold = 1.01f)
        }
    }
}
