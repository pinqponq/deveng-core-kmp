package core.domain.camera.analysis.nanodet

import kotlin.math.max
import kotlin.math.min

/** Inverts the preprocessing geometry for NanoDet bounding boxes. */
internal object FrameCoordinateMapper {
    fun map(
        detection: PlateDetection,
        transform: FrameTransform,
    ): SourcePlateDetection = SourcePlateDetection(
        boundingBox = map(detection.boundingBox, transform),
        confidence = detection.confidence,
    )

    fun map(
        detections: List<PlateDetection>,
        transform: FrameTransform,
    ): List<SourcePlateDetection> = detections.map { map(it, transform) }

    fun map(
        boundingBox: DetectorBoundingBox,
        transform: FrameTransform,
    ): SourceBoundingBox {
        require(
            listOf(
                boundingBox.left,
                boundingBox.top,
                boundingBox.right,
                boundingBox.bottom,
            ).all(Float::isFinite),
        ) {
            "Detector bounding box must contain finite coordinates."
        }

        val detectorBox = normalizeAndClamp(
            left = boundingBox.left,
            top = boundingBox.top,
            right = boundingBox.right,
            bottom = boundingBox.bottom,
            maxX = (transform.inputWidth - 1).toFloat(),
            maxY = (transform.inputHeight - 1).toFloat(),
        )
        val rotatedBox = normalizeAndClamp(
            left = (detectorBox.left - transform.paddingX) / transform.scaleX,
            top = (detectorBox.top - transform.paddingY) / transform.scaleY,
            right = (detectorBox.right - transform.paddingX) / transform.scaleX,
            bottom = (detectorBox.bottom - transform.paddingY) / transform.scaleY,
            maxX = (transform.rotatedWidth - 1).toFloat(),
            maxY = (transform.rotatedHeight - 1).toFloat(),
        ).let { box ->
            if (!transform.mirrored) {
                box
            } else {
                DetectorBoundingBox(
                    left = (transform.rotatedWidth - 1).toFloat() - box.right,
                    top = box.top,
                    right = (transform.rotatedWidth - 1).toFloat() - box.left,
                    bottom = box.bottom,
                )
            }
        }

        return when (transform.rotationDegrees) {
            0 -> sourceBox(
                left = rotatedBox.left,
                top = rotatedBox.top,
                right = rotatedBox.right,
                bottom = rotatedBox.bottom,
                transform = transform,
            )
            90 -> sourceBox(
                left = rotatedBox.top,
                top = (transform.sourceHeight - 1).toFloat() - rotatedBox.right,
                right = rotatedBox.bottom,
                bottom = (transform.sourceHeight - 1).toFloat() - rotatedBox.left,
                transform = transform,
            )
            180 -> sourceBox(
                left = (transform.sourceWidth - 1).toFloat() - rotatedBox.right,
                top = (transform.sourceHeight - 1).toFloat() - rotatedBox.bottom,
                right = (transform.sourceWidth - 1).toFloat() - rotatedBox.left,
                bottom = (transform.sourceHeight - 1).toFloat() - rotatedBox.top,
                transform = transform,
            )
            270 -> sourceBox(
                left = (transform.sourceWidth - 1).toFloat() - rotatedBox.bottom,
                top = rotatedBox.left,
                right = (transform.sourceWidth - 1).toFloat() - rotatedBox.top,
                bottom = rotatedBox.right,
                transform = transform,
            )
            else -> error("Rotation was validated by FrameTransform.")
        }
    }

    private fun sourceBox(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        transform: FrameTransform,
    ): SourceBoundingBox {
        val normalized = normalizeAndClamp(
            left = left,
            top = top,
            right = right,
            bottom = bottom,
            maxX = (transform.sourceWidth - 1).toFloat(),
            maxY = (transform.sourceHeight - 1).toFloat(),
        )
        return SourceBoundingBox(
            left = normalized.left,
            top = normalized.top,
            right = normalized.right,
            bottom = normalized.bottom,
        )
    }

    private fun normalizeAndClamp(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        maxX: Float,
        maxY: Float,
    ): DetectorBoundingBox {
        val normalizedLeft = min(left, right).coerceIn(0f, maxX)
        val normalizedRight = max(left, right).coerceIn(0f, maxX)
        val normalizedTop = min(top, bottom).coerceIn(0f, maxY)
        val normalizedBottom = max(top, bottom).coerceIn(0f, maxY)
        return DetectorBoundingBox(
            left = min(normalizedLeft, normalizedRight),
            top = min(normalizedTop, normalizedBottom),
            right = max(normalizedLeft, normalizedRight),
            bottom = max(normalizedTop, normalizedBottom),
        )
    }
}
