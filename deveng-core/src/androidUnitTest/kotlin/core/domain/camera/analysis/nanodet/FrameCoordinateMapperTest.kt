package core.domain.camera.analysis.nanodet

import core.domain.camera.analysis.nanodet.geometry.FrameCoordinateMapper
import core.domain.camera.analysis.nanodet.geometry.FrameTransform
import core.domain.camera.analysis.nanodet.model.PlateBoundingBox
import core.domain.camera.analysis.nanodet.model.PlateDetectionResult
import kotlin.test.Test
import kotlin.test.assertEquals

class FrameCoordinateMapperTest {
    @Test
    fun directResizeMapsIndependentPortraitAndLandscapeScales() {
        val transform = FrameTransform.directResize(
            sourceWidth = 100,
            sourceHeight = 50,
            inputWidth = 10,
            inputHeight = 10,
            rotationDegrees = 0,
        )

        val mapped = mapBox(
            boundingBox = PlateBoundingBox(
                left = 1f,
                top = 2f,
                right = 8f,
                bottom = 6f,
            ),
            transform = transform,
        )

        assertBox(mapped, left = 10f, top = 10f, right = 80f, bottom = 30f)
    }

    @Test
    fun plateDetectionMappingPreservesConfidenceAndHandlesNull() {
        val transform = FrameTransform.directResize(
            sourceWidth = 100,
            sourceHeight = 50,
            inputWidth = 10,
            inputHeight = 10,
            rotationDegrees = 0,
        )
        val detection = PlateDetectionResult(
            boundingBox = PlateBoundingBox(1f, 2f, 8f, 6f),
            confidence = 0.95f,
        )

        val mapped = FrameCoordinateMapper.map(detection, transform)
        val mappedNull = FrameCoordinateMapper.map(null, transform)

        assertEquals(0.95f, mapped?.confidence)
        assertBox(requireNotNull(mapped?.boundingBox), left = 10f, top = 10f, right = 80f, bottom = 30f)
        assertEquals(null, mappedNull)
    }

    @Test
    fun allSupportedRotationsMapBackToTheSameSourceBox() {
        val expected = PlateBoundingBox(
            left = 10f,
            top = 8f,
            right = 40f,
            bottom = 30f,
        )

        listOf(0, 90, 180, 270).forEach { rotation ->
            val transform = FrameTransform.directResize(
                sourceWidth = 100,
                sourceHeight = 50,
                inputWidth = 10,
                inputHeight = 10,
                rotationDegrees = rotation,
            )
            val rotated = rotateFromSource(expected, transform)
            val detectorBox = PlateBoundingBox(
                left = rotated.left * transform.scaleX,
                top = rotated.top * transform.scaleY,
                right = rotated.right * transform.scaleX,
                bottom = rotated.bottom * transform.scaleY,
            )

            val mapped = mapBox(detectorBox, transform)

            assertBox(
                mapped,
                left = expected.left,
                top = expected.top,
                right = expected.right,
                bottom = expected.bottom,
            )
        }
    }

    @Test
    fun letterboxPaddingIsRemovedBeforeScalingBackToSource() {
        val transform = FrameTransform(
            sourceWidth = 80,
            sourceHeight = 60,
            rotatedWidth = 80,
            rotatedHeight = 60,
            inputWidth = 10,
            inputHeight = 10,
            scaleX = 0.1f,
            scaleY = 0.1f,
            paddingX = 1f,
            paddingY = 2f,
            rotationDegrees = 0,
        )
        val source = PlateBoundingBox(left = 10f, top = 12f, right = 50f, bottom = 42f)
        val detectorBox = PlateBoundingBox(
            left = source.left * transform.scaleX + transform.paddingX,
            top = source.top * transform.scaleY + transform.paddingY,
            right = source.right * transform.scaleX + transform.paddingX,
            bottom = source.bottom * transform.scaleY + transform.paddingY,
        )

        val mapped = mapBox(detectorBox, transform)

        assertBox(mapped, source.left, source.top, source.right, source.bottom)
    }

    @Test
    fun detectorBoundsAreNormalizedAndClamped() {
        val transform = FrameTransform.directResize(
            sourceWidth = 100,
            sourceHeight = 50,
            inputWidth = 10,
            inputHeight = 10,
            rotationDegrees = 0,
        )

        val mapped = mapBox(
            boundingBox = PlateBoundingBox(
                left = 8f,
                top = 9f,
                right = -3f,
                bottom = -4f,
            ),
            transform = transform,
        )

        assertBox(mapped, left = 0f, top = 0f, right = 80f, bottom = 45f)
    }

    private fun rotateFromSource(
        source: PlateBoundingBox,
        transform: FrameTransform,
    ): PlateBoundingBox = when (transform.rotationDegrees) {
        0 -> PlateBoundingBox(source.left, source.top, source.right, source.bottom)
        90 -> PlateBoundingBox(
            left = (transform.sourceHeight - 1).toFloat() - source.bottom,
            top = source.left,
            right = (transform.sourceHeight - 1).toFloat() - source.top,
            bottom = source.right,
        )
        180 -> PlateBoundingBox(
            left = (transform.sourceWidth - 1).toFloat() - source.right,
            top = (transform.sourceHeight - 1).toFloat() - source.bottom,
            right = (transform.sourceWidth - 1).toFloat() - source.left,
            bottom = (transform.sourceHeight - 1).toFloat() - source.top,
        )
        270 -> PlateBoundingBox(
            left = source.top,
            top = (transform.sourceWidth - 1).toFloat() - source.right,
            right = source.bottom,
            bottom = (transform.sourceWidth - 1).toFloat() - source.left,
        )
        else -> error("Rotation was validated by FrameTransform.")
    }

    private fun mapBox(
        boundingBox: PlateBoundingBox,
        transform: FrameTransform,
    ): PlateBoundingBox = requireNotNull(
        FrameCoordinateMapper.map(
            detection = PlateDetectionResult(boundingBox = boundingBox, confidence = 1f),
            transform = transform,
        ),
    ).boundingBox

    private fun assertBox(
        actual: PlateBoundingBox,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
    ) {
        assertEquals(left, actual.left, absoluteTolerance = 0.001f)
        assertEquals(top, actual.top, absoluteTolerance = 0.001f)
        assertEquals(right, actual.right, absoluteTolerance = 0.001f)
        assertEquals(bottom, actual.bottom, absoluteTolerance = 0.001f)
    }
}
