package core.domain.camera.utils

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.get
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.useContents
import kotlinx.cinterop.usePinned
import org.jetbrains.skia.Data
import org.jetbrains.skia.Image
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSData
import platform.Foundation.create
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.UIKit.UIGraphicsBeginImageContextWithOptions
import platform.UIKit.UIGraphicsEndImageContext
import platform.UIKit.UIGraphicsGetImageFromCurrentImageContext
import platform.posix.memcpy
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlinx.cinterop.IntVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import core.util.bytearray.toImageBitmap
import platform.CoreFoundation.CFDataCreate
import platform.CoreFoundation.CFDataRef
import platform.CoreFoundation.CFDictionaryCreateMutable
import platform.CoreFoundation.CFDictionarySetValue
import platform.CoreFoundation.CFNumberCreate
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.kCFAllocatorDefault
import platform.CoreFoundation.kCFBooleanTrue
import platform.CoreFoundation.kCFNumberIntType
import platform.ImageIO.CGImageSourceCreateThumbnailAtIndex
import platform.ImageIO.CGImageSourceCreateWithData
import platform.ImageIO.kCGImageSourceCreateThumbnailFromImageAlways
import platform.ImageIO.kCGImageSourceCreateThumbnailWithTransform
import platform.ImageIO.kCGImageSourceThumbnailMaxPixelSize

fun ImageBitmap.toByteArray(): ByteArray? {
    val skiaBitmap = this.asSkiaBitmap()
    val skiaImage: Image = Image.makeFromBitmap(skiaBitmap)

    val encodedData: Data? = skiaImage.encodeToData(quality = 100)
    return encodedData?.bytes
}

/**
 * Copies the bytes into an [NSData] with a single bulk copy.
 *
 * Must not use `allocArrayOf`: Kotlin/Native fills that array element-by-element, which costs
 * ~550 ms for a multi-megabyte JPEG (measured, iPhone 11) and dominated both the GPS-EXIF write
 * and the temp-photo save on the camera capture path.
 */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
fun ByteArray.toNSData(): NSData {
    if (isEmpty()) {
        return NSData.create(bytes = null, length = 0uL)
    }

    return usePinned { pinned ->
        NSData.create(bytes = pinned.addressOf(0), length = size.toULong())
    }
}

/**
 * Converts NSData to ByteArray with optional buffer reuse for better memory efficiency
 *
 * @param reuseBuffer Optional pre-allocated buffer to use if large enough
 * @return ByteArray containing the data
 */
@OptIn(ExperimentalForeignApi::class)
fun NSData.toByteArray(reuseBuffer: ByteArray? = null): ByteArray {
    val length = this.length.toInt()
    if (length == 0) return ByteArray(0)

    val buffer =
        if (reuseBuffer != null && reuseBuffer.size >= length) {
            reuseBuffer
        } else {
            ByteArray(length)
        }

    buffer.usePinned {
        memcpy(it.addressOf(0), this@toByteArray.bytes, this@toByteArray.length)
    }

    return buffer
}

@OptIn(ExperimentalForeignApi::class)
fun NSData.toByteArray(): ByteArray = toByteArray(null)

fun NSData.toUIImage() = UIImage(this)

/**
 * If the decoded JPEG exceeds [capWidth]×[capHeight] when measured as (min side, max side),
 * downscales (aspect preserved) so both sides fit within that box and re-encodes as JPEG.
 * Matches Android still-capture max short/long semantics for [Pair] caps like 2160×3840.
 */
@OptIn(ExperimentalForeignApi::class)
fun capNSDataJpegToMaxPhotoDimensions(data: NSData, capWidth: Int, capHeight: Int): NSData {
    if (capWidth <= 0 || capHeight <= 0) return data
    val shortCap = min(capWidth, capHeight)
    val longCap = max(capWidth, capHeight)
    val uiImage = data.toUIImage()
    // Use oriented display pixel size, not CGImageGetWidth/Height. Raw CGImage dimensions ignore
    // UIImage.imageOrientation; drawInRect applies orientation when drawing, so a target rect sized
    // from CGImage would mismatch the drawn aspect and stretch/squash (e.g. portrait photo in a
    // landscape-sized rect on review).
    val pixelW = (uiImage.size.useContents { width } * uiImage.scale).roundToInt().coerceAtLeast(1)
    val pixelH = (uiImage.size.useContents { height } * uiImage.scale).roundToInt().coerceAtLeast(1)
    val shortSide = min(pixelW, pixelH)
    val longSide = max(pixelW, pixelH)
    if (shortSide <= shortCap && longSide <= longCap) {
        return data
    }
    val scale = min(shortCap.toFloat() / shortSide, longCap.toFloat() / longSide).coerceAtMost(1f)
    val newW = max(1, (pixelW * scale).roundToInt())
    val newH = max(1, (pixelH * scale).roundToInt())
    UIGraphicsBeginImageContextWithOptions(CGSizeMake(newW.toDouble(), newH.toDouble()), false, 1.0)
    try {
        uiImage.drawInRect(CGRectMake(0.0, 0.0, newW.toDouble(), newH.toDouble()))
        val resized = UIGraphicsGetImageFromCurrentImageContext() ?: return data
        return UIImageJPEGRepresentation(resized, 0.92) ?: data
    } finally {
        UIGraphicsEndImageContext()
    }
}

/** Max pixel size for the capture-result corner preview; a crisp retina preview needs only a few hundred px. */
private const val CAPTURE_PREVIEW_MAX_PIXEL = 512

/**
 * Fast preview decode for the capture-result corner preview: decodes the JPEG **directly at a
 * reduced size** via ImageIO instead of decoding the full multi-megapixel image and throwing it away
 * (the full Skia decode was ~1–2s on older devices). Falls back to the full decode on any failure so
 * a preview is always produced.
 */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
fun ByteArray.toCapturePreviewImageBitmap(): ImageBitmap? {
    val downsized = runCatching { decodeDownsampledPreviewBitmap(CAPTURE_PREVIEW_MAX_PIXEL) }.getOrNull()
    return downsized ?: runCatching { toImageBitmap() }.getOrNull()
}

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private fun ByteArray.decodeDownsampledPreviewBitmap(maxPixelSize: Int): ImageBitmap? {
    if (isEmpty()) return null
    val cfData: CFDataRef = usePinned { pinned ->
        CFDataCreate(kCFAllocatorDefault, pinned.addressOf(0).reinterpret(), size.convert())
    } ?: return null
    try {
        val source = CGImageSourceCreateWithData(cfData, null) ?: return null
        try {
            val options = CFDictionaryCreateMutable(kCFAllocatorDefault, 0.convert(), null, null) ?: return null
            try {
                CFDictionarySetValue(options, kCGImageSourceCreateThumbnailFromImageAlways, kCFBooleanTrue)
                CFDictionarySetValue(options, kCGImageSourceCreateThumbnailWithTransform, kCFBooleanTrue)
                val cfMax = memScoped {
                    val holder = alloc<IntVar>()
                    holder.value = maxPixelSize
                    CFNumberCreate(kCFAllocatorDefault, kCFNumberIntType, holder.ptr)
                } ?: return null
                try {
                    CFDictionarySetValue(options, kCGImageSourceThumbnailMaxPixelSize, cfMax)
                    val cgPreview = CGImageSourceCreateThumbnailAtIndex(source, 0u, options) ?: return null
                    val uiImage = UIImage.imageWithCGImage(cgPreview) ?: return null
                    val jpeg = UIImageJPEGRepresentation(uiImage, 0.9) ?: return null
                    return jpeg.toByteArray().toImageBitmap()
                } finally {
                    CFRelease(cfMax)
                }
            } finally {
                CFRelease(options)
            }
        } finally {
            CFRelease(source)
        }
    } finally {
        CFRelease(cfData)
    }
}

/**
 * Redraws the UIImage with orientation transformations applied to the pixel data.
 * Fixes issues where EXIF orientation metadata doesn't match the actual pixels,
 * which can cause rotated images when re-encoded to JPEG/PNG.
 *
 * @return UIImage with orientation baked into pixels
 */
@OptIn(ExperimentalForeignApi::class)
fun UIImage.fixOrientation(): UIImage {
    // If image is already in correct orientation, return it as-is
    if (this.imageOrientation == platform.UIKit.UIImageOrientation.UIImageOrientationUp) {
        return this
    }

    // Get the actual display size (after orientation transform is applied)
    val width = this.size.useContents { this.width }
    val height = this.size.useContents { this.height }

    // Create a graphics context with the display size and draw the image
    // UIImage.drawInRect automatically applies the orientation transformation
    platform.UIKit.UIGraphicsBeginImageContextWithOptions(this.size, false, this.scale)
    this.drawInRect(platform.CoreGraphics.CGRectMake(0.0, 0.0, width, height))
    val normalizedImage = platform.UIKit.UIGraphicsGetImageFromCurrentImageContext()
    platform.UIKit.UIGraphicsEndImageContext()

    return normalizedImage ?: this
}

@OptIn(ExperimentalForeignApi::class)
fun UIImage.toByteArray(): ByteArray = run {
    val imageData = UIImageJPEGRepresentation(this, 1.0)
        ?: throw IllegalArgumentException("image data is null")
    val bytes = imageData.bytes ?: throw IllegalArgumentException("image bytes is null")
    val length = imageData.length

    val data: CPointer<ByteVar> = bytes.reinterpret()
    ByteArray(length.toInt()) { index -> data[index] }
}
