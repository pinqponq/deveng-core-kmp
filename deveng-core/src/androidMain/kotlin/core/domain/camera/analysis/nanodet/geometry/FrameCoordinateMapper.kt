package core.domain.camera.analysis.nanodet.geometry

import core.domain.camera.analysis.nanodet.model.PlateBoundingBox
import core.domain.camera.analysis.nanodet.model.PlateDetectionResult
import kotlin.math.max
import kotlin.math.min

/**
 * Bu dosyanın amacı, modele giren resmin boyutlarını orijinal hale getirmektir.
 * Örneğin AxB çözünürlüğündeki kameradan gelen veri modele CxD boyutlarına indirgenerek verilir.
 * OCR modeline ise AxB çözünürlüğündeki resim üzerinden plaka görseli verilir.
 * Bu Mapper'ın amacı da modelin CxD boyutlarına göre verdiği sonucu, AxB çözünürlüğüne göre maplemektir.
 */
internal object FrameCoordinateMapper {

    fun map(
        detection: PlateDetectionResult?,
        transform: FrameTransform,
    ): PlateDetectionResult? {
        if (detection == null) return null
        return PlateDetectionResult(
            boundingBox = mapBoundingBox(detection.boundingBox, transform),
            confidence = detection.confidence
        )
    }

    private fun mapBoundingBox(
        boundingBox: PlateBoundingBox,
        transform: FrameTransform,
    ): PlateBoundingBox {
        val detectorBox = normalizeAndClamp(
            left = boundingBox.left,
            top = boundingBox.top,
            right = boundingBox.right,
            bottom = boundingBox.bottom,
            maxX = (transform.inputWidth - 1).toFloat(),
            maxY = (transform.inputHeight - 1).toFloat()
        )
        val rotatedBox = normalizeAndClamp(
            left = (detectorBox.left - transform.paddingX) / transform.scaleX,
            top = (detectorBox.top - transform.paddingY) / transform.scaleY,
            right = (detectorBox.right - transform.paddingX) / transform.scaleX,
            bottom = (detectorBox.bottom - transform.paddingY) / transform.scaleY,
            maxX = (transform.rotatedWidth - 1).toFloat(),
            maxY = (transform.rotatedHeight - 1).toFloat()
        )

        return when (transform.rotationDegrees) {
            0 -> sourceBox(
                left = rotatedBox.left,
                top = rotatedBox.top,
                right = rotatedBox.right,
                bottom = rotatedBox.bottom,
                transform = transform
            )
            90 -> sourceBox(
                left = rotatedBox.top,
                top = (transform.sourceHeight - 1).toFloat() - rotatedBox.right,
                right = rotatedBox.bottom,
                bottom = (transform.sourceHeight - 1).toFloat() - rotatedBox.left,
                transform = transform
            )
            180 -> sourceBox(
                left = (transform.sourceWidth - 1).toFloat() - rotatedBox.right,
                top = (transform.sourceHeight - 1).toFloat() - rotatedBox.bottom,
                right = (transform.sourceWidth - 1).toFloat() - rotatedBox.left,
                bottom = (transform.sourceHeight - 1).toFloat() - rotatedBox.top,
                transform = transform
            )
            270 -> sourceBox(
                left = (transform.sourceWidth - 1).toFloat() - rotatedBox.bottom,
                top = rotatedBox.left,
                right = (transform.sourceWidth - 1).toFloat() - rotatedBox.top,
                bottom = rotatedBox.right,
                transform = transform
            )
            else -> error("Rotation was validated by FrameTransform.")
        }
    }

    private fun sourceBox(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        transform: FrameTransform
    ): PlateBoundingBox {
        val normalized = normalizeAndClamp(
            left = left,
            top = top,
            right = right,
            bottom = bottom,
            maxX = (transform.sourceWidth - 1).toFloat(),
            maxY = (transform.sourceHeight - 1).toFloat()
        )
        return PlateBoundingBox(
            left = normalized.left,
            top = normalized.top,
            right = normalized.right,
            bottom = normalized.bottom
        )
    }

    private fun normalizeAndClamp(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        maxX: Float,
        maxY: Float
    ): PlateBoundingBox {
        val normalizedLeft = min(left, right).coerceIn(0f, maxX)
        val normalizedRight = max(left, right).coerceIn(0f, maxX)
        val normalizedTop = min(top, bottom).coerceIn(0f, maxY)
        val normalizedBottom = max(top, bottom).coerceIn(0f, maxY)
        return PlateBoundingBox(
            left = min(normalizedLeft, normalizedRight),
            top = min(normalizedTop, normalizedBottom),
            right = max(normalizedLeft, normalizedRight),
            bottom = max(normalizedTop, normalizedBottom)
        )
    }
}