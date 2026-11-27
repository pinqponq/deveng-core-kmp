package core.domain.camera.scanner

import core.domain.camera.state.CameraKEvent
import core.domain.camera.state.CameraKPlugin
import core.domain.camera.state.CameraKState
import core.domain.camera.state.CameraKStateHolder
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import platform.AVFoundation.AVMetadataObjectTypeQRCode
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
actual class QrScannerPlugin actual constructor(
    private val config: QrScannerConfig,
) : CameraKPlugin {

    private var attachJob: Job? = null
    private var delegate: QrMetadataDelegate? = null
    private var stateHolder: CameraKStateHolder? = null

    private var lastCode: String? = null
    private var lastEmitMs: Long = 0L

    // iOS callbacks run on `dispatch_get_main_queue()`, the same queue the plugin's
    // coroutine is eventually resumed on — no cross-thread write to `paused`, so no
    // `@Volatile` is needed (K/N wouldn't accept it anyway without extra opt-ins).
    private var paused: Boolean = false

    actual override fun onAttach(stateHolder: CameraKStateHolder) {
        this.stateHolder = stateHolder
        this.paused = false
        this.lastCode = null
        this.lastEmitMs = 0L

        attachJob = stateHolder.pluginScope.launch {
            val ready = stateHolder.cameraState
                .filterIsInstance<CameraKState.Ready>()
                .first()
            val controller = ready.controller

            val d = QrMetadataDelegate { code -> handleDetection(code) }
            delegate = d

            // Install the delegate first so any early metadata is caught, then switch the
            // output's accepted types to QR. The type update must happen inside
            // queueConfigurationChange because AVFoundation rejects metadata-type mutations
            // outside begin/commitConfiguration blocks.
            controller.setMetadataObjectsDelegate(d)
            controller.queueConfigurationChange {
                // AVFoundation type constants are imported as `String?` in Kotlin/Native;
                // `listOfNotNull` keeps the List<String> contract required by
                // `updateMetadataObjectTypes` and silently drops any null values.
                controller.updateMetadataObjectTypes(listOfNotNull(AVMetadataObjectTypeQRCode))
            }
        }
    }

    actual override fun onDetach() {
        attachJob?.cancel()
        attachJob = null

        // Clearing the accepted types disables detection without tearing down the session.
        val controller = stateHolder?.getController()
        if (controller != null) {
            try {
                controller.queueConfigurationChange {
                    controller.updateMetadataObjectTypes(emptyList())
                }
            } catch (_: Throwable) {
                // Session may already be tearing down; harmless.
            }
        }

        delegate = null
        stateHolder = null
    }

    private fun handleDetection(code: String) {
        if (paused) return

        val now = Clock.System.now().toEpochMilliseconds()
        if (config.scanThrottleMs > 0L &&
            code == lastCode &&
            now - lastEmitMs < config.scanThrottleMs
        ) {
            return
        }

        lastCode = code
        lastEmitMs = now

        val holder = stateHolder ?: return
        holder.pluginScope.launch {
            holder.emitEvent(CameraKEvent.QRCodeScanned(code))
        }
        config.onQrScanned?.invoke(code)

        if (config.pauseOnFirstScan) {
            paused = true
        }
    }
}
