package core.domain.camera.scanner

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import core.domain.camera.compose.CameraKScreen
import core.domain.camera.compose.rememberCameraKState
import core.domain.camera.enums.CameraLens
import core.domain.camera.state.CameraConfiguration
import core.domain.camera.state.CameraKEvent
import core.domain.camera.state.CameraKStateHolder
import kotlinx.coroutines.flow.filterIsInstance

/**
 * Convenience composable that wires a live camera, attaches a [QrScannerPlugin], and
 * invokes [onQrScanned] on every scan that passes the plugin's throttling rules.
 *
 * This is the simplest way to embed QR scanning:
 *
 * ```kotlin
 * QrScannerView(onQrScanned = { code -> viewModel.onScan(code) })
 * ```
 *
 * For more control (custom camera config, multiple plugins, shared camera state across
 * composables) use [QrScannerPlugin] directly with `rememberCameraKState`.
 *
 * ## Permissions
 *
 * This composable does **not** request the camera permission. Callers must ensure the
 * `Permission.CAMERA` is granted before this composable enters composition (for example via
 * `PermissionsController.RequestCameraPermission`). When the permission is missing the
 * underlying `CameraKScreen` will surface a platform-appropriate error state.
 *
 * @param onQrScanned Called on the UI thread for every QR code that passes the plugin's
 *   deduplication and throttling rules. If [scannerConfig]`.onQrScanned` is also set, it
 *   will be invoked alongside this callback.
 * @param modifier Modifier applied to the root container.
 * @param overlay Composable drawn on top of the camera preview. Defaults to
 *   [DefaultScannerOverlay] (centered viewfinder + scrim). Pass a no-op lambda to disable.
 * @param config Camera configuration. Defaults to back-lens, auto-everything.
 * @param scannerConfig Throttling / pause-on-first-scan behavior for the plugin.
 */
@Composable
fun QrScannerView(
    onQrScanned: (String) -> Unit,
    modifier: Modifier = Modifier,
    overlay: @Composable () -> Unit = { DefaultScannerOverlay() },
    config: CameraConfiguration = CameraConfiguration(cameraLens = CameraLens.BACK),
    scannerConfig: QrScannerConfig = QrScannerConfig(),
) {
    val plugin = remember(scannerConfig) { QrScannerPlugin(scannerConfig) }
    val currentOnScan by rememberUpdatedState(onQrScanned)

    val holderRef = remember { HolderRef() }

    val cameraState by rememberCameraKState(
        config = config,
        setupPlugins = { holder ->
            holder.attachPlugin(plugin)
            holderRef.value = holder
        },
    )

    // Collect scan events and forward them to the caller. We can't launch this inside
    // setupPlugins because that lambda completes during initialization; a LaunchedEffect
    // tied to the holder gives us a coroutine that lives as long as the screen.
    LaunchedEffect(holderRef.value) {
        val holder = holderRef.value ?: return@LaunchedEffect
        holder.events
            .filterIsInstance<CameraKEvent.QRCodeScanned>()
            .collect { event -> currentOnScan(event.qrCode) }
    }

    CameraKScreen(
        modifier = modifier,
        cameraState = cameraState,
        showPreview = true,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            overlay()
        }
    }
}

/** Tiny holder used to pass the [CameraKStateHolder] from setupPlugins into a LaunchedEffect. */
private class HolderRef {
    var value: CameraKStateHolder? = null
}
