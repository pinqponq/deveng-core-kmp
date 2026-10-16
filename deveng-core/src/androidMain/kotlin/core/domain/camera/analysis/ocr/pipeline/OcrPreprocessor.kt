package core.domain.camera.analysis.ocr.pipeline

import core.domain.camera.analysis.AndroidCameraFrame
import core.domain.camera.analysis.nanodet.model.PlateBoundingBox
import core.domain.camera.analysis.ocr.roi.OcrBboxPadding
import core.domain.camera.analysis.ocr.roi.SourceRoiReader
import java.nio.FloatBuffer
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.roundToInt

/** Converts a source-frame ROI into fast-plate-ocr v5's raw RGB float32 tensor. */
internal class OcrPreprocessor(
    private val inputWidth: Int,
    private val inputHeight: Int,
    private val roiReader: SourceRoiReader = SourceRoiReader()
) : AutoCloseable {

    fun preprocessInto(
        frame: AndroidCameraFrame,
        boundingBox: PlateBoundingBox,
        destination: FloatBuffer,
        padding: OcrBboxPadding = OcrBboxPadding()
    ) {
        val roi = roiReader.read(frame, boundingBox, padding)

        resizeRgbLinearInto(
            sourcePixels = roi.pixels,
            sourceWidth = roi.pixelWidth,
            sourceHeight = roi.pixelHeight,
            destination = destination
        )
    }

    /** Releases cached pixel buffers held by the ROI reader to free memory. */
    override fun close() {
        roiReader.releaseBuffers()
    }

    internal fun resizeRgbLinearInto(
        sourcePixels: IntArray,
        sourceWidth: Int,
        sourceHeight: Int,
        destination: FloatBuffer
    ) {
        destination.clear()

        for (destY in 0 until inputHeight) {
            val sourceY = ((destY + 0.5f) * sourceHeight / inputHeight - 0.5f)
                .coerceIn(0f, (sourceHeight - 1).toFloat())
            val top = floor(sourceY).toInt()
            val bottom = min(top + 1, sourceHeight - 1)
            val yFraction = sourceY - top

            val topRow = top * sourceWidth
            val bottomRow = bottom * sourceWidth

            for (destX in 0 until inputWidth) {
                val sourceX = ((destX + 0.5f) * sourceWidth / inputWidth - 0.5f)
                    .coerceIn(0f, (sourceWidth - 1).toFloat())
                val left = floor(sourceX).toInt()
                val right = min(left + 1, sourceWidth - 1)
                val xFraction = sourceX - left

                val topLeft = sourcePixels[topRow + left]
                val topRight = sourcePixels[topRow + right]
                val bottomLeft = sourcePixels[bottomRow + left]
                val bottomRight = sourcePixels[bottomRow + right]

                destination.put(interpolateChannel(topLeft, topRight, bottomLeft, bottomRight, shift = 16, xFraction, yFraction))
                destination.put(interpolateChannel(topLeft, topRight, bottomLeft, bottomRight, shift = 8, xFraction, yFraction))
                destination.put(interpolateChannel(topLeft, topRight, bottomLeft, bottomRight, shift = 0, xFraction, yFraction))
            }
        }

        destination.rewind()
    }

    private fun interpolateChannel(
        topLeft: Int,
        topRight: Int,
        bottomLeft: Int,
        bottomRight: Int,
        shift: Int,
        xFraction: Float,
        yFraction: Float
    ): Float {
        val topLeftChannel = (topLeft ushr shift) and 0xFF
        val topRightChannel = (topRight ushr shift) and 0xFF
        val bottomLeftChannel = (bottomLeft ushr shift) and 0xFF
        val bottomRightChannel = (bottomRight ushr shift) and 0xFF

        val top = topLeftChannel + (topRightChannel - topLeftChannel) * xFraction
        val bottom = bottomLeftChannel + (bottomRightChannel - bottomLeftChannel) * yFraction
        return (top + (bottom - top) * yFraction)
            .roundToInt()
            .coerceIn(0, 255)
            .toFloat()
    }
}