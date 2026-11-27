package core.domain.camera.ios

import platform.AVFoundation.AVCaptureVideoOrientation
import platform.AVFoundation.AVCaptureVideoOrientationLandscapeLeft
import platform.AVFoundation.AVCaptureVideoOrientationLandscapeRight
import platform.AVFoundation.AVCaptureVideoOrientationPortrait
import platform.AVFoundation.AVCaptureVideoOrientationPortraitUpsideDown
import platform.UIKit.UIApplication
import platform.UIKit.UIInterfaceOrientation
import platform.UIKit.UIInterfaceOrientationLandscapeLeft
import platform.UIKit.UIInterfaceOrientationLandscapeRight
import platform.UIKit.UIInterfaceOrientationPortrait
import platform.UIKit.UIInterfaceOrientationPortraitUpsideDown
import platform.UIKit.UISceneActivationStateForegroundActive
import platform.UIKit.UIView
import platform.UIKit.UIWindowScene

/**
 * Orientation the capture pipeline — preview layer, photo output and movie file output — is
 * configured with.
 *
 * The source of truth is the *interface* orientation, not `UIDevice.currentDevice.orientation`:
 *
 * - Device orientation describes how the phone is held, not how the preview layer is laid out. A
 *   host that pins the camera screen to portrait keeps the layer upright while the device already
 *   reports landscape, so feeding the device orientation into the connection turns the image
 *   sideways inside an upright layer — the preview looks like it rotates on its own.
 * - Device orientation also reports face up, face down and unknown, none of which describe a
 *   capture orientation, so putting the phone down flat snapped the preview back to portrait.
 *
 * Interface orientation has neither problem: it stays portrait while the host locks the screen to
 * portrait and follows the device wherever the host allows rotation. It is also what the still and
 * video outputs need, so saved media matches the framing the preview showed.
 *
 * UIKit may only be read from the main thread while capture connections are also configured off it,
 * so [refresh] resolves and caches on the main thread and [current] serves that value everywhere
 * else.
 */
internal object IosCaptureVideoOrientation {

    private var resolvedOrientation: AVCaptureVideoOrientation = AVCaptureVideoOrientationPortrait

    /** Orientation resolved by the last [refresh]. Safe to read from any queue. */
    val current: AVCaptureVideoOrientation
        get() = resolvedOrientation

    /**
     * Re-reads the interface orientation and caches it. Main thread only.
     *
     * @param view view hosting the preview, used to find the window scene it belongs to; the
     *   foreground scene is used while the view has no window yet.
     * @return the orientation now cached, which is the previously resolved one when the interface
     *   orientation is still unknown (mid launch) — there is nothing better to fall back to.
     */
    fun refresh(view: UIView?): AVCaptureVideoOrientation {
        val orientation = interfaceOrientation(view)?.toCaptureVideoOrientation()
            ?: return resolvedOrientation
        resolvedOrientation = orientation
        return orientation
    }

    private fun interfaceOrientation(view: UIView?): UIInterfaceOrientation? =
        (view?.window?.windowScene ?: foregroundWindowScene())?.interfaceOrientation

    private fun foregroundWindowScene(): UIWindowScene? {
        val windowScenes = UIApplication.sharedApplication.connectedScenes
            .filterIsInstance<UIWindowScene>()
        return windowScenes.firstOrNull {
            it.activationState == UISceneActivationStateForegroundActive
        } ?: windowScenes.firstOrNull()
    }

    private fun UIInterfaceOrientation.toCaptureVideoOrientation(): AVCaptureVideoOrientation? =
        when (this) {
            UIInterfaceOrientationPortrait -> AVCaptureVideoOrientationPortrait
            UIInterfaceOrientationPortraitUpsideDown -> AVCaptureVideoOrientationPortraitUpsideDown
            UIInterfaceOrientationLandscapeLeft -> AVCaptureVideoOrientationLandscapeLeft
            UIInterfaceOrientationLandscapeRight -> AVCaptureVideoOrientationLandscapeRight
            else -> null
        }
}
