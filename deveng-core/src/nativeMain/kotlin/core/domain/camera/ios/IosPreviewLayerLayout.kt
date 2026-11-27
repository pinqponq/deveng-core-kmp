package core.domain.camera.ios

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.AVFoundation.AVCaptureVideoPreviewLayer
import platform.CoreGraphics.CGRectMake
import platform.UIKit.UIScreen
import platform.UIKit.UIView

/**
 * During bottom-nav transitions, Compose/UIKit can lay out the camera [UIView] wider than the
 * screen. [AVCaptureVideoPreviewLayer] with [AVLayerVideoGravityResizeAspectFill] on an oversized
 * frame looks zoomed-in even when [AVCaptureDevice.videoZoomFactor] is 1.0.
 */
@OptIn(ExperimentalForeignApi::class)
internal data class IosPreviewLayerFrame(
    val width: Double,
    val height: Double,
    val clampedWidth: Boolean,
) {
    val key: String get() = "${width.toInt()}x${height.toInt()}"
}

@OptIn(ExperimentalForeignApi::class)
internal fun clampedPreviewLayerFrameForView(view: UIView): IosPreviewLayerFrame {
    val screenW = UIScreen.mainScreen.bounds.useContents { size.width }
    val screenH = UIScreen.mainScreen.bounds.useContents { size.height }
    val (vw, vh) = view.bounds.useContents { size.width to size.height }
    val clampedWidth = vw > screenW + 1.0
    val width = if (clampedWidth) screenW else vw
    val height = vh.coerceIn(1.0, screenH)
    return IosPreviewLayerFrame(
        width = width,
        height = height,
        clampedWidth = clampedWidth,
    )
}

@OptIn(ExperimentalForeignApi::class)
internal fun applyClampedPreviewLayerFrame(view: UIView, layer: AVCaptureVideoPreviewLayer) {
    val frame = clampedPreviewLayerFrameForView(view)
    layer.setFrame(CGRectMake(0.0, 0.0, frame.width, frame.height))
    layer.contentsScale = UIScreen.mainScreen.scale
}
