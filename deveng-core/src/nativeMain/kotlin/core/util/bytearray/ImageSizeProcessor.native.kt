package core.util.bytearray

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.get
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.useContents
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSData
import platform.Foundation.create
import platform.UIKit.UIGraphicsBeginImageContextWithOptions
import platform.UIKit.UIGraphicsEndImageContext
import platform.UIKit.UIGraphicsGetImageFromCurrentImageContext
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import kotlin.math.max
import kotlin.math.roundToInt


actual class ImageSizeProcessor {
    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
    actual suspend fun resizeAndCompressBytes(
        inputBytes: ByteArray,
        targetMaxSizePx: Int,
        quality: Int
    ): ByteArray {
        if (inputBytes.isEmpty()) return inputBytes

        return withContext(Dispatchers.Main) {
            val nsData: NSData = inputBytes.usePinned { pinned ->
                NSData.create(
                    bytes = pinned.addressOf(0),
                    length = inputBytes.size.toULong()
                )
            }

            val sourceImage: UIImage = UIImage(data = nsData) ?: return@withContext inputBytes

            val sourceSize = sourceImage.size
            var sourceWidth = 0.0
            var sourceHeight = 0.0
            sourceSize.useContents {
                sourceWidth = width
                sourceHeight = height
            }

            if (sourceWidth <= 0.0 || sourceHeight <= 0.0) {
                return@withContext inputBytes
            }

            val largestSide = max(sourceWidth, sourceHeight)
            val (targetWidth, targetHeight) =
                if (largestSide > targetMaxSizePx) {
                    val scaleFactor = targetMaxSizePx.toDouble() / largestSide
                    (sourceWidth * scaleFactor).roundToInt() to
                            (sourceHeight * scaleFactor).roundToInt()
                } else {
                    sourceWidth.roundToInt() to sourceHeight.roundToInt()
                }

            val finalWidth = max(targetWidth, 1)
            val finalHeight = max(targetHeight, 1)

            val targetSize = CGSizeMake(
                finalWidth.toDouble(),
                finalHeight.toDouble()
            )

            UIGraphicsBeginImageContextWithOptions(targetSize, false, 1.0)
            sourceImage.drawInRect(
                CGRectMake(
                    0.0,
                    0.0,
                    finalWidth.toDouble(),
                    finalHeight.toDouble()
                )
            )
            val resizedImage = UIGraphicsGetImageFromCurrentImageContext()
            UIGraphicsEndImageContext()

            val resizedImageNonNull = resizedImage ?: return@withContext inputBytes

            val clampedQuality = quality.coerceIn(1, 100) / 100.0
            val jpegData = UIImageJPEGRepresentation(resizedImageNonNull, clampedQuality)
                ?: return@withContext inputBytes

            val length = jpegData.length.toInt()
            if (length <= 0) return@withContext inputBytes

            val bytes = jpegData.bytes ?: return@withContext inputBytes
            val pointer: CPointer<ByteVar> = bytes.reinterpret()

            ByteArray(length) { idx -> pointer[idx] }
        }
    }
}