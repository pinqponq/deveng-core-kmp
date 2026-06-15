package core.domain.camera.scanner

import core.domain.camera.state.CameraKEvent
import core.domain.camera.state.CameraKPlugin
import core.domain.camera.state.CameraKState
import core.domain.camera.state.CameraKStateHolder
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
actual class QrScannerPlugin actual constructor(
    private val config: QrScannerConfig,
) : CameraKPlugin {

    private var attachJob: Job? = null
    private var analyzer: MlKitQrCodeAnalyzer? = null
    private var stateHolder: CameraKStateHolder? = null

    private var lastCode: String? = null
    private var lastEmitMs: Long = 0L

    @Volatile
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

            val a = MlKitQrCodeAnalyzer { code -> handleDetection(code) }
            analyzer = a
            controller.registerImageAnalyzer(a)
        }
    }

    actual override fun onDetach() {
        attachJob?.cancel()
        attachJob = null

        val controller = stateHolder?.getController()
        analyzer?.let { a ->
            try {
                controller?.unregisterImageAnalyzer(a)
            } catch (_: Throwable) {
                // Controller may have already been cleaned up; nothing we can do.
            }
            a.close()
        }
        analyzer = null
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
