package core.domain.camera.analysis.nanodet

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

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

        val mapped = FrameCoordinateMapper.map(
            boundingBox = DetectorBoundingBox(
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
    fun allSupportedRotationsMapBackToTheSameSourceBox() {
        val expected = SourceBoundingBox(
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
            val detectorBox = DetectorBoundingBox(
                left = rotated.left * transform.scaleX,
                top = rotated.top * transform.scaleY,
                right = rotated.right * transform.scaleX,
                bottom = rotated.bottom * transform.scaleY,
            )

            val mapped = FrameCoordinateMapper.map(detectorBox, transform)

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
    fun mirroredDetectorCoordinatesAreUnmirroredBeforeRotationMapping() {
        val transform = FrameTransform.directResize(
            sourceWidth = 100,
            sourceHeight = 50,
            inputWidth = 10,
            inputHeight = 10,
            rotationDegrees = 0,
            mirrored = true,
        )
        val expected = SourceBoundingBox(left = 10f, top = 8f, right = 40f, bottom = 30f)
        val mirrored = mirror(rotateFromSource(expected, transform), transform)
        val detectorBox = DetectorBoundingBox(
            left = mirrored.left * transform.scaleX,
            top = mirrored.top * transform.scaleY,
            right = mirrored.right * transform.scaleX,
            bottom = mirrored.bottom * transform.scaleY,
        )

        val mapped = FrameCoordinateMapper.map(detectorBox, transform)

        assertBox(mapped, expected.left, expected.top, expected.right, expected.bottom)
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
        val source = SourceBoundingBox(left = 10f, top = 12f, right = 50f, bottom = 42f)
        val detectorBox = DetectorBoundingBox(
            left = source.left * transform.scaleX + transform.paddingX,
            top = source.top * transform.scaleY + transform.paddingY,
            right = source.right * transform.scaleX + transform.paddingX,
            bottom = source.bottom * transform.scaleY + transform.paddingY,
        )

        val mapped = FrameCoordinateMapper.map(detectorBox, transform)

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

        val mapped = FrameCoordinateMapper.map(
            boundingBox = DetectorBoundingBox(
                left = 8f,
                top = 9f,
                right = -3f,
                bottom = -4f,
            ),
            transform = transform,
        )

        assertBox(mapped, left = 0f, top = 0f, right = 80f, bottom = 45f)
    }

    @Test
    fun invalidTransformGeometryIsRejected() {
        assertFailsWith<IllegalArgumentException> {
            FrameTransform.directResize(
                sourceWidth = 100,
                sourceHeight = 50,
                inputWidth = 10,
                inputHeight = 10,
                rotationDegrees = 45,
            )
        }
        assertFailsWith<IllegalArgumentException> {
            FrameTransform(
                sourceWidth = 100,
                sourceHeight = 50,
                rotatedWidth = 100,
                rotatedHeight = 50,
                inputWidth = 10,
                inputHeight = 10,
                scaleX = 0f,
                scaleY = 1f,
                rotationDegrees = 0,
            )
        }
    }

    private fun rotateFromSource(
        source: SourceBoundingBox,
        transform: FrameTransform,
    ): DetectorBoundingBox = when (transform.rotationDegrees) {
        0 -> DetectorBoundingBox(source.left, source.top, source.right, source.bottom)
        90 -> DetectorBoundingBox(
            left = (transform.sourceHeight - 1).toFloat() - source.bottom,
            top = source.left,
            right = (transform.sourceHeight - 1).toFloat() - source.top,
            bottom = source.right,
        )
        180 -> DetectorBoundingBox(
            left = (transform.sourceWidth - 1).toFloat() - source.right,
            top = (transform.sourceHeight - 1).toFloat() - source.bottom,
            right = (transform.sourceWidth - 1).toFloat() - source.left,
            bottom = (transform.sourceHeight - 1).toFloat() - source.top,
        )
        270 -> DetectorBoundingBox(
            left = source.top,
            top = (transform.sourceWidth - 1).toFloat() - source.right,
            right = source.bottom,
            bottom = (transform.sourceWidth - 1).toFloat() - source.left,
        )
        else -> error("Rotation was validated by FrameTransform.")
    }

    private fun mirror(
        box: DetectorBoundingBox,
        transform: FrameTransform,
    ): DetectorBoundingBox = DetectorBoundingBox(
        left = (transform.rotatedWidth - 1).toFloat() - box.right,
        top = box.top,
        right = (transform.rotatedWidth - 1).toFloat() - box.left,
        bottom = box.bottom,
    )

    private fun assertBox(
        actual: SourceBoundingBox,
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
