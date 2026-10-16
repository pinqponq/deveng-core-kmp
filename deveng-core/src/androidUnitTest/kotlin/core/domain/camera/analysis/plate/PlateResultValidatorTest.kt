package core.domain.camera.analysis.plate

import core.domain.camera.analysis.ocr.model.FastPlateOcrResult
import core.domain.camera.analysis.ocr.roi.CapturedCameraFrame
import core.domain.camera.analysis.plate.validation.PlateResultValidator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PlateResultValidatorTest {
    @Test
    fun emitsResultForTwoMatchingReadsWithinThreeValidReads() {
        val validator = validator()

        assertNull(validator.validate(read("34ABC123", 0.95f)))
        assertNull(validator.validate(read("34ABC124", 0.90f)))
        val result = validator.validate(read("34ABC123", 0.97f))

        assertEquals("34ABC123", result?.plate)
        assertEquals(0.96f, result?.confidence ?: 0f, absoluteTolerance = 0.0001f)
        assertNull(validator.validate(read("34ABC123", 0.95f)))
    }

    @Test
    fun rollsTheWindowUntilAConsensusIsFound() {
        val validator = validator()

        assertNull(validator.validate(read("34ABC123", 0.91f)))
        assertNull(validator.validate(read("34ABC124", 0.92f)))
        assertNull(validator.validate(read("34ABC125", 0.93f)))
        assertEquals(
            PlateRecognitionResult(
                plate = "34ABC124",
                confidence = 0.93f,
            ),
            validator.validate(read("34ABC124", 0.94f)),
        )
    }

    @Test
    fun lowConfidenceReadDoesNotEnterOrResetTheWindow() {
        val validator = validator()

        assertNull(validator.validate(read("34ABC123", 0.95f)))
        assertNull(validator.validate(read("34ABC124", 0.93f)))
        assertNull(validator.validate(read("99LOW999", 0.72f)))
        assertEquals(
            "34ABC123",
            validator.validate(read("34ABC123", 0.97f))?.plate,
        )
    }

    @Test
    fun blankAndNonFiniteReadsDoNotEnterOrResetTheWindow() {
        val validator = validator()

        assertNull(validator.validate(read("34ABC123", 0.95f)))
        assertNull(validator.validate(read("   ", 0.99f)))
        assertNull(validator.validate(read("34ABC123", Float.NaN)))
        assertNull(validator.validate(read("34ABC124", 0.93f)))
        assertEquals(
            "34ABC123",
            validator.validate(read("34ABC123", 0.97f))?.plate,
        )
    }

    @Test
    fun resetUnlocksValidatorForANewCameraSession() {
        val validator = PlateResultValidator(
            requiredMatchingReads = 1,
            validationWindowSize = 1,
            confidenceThreshold = 0.90f,
        )

        assertEquals("34ABC123", validator.validate(read("34ABC123", 0.95f))?.plate)
        assertNull(validator.validate(read("35XYZ789", 0.95f)))

        validator.reset()

        assertEquals("35XYZ789", validator.validate(read("35XYZ789", 0.95f))?.plate)
    }

    @Test
    fun emitsResultForTwoMatchingReadsWithinTwoReads() {
        val validator = PlateResultValidator(
            requiredMatchingReads = 2,
            validationWindowSize = 2,
            confidenceThreshold = 0.90f,
        )

        assertNull(validator.validate(read("34ABC123", 0.95f)))
        assertEquals("34ABC123", validator.validate(read("34ABC123", 0.97f))?.plate)
    }

    @Test
    fun selectsTheHighestConfidenceFrameAmongReadsSupportingConsensus() {
        val validator = validator()
        val olderSupportingFrame = frame(timestampNanos = 10L)
        val newerSupportingFrame = frame(timestampNanos = 30L)

        assertNull(validator.validate(read("34ABC123", 0.95f, olderSupportingFrame)))
        assertNull(validator.validate(read("34ABC124", 0.99f, frame(timestampNanos = 20L))))
        val result = validator.validate(read("34ABC123", 0.97f, newerSupportingFrame))

        assertEquals(newerSupportingFrame, result?.fullFrame)
    }

    @Test
    fun selectsTheNewestFrameWhenSupportingReadsHaveEqualConfidence() {
        val validator = validator()
        val olderSupportingFrame = frame(timestampNanos = 10L)
        val newerSupportingFrame = frame(timestampNanos = 30L)

        assertNull(validator.validate(read("34ABC123", 0.97f, olderSupportingFrame)))
        assertNull(validator.validate(read("34ABC124", 0.99f, frame(timestampNanos = 20L))))
        val result = validator.validate(read("34ABC123", 0.97f, newerSupportingFrame))

        assertEquals(newerSupportingFrame, result?.fullFrame)
    }

    private fun validator(): PlateResultValidator = PlateResultValidator(
        requiredMatchingReads = 2,
        validationWindowSize = 3,
        confidenceThreshold = 0.90f,
    )

    private fun read(
        text: String,
        confidence: Float = 0.95f,
        fullFrame: CapturedCameraFrame? = null,
    ): FastPlateOcrResult = FastPlateOcrResult(
        plateText = text,
        characterConfidences = emptyList(),
        confidence = confidence,
        fullFrame = fullFrame,
    )

    private fun frame(timestampNanos: Long): CapturedCameraFrame =
        CapturedCameraFrame(
            packedRgba = byteArrayOf(0, 0, 0, 0),
            sourceWidth = 1,
            sourceHeight = 1,
            rotationDegrees = 0,
            timestampNanos = timestampNanos,
        )
}
