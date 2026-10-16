package core.domain.camera.analysis.plate

import core.domain.camera.analysis.nanodet.model.PlateBoundingBox
import core.domain.camera.analysis.nanodet.model.PlateDetectionResult
import core.domain.camera.analysis.plate.validation.PlateQualityGate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PlateQualityGateTest {
    private val gate = PlateQualityGate(
        detectionConfidenceThreshold = 0.50f,
        minimumBoundingBoxWidthPx = 20f,
        minimumBoundingBoxHeightPx = 8f,
    )

    @Test
    fun acceptsDetectionThatPassesMinimumChecks() {
        val selected = gate.selectAcceptableDetection(
            detection = detection(
                left = 10f,
                top = 10f,
                right = 50f,
                bottom = 24f,
                confidence = 0.92f,
            ),
            frameWidth = 100,
            frameHeight = 60,
        )

        assertEquals(0.92f, selected?.confidence)
    }

    @Test
    fun rejectsLowConfidenceAndOutOfBoundsDetectionsBeforeOcr() {
        val lowConfidence = gate.selectAcceptableDetection(
            detection = detection(
                left = 0f,
                top = 0f,
                right = 40f,
                bottom = 12f,
                confidence = 0.49f,
            ),
            frameWidth = 100,
            frameHeight = 60,
        )
        val outOfBounds = gate.selectAcceptableDetection(
            detection = detection(
                left = 70f,
                top = 10f,
                right = 100f,
                bottom = 20f,
                confidence = 0.99f,
            ),
            frameWidth = 100,
            frameHeight = 60,
        )

        assertNull(lowConfidence)
        assertNull(outOfBounds)
    }

    @Test
    fun rejectsInvalidFrameDimensions() {
        assertNull(
            gate.selectAcceptableDetection(
                detection = null,
                frameWidth = 0,
                frameHeight = 60,
            ),
        )
    }

    private fun detection(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        confidence: Float,
    ): PlateDetectionResult = PlateDetectionResult(
        boundingBox = PlateBoundingBox(
            left = left,
            top = top,
            right = right,
            bottom = bottom,
        ),
        confidence = confidence,
    )
}
