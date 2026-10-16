package core.domain.camera.analysis.ocr.roi

import core.domain.camera.analysis.AndroidCameraFrame
import core.domain.camera.analysis.nanodet.model.PlateBoundingBox
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/** Reads and orients only the mapped source-frame ROI from CameraX's RGBA plane. */
internal class SourceRoiReader {
    private var rgbPixels = IntArray(0)
    private var orientedRgbPixels = IntArray(0)

    fun read(
        frame: AndroidCameraFrame,
        boundingBox: PlateBoundingBox,
        padding: OcrBboxPadding = OcrBboxPadding()
    ): SourceRoi {
        val bounds = resolveBounds(
            boundingBox = boundingBox,
            frameWidth = frame.width,
            frameHeight = frame.height,
            padding = padding
        )

        val plane = frame.planes.single()
        val buffer = plane.buffer.duplicate()
        val startPosition = buffer.position()

        val sourceWidth = bounds.width
        val sourceHeight = bounds.height
        val pixelCount = sourceWidth * sourceHeight
        val destination = getPixelBuffer(pixelCount)
        var destinationIndex = 0

        for (y in bounds.top..bounds.bottom) {
            val rowStart = startPosition + y * plane.rowStride

            for (x in bounds.left..bounds.right) {
                val pixelStart = rowStart + x * plane.pixelStride
                val red = buffer.get(pixelStart).toInt() and 0xFF
                val green = buffer.get(pixelStart + 1).toInt() and 0xFF
                val blue = buffer.get(pixelStart + 2).toInt() and 0xFF
                destination[destinationIndex++] = (red shl 16) or (green shl 8) or blue
            }
        }

        return rotateRoiPixels(
            bounds = bounds,
            sourcePixels = destination,
            rotationDegrees = frame.rotationDegrees
        )
    }

    /** Releases cached pixel buffers to free memory when analysis stops. */
    fun releaseBuffers() {
        rgbPixels = IntArray(0)
        orientedRgbPixels = IntArray(0)
    }

    private fun getPixelBuffer(requiredSize: Int): IntArray {
        if (rgbPixels.size < requiredSize) {
            rgbPixels = IntArray(requiredSize)
        }
        return rgbPixels
    }

    private fun getOrientedPixelBuffer(requiredSize: Int): IntArray {
        if (orientedRgbPixels.size < requiredSize) {
            orientedRgbPixels = IntArray(requiredSize)
        }
        return orientedRgbPixels
    }

    private fun rotateRoiPixels(
        bounds: SourceRoiBounds,
        sourcePixels: IntArray,
        rotationDegrees: Int
    ): SourceRoi {
        val sourceWidth = bounds.width
        val sourceHeight = bounds.height

        if (rotationDegrees == 0) {
            return SourceRoi(
                bounds = bounds,
                pixels = sourcePixels,
                pixelWidth = sourceWidth,
                pixelHeight = sourceHeight
            )
        }

        val orientedWidth = if (rotationDegrees == 90 || rotationDegrees == 270) {
            sourceHeight
        } else {
            sourceWidth
        }

        val orientedHeight = if (rotationDegrees == 90 || rotationDegrees == 270) {
            sourceWidth
        } else {
            sourceHeight
        }

        val orientedPixels = getOrientedPixelBuffer(sourcePixels.size)

        for (sourceY in 0 until sourceHeight) {
            for (sourceX in 0 until sourceWidth) {
                val (orientedX, orientedY) = when (rotationDegrees) {
                    90 -> (sourceHeight - 1 - sourceY) to sourceX
                    180 -> (sourceWidth - 1 - sourceX) to (sourceHeight - 1 - sourceY)
                    270 -> sourceY to (sourceWidth - 1 - sourceX)
                    else -> error("Unsupported frame rotation: $rotationDegrees")
                }
                orientedPixels[orientedY * orientedWidth + orientedX] =
                    sourcePixels[sourceY * sourceWidth + sourceX]
            }
        }

        return SourceRoi(
            bounds = bounds,
            pixels = orientedPixels,
            pixelWidth = orientedWidth,
            pixelHeight = orientedHeight
        )
    }

    private fun resolveBounds(
        boundingBox: PlateBoundingBox,
        frameWidth: Int,
        frameHeight: Int,
        padding: OcrBboxPadding
    ): SourceRoiBounds {
        val left = min(boundingBox.left, boundingBox.right)
        val right = max(boundingBox.left, boundingBox.right)
        val top = min(boundingBox.top, boundingBox.bottom)
        val bottom = max(boundingBox.top, boundingBox.bottom)

        val horizontalPadding = (right - left + 1f) * padding.horizontalRatio
        val verticalPadding = (bottom - top + 1f) * padding.verticalRatio
        val maxX = (frameWidth - 1).toFloat()
        val maxY = (frameHeight - 1).toFloat()

        val paddedLeft = floor(left - horizontalPadding).coerceIn(0f, maxX).toInt()
        val paddedTop = floor(top - verticalPadding).coerceIn(0f, maxY).toInt()
        val paddedRight = ceil(right + horizontalPadding).coerceIn(0f, maxX).toInt()
        val paddedBottom = ceil(bottom + verticalPadding).coerceIn(0f, maxY).toInt()

        return SourceRoiBounds(
            left = paddedLeft,
            top = paddedTop,
            right = paddedRight,
            bottom = paddedBottom
        )
    }
}