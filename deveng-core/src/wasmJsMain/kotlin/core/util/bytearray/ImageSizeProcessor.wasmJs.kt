package core.util.bytearray

import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.FilterMode
import org.jetbrains.skia.FilterMipmap
import org.jetbrains.skia.Image
import org.jetbrains.skia.MipmapMode
import org.jetbrains.skia.Rect
import org.jetbrains.skia.Surface
import kotlin.math.max
import kotlin.math.roundToInt

actual class ImageSizeProcessor {
    actual suspend fun resizeAndCompressBytes(
        inputBytes: ByteArray,
        targetMaxSizePx: Int,
        quality: Int
    ): ByteArray {
        if (inputBytes.isEmpty()) {
            return inputBytes
        }

        val skiaImage = Image.makeFromEncoded(inputBytes)
        val image = skiaImage ?: return inputBytes

        val sourceWidth = image.width.toDouble()
        val sourceHeight = image.height.toDouble()
        
        if (sourceWidth <= 0 || sourceHeight <= 0) {
            return inputBytes
        }

        val largestSourceSide = max(sourceWidth, sourceHeight)

        val (targetWidth, targetHeight) = if (largestSourceSide > targetMaxSizePx) {
            val scaleFactor = targetMaxSizePx.toDouble() / largestSourceSide
            (sourceWidth * scaleFactor).roundToInt() to (sourceHeight * scaleFactor).roundToInt()
        } else {
            sourceWidth.roundToInt() to sourceHeight.roundToInt()
        }

        val finalWidth = max(targetWidth, 1)
        val finalHeight = max(targetHeight, 1)

        val resizedImage = if (finalWidth != sourceWidth.roundToInt() || finalHeight != sourceHeight.roundToInt()) {
            val surface = Surface.makeRasterN32Premul(finalWidth, finalHeight)
            val canvas = surface.canvas
            val samplingMode = FilterMipmap(FilterMode.LINEAR, MipmapMode.NONE)
            canvas.drawImageRect(
                image,
                Rect.makeXYWH(0f, 0f, sourceWidth.toFloat(), sourceHeight.toFloat()),
                Rect.makeXYWH(0f, 0f, finalWidth.toFloat(), finalHeight.toFloat()),
                samplingMode,
                null,
                false
            )
            surface.makeImageSnapshot()
        } else {
            image
        }

        val clampedQuality = quality.coerceIn(1, 100)
        val encodedData = resizedImage.encodeToData(EncodedImageFormat.JPEG, clampedQuality)
        
        return encodedData?.bytes ?: inputBytes
    }
}
