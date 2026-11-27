package core.domain.camera.ios

import androidx.compose.ui.graphics.ImageBitmap
import core.domain.camera.utils.fixOrientation
import core.domain.camera.utils.toByteArray
import core.util.bytearray.toImageBitmap
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import platform.AVFoundation.AVAssetImageGenerator
import platform.AVFoundation.AVURLAsset
import platform.CoreGraphics.CGSizeMake
import platform.CoreMedia.CMTimeMake
import platform.Foundation.NSLog
import platform.Foundation.NSURL
import platform.UIKit.UIImage

/**
 * First-frame thumbnail from a finished recording file — avoids a live [capturePhoto] before/during video.
 */
@OptIn(ExperimentalForeignApi::class)
internal fun extractVideoThumbnailFromFile(
    filePath: String,
    compensateMirroredRecording: Boolean = false,
): ImageBitmap? {
    if (filePath.isEmpty()) return null
    val url = NSURL.fileURLWithPath(filePath)
    val asset = AVURLAsset(uRL = url, options = null)
    val generator = AVAssetImageGenerator(asset).apply {
        appliesPreferredTrackTransform = true
        maximumSize = CGSizeMake(640.0, 640.0)
    }
    return memScoped {
        val time = CMTimeMake(0, 1)
        val actualTime = alloc<platform.CoreMedia.CMTime>()
        val cgImage = generator.copyCGImageAtTime(
            requestedTime = time,
            actualTime = actualTime.ptr,
            error = null,
        ) ?: run {
            NSLog("CameraK: video thumbnail extract failed for path=$filePath")
            return null
        }
        val uiImage = UIImage.imageWithCGImage(cgImage) ?: return null
        try {
            var bitmap = uiImage.fixOrientation().toByteArray().toImageBitmap()
            if (compensateMirroredRecording) {
                bitmap = mirrorImageBitmapHorizontally(bitmap)
            }
            bitmap
        } catch (e: Exception) {
            NSLog("CameraK: video thumbnail decode failed: ${e.message}")
            null
        }
    }
}
