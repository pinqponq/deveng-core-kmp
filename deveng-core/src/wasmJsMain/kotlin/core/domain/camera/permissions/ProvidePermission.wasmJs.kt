package core.domain.camera.permissions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * WASM/JS permission shim.
 *
 * On the web there is no standalone "request permission" step — the browser prompts the
 * user at the moment `navigator.mediaDevices.getUserMedia` is called. So this
 * implementation is deliberately optimistic:
 *
 * - [hasCameraPermission] returns `true`; callers shouldn't pre-block the UI.
 * - [RequestCameraPermission] immediately calls [onGranted]; the actual browser prompt
 *   fires later when the camera session starts. If the user denies, the underlying
 *   `getUserMedia` promise rejection surfaces the failure.
 *
 * Storage permission stays `false` / `onDenied` because there is no equivalent browser
 * API that maps cleanly to the Android/iOS concept of "storage permission granted".
 */
@Composable
actual fun providePermissions(): Permissions = remember {
    object : Permissions {
        override fun hasCameraPermission(): Boolean = true
        override fun hasStoragePermission(): Boolean = false

        @Composable
        override fun RequestCameraPermission(onGranted: () -> Unit, onDenied: () -> Unit) {
            onGranted()
        }

        @Composable
        override fun RequestStoragePermission(onGranted: () -> Unit, onDenied: () -> Unit) {
            onDenied()
        }
    }
}
