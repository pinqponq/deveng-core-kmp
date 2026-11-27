@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package core.domain.camera.scanner

import core.domain.camera.state.CameraKEvent
import core.domain.camera.state.CameraKPlugin
import core.domain.camera.state.CameraKState
import core.domain.camera.state.CameraKStateHolder
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Minimum interval between jsQR calls. jsQR is fast (~20–50 ms on typical frames) but we
 * throttle polling to keep CPU use reasonable. 150 ms ≈ 6–7 scans/sec which is plenty for
 * QR recognition from a live feed.
 */
private const val SCAN_POLL_INTERVAL_MS: Long = 150L

@OptIn(ExperimentalTime::class)
actual class QrScannerPlugin actual constructor(
    private val config: QrScannerConfig,
) : CameraKPlugin {

    private var attachJob: Job? = null
    private var stateHolder: CameraKStateHolder? = null

    private var lastCode: String? = null
    private var lastEmitMs: Long = 0L

    // WASM is single-threaded — no @Volatile needed. Reads and writes always happen from
    // the Main dispatcher on which the plugin's coroutine runs.
    private var paused: Boolean = false

    actual override fun onAttach(stateHolder: CameraKStateHolder) {
        ensureWasmScannerBootstrap()
        this.stateHolder = stateHolder
        this.paused = false
        this.lastCode = null
        this.lastEmitMs = 0L

        attachJob = stateHolder.pluginScope.launch {
            val ready = stateHolder.cameraState
                .filterIsInstance<CameraKState.Ready>()
                .first()
            val controller = ready.controller
            val video = controller.video
            val canvas = controller.canvas

            while (isActive) {
                // `readyState >= HAVE_CURRENT_DATA` (2) means the video has at least one
                // frame available — sufficient to indicate the stream is attached AND we
                // can sample frames.
                if (!paused && video.readyState.toInt() >= 2) {
                    val result = jsScanQr(video, canvas)
                    val code = result?.toString()
                    if (!code.isNullOrEmpty()) {
                        handleDetection(code)
                    }
                }
                delay(SCAN_POLL_INTERVAL_MS)
            }
        }
    }

    actual override fun onDetach() {
        attachJob?.cancel()
        attachJob = null
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
