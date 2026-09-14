package core.util.multiplatform

import kotlinx.cinterop.ExperimentalForeignApi
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UISceneActivationStateForegroundActive
import platform.UIKit.UIView
import platform.UIKit.UIViewController
import platform.UIKit.UIWindow
import platform.UIKit.UIWindowLevelNormal
import platform.UIKit.UIWindowScene
import platform.UIKit.popoverPresentationController
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

/**
 * Presents the system share sheet from the foreground window scene.
 *
 * `UIApplication.keyWindow` is deprecated and returns nil in scene based apps, which made the share
 * sheet silently fail to open. The presenting view controller is therefore resolved through
 * `connectedScenes`, and UIKit only allows presenting from the main thread.
 */
internal object IosShareSheetPresenter {

    /**
     * Presents the share sheet from the main queue. Safe to call from any thread.
     *
     * @param activityItems items handed to `UIActivityViewController` (text, file URLs, images).
     */
    fun presentOnMainQueue(activityItems: List<Any?>) {
        dispatch_async(dispatch_get_main_queue()) {
            present(activityItems = activityItems)
        }
    }

    /**
     * Presents the share sheet. Main thread only.
     *
     * @param activityItems items handed to `UIActivityViewController` (text, file URLs, images).
     * @return false when there is no window to present from, so callers can report the failure.
     */
    fun present(activityItems: List<Any?>): Boolean {
        val presentingViewController = findTopViewController() ?: return false
        val activityViewController = UIActivityViewController(
            activityItems = activityItems,
            applicationActivities = null,
        )
        anchorPopoverForIpad(
            activityViewController = activityViewController,
            anchorView = presentingViewController.view,
        )
        presentingViewController.presentViewController(
            activityViewController,
            animated = true,
            completion = null,
        )
        return true
    }

    /**
     * On iPad the share sheet is a popover and UIKit throws when it has no source view. On iPhone
     * the popover controller is ignored, so anchoring it is harmless there.
     */
    @OptIn(ExperimentalForeignApi::class)
    private fun anchorPopoverForIpad(
        activityViewController: UIActivityViewController,
        anchorView: UIView?,
    ) {
        val popoverPresentationController = activityViewController.popoverPresentationController
        if (popoverPresentationController == null || anchorView == null) return

        popoverPresentationController.sourceView = anchorView
        popoverPresentationController.sourceRect = anchorView.bounds
    }

    private fun findTopViewController(): UIViewController? {
        var topViewController = findAppWindow()?.rootViewController ?: return null
        while (true) {
            val presentedViewController = topViewController.presentedViewController
            if (presentedViewController == null || presentedViewController.isBeingDismissed()) break
            topViewController = presentedViewController
        }
        return topViewController
    }

    /**
     * Compose Multiplatform shows dialogs and popups in a separate window above the app
     * (`windowLevel` alert + 1) and makes it the key window while the dialog has focus. That window
     * drops its root view controller as soon as the dialog closes, so a share sheet presented from
     * it disappears with the dialog. Only normal-level windows are considered for that reason.
     */
    private fun findAppWindow(): UIWindow? {
        val windowScenes = UIApplication.sharedApplication.connectedScenes
            .filterIsInstance<UIWindowScene>()
        val windowScene = windowScenes.firstOrNull { windowScene ->
            windowScene.activationState == UISceneActivationStateForegroundActive
        } ?: windowScenes.firstOrNull() ?: return null
        val appWindows = windowScene.windows
            .filterIsInstance<UIWindow>()
            .filter { window ->
                window.windowLevel == UIWindowLevelNormal &&
                    !window.isHidden() &&
                    window.rootViewController != null
            }
        return appWindows.firstOrNull { window -> window.isKeyWindow() } ?: appWindows.firstOrNull()
    }
}
