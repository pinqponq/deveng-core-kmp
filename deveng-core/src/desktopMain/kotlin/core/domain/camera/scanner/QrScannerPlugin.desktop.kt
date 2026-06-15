package core.domain.camera.scanner

import core.domain.camera.state.CameraKEvent
import core.domain.camera.state.CameraKPlugin
import core.domain.camera.state.CameraKState
import core.domain.camera.state.CameraKStateHolder
import kotlinx.coroutines.Dispatchers
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
    private var decoder: OpenCvQrCodeDecoder? = null
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

        attachJob = stateHolder.pluginScope.launch(Dispatchers.Default) {
            val ready = stateHolder.cameraState
                .filterIsInstance<CameraKState.Ready>()
                .first()
            val controller = ready.controller

            val d = OpenCvQrCodeDecoder()
            decoder = d

            // Collect frames from the fan-out flow. The grabber emits at ~30 fps;
            // OpenCV decode is the slow step (tens of ms per frame), so back-pressure
            // is managed by SharedFlow's DROP_OLDEST policy — we naturally sample
            // whichever frame is most recent when the decoder finishes the previous one.
            controller.getFrameFlow().collect { image ->
                if (paused) return@collect
                val code = d.decode(image) ?: return@collect
                handleDetection(code)
            }
        }
    }

    actual override fun onDetach() {
        attachJob?.cancel()
        attachJob = null
        decoder = null
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
