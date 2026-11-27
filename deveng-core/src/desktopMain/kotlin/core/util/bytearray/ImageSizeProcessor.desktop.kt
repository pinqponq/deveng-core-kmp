package core.util.bytearray

import java.awt.Image
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam
import javax.imageio.ImageWriter
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

        val inputStream = ByteArrayInputStream(inputBytes)
        val sourceImage: BufferedImage = ImageIO.read(inputStream) ?: return inputBytes

        val sourceWidth = sourceImage.width
        val sourceHeight = sourceImage.height

        if (sourceWidth <= 0 || sourceHeight <= 0) {
            return inputBytes
        }

        val largestSourceSide = max(sourceWidth, sourceHeight)

        val (targetWidth, targetHeight) = if (largestSourceSide > targetMaxSizePx) {
            val scaleFactor = targetMaxSizePx.toDouble() / largestSourceSide.toDouble()
            (sourceWidth * scaleFactor).roundToInt() to
                    (sourceHeight * scaleFactor).roundToInt()
        } else {
            sourceWidth to sourceHeight
        }

        val finalWidth = max(targetWidth, 1)
        val finalHeight = max(targetHeight, 1)

        val scaledAwtImage: Image = sourceImage.getScaledInstance(
            finalWidth,
            finalHeight,
            Image.SCALE_SMOOTH
        )

        val targetBufferedImage = BufferedImage(
            finalWidth,
            finalHeight,
            BufferedImage.TYPE_INT_RGB
        )
        val graphics = targetBufferedImage.createGraphics()
        graphics.drawImage(scaledAwtImage, 0, 0, null)
        graphics.dispose()

        val outputStream = ByteArrayOutputStream()
        val clampedQuality = quality.coerceIn(1, 100) / 100.0f

        val writers = ImageIO.getImageWritersByFormatName("jpg")
        if (!writers.hasNext()) {
            return inputBytes
        }

        val writer: ImageWriter = writers.next()

        val writeParam: ImageWriteParam = writer.defaultWriteParam.apply {
            if (canWriteCompressed()) {
                compressionMode = ImageWriteParam.MODE_EXPLICIT
                compressionQuality = clampedQuality
            }
        }

        val imageOutputStream = ImageIO.createImageOutputStream(outputStream)
        writer.output = imageOutputStream
        writer.write(null, IIOImage(targetBufferedImage, null, null), writeParam)
        writer.dispose()
        imageOutputStream.close()

        return outputStream.toByteArray()
    }
}