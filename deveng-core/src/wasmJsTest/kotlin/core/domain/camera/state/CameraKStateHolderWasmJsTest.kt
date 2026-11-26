@file:OptIn(ExperimentalWasmJsInterop::class)

package core.domain.camera.state

import core.domain.camera.controller.WasmCameraControllerBuilder
import core.domain.camera.enums.Directory
import core.domain.camera.enums.ImageFormat
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

class CameraKStateHolderWasmJsTest {

    @BeforeTest
    fun setUp() {
        holdCameraRequests()
    }

    @AfterTest
    fun tearDown() {
        restoreCameraRequests()
    }

    @Test
    fun initialize_whileBrowserOpensCamera_staysInitializing() = runTest {
        val holder = createHolder()

        backgroundScope.launch { holder.initialize() }
        waitUntil { isCameraRequestPending() }

        assertIs<CameraKState.Initializing>(holder.cameraState.value)
    }

    @Test
    fun initialize_afterBrowserDeniesCamera_becomesReadyWithoutPreview() = runTest {
        val holder = createHolder()
        backgroundScope.launch { runCatching { holder.initialize() } }
        waitUntil { isCameraRequestPending() }

        denyCameraRequest()
        waitUntil { holder.cameraState.value is CameraKState.Ready }

        assertIs<CameraKState.Ready>(holder.cameraState.value)
        holder.shutdown()
    }

    @Test
    fun initialize_cancelledWhileBrowserOpensCamera_removesPreviewAndReportsNoError() = runTest {
        val holder = createHolder()
        val initializing = backgroundScope.launch { holder.initialize() }
        waitUntil { isCameraRequestPending() && isPreviewVideoAttached() }

        initializing.cancel()
        waitUntil { !isPreviewVideoAttached() }

        assertFalse(isPreviewVideoAttached())
        assertIs<CameraKState.Initializing>(holder.cameraState.value)
    }

    private fun TestScope.createHolder() = CameraKStateHolder(
        cameraConfiguration = CameraConfiguration(),
        controllerFactory = {
            WasmCameraControllerBuilder()
                .setImageFormat(ImageFormat.JPEG)
                .setDirectory(Directory.PICTURES)
                .build()
        },
        coroutineScope = backgroundScope,
    )

    // The controller opens the camera on its own scope and the browser answers through a JavaScript promise,
    // so virtual time cannot move them; this waits in real time.
    private suspend fun waitUntil(condition: () -> Boolean) = withContext(Dispatchers.Default) {
        withTimeout(WAIT_TIMEOUT_MILLIS) {
            while (!condition()) delay(POLL_INTERVAL_MILLIS)
        }
    }

    private companion object {
        // Below the 2 s timeout of the browser test runner, so a stuck wait fails here with its own message.
        const val WAIT_TIMEOUT_MILLIS = 1_500L
        const val POLL_INTERVAL_MILLIS = 10L
    }
}

private fun holdCameraRequests(): Unit = js(
    """{
    window.__testOriginalGetUserMedia = MediaDevices.prototype.getUserMedia;
    window.__testCameraRequest = null;
    MediaDevices.prototype.getUserMedia = function () {
        return new Promise(function (resolve, reject) { window.__testCameraRequest = { resolve: resolve, reject: reject }; });
    };
}""",
)

private fun restoreCameraRequests(): Unit = js(
    """{
    MediaDevices.prototype.getUserMedia = window.__testOriginalGetUserMedia;
    window.__testCameraRequest = null;
}""",
)

private fun isCameraRequestPending(): Boolean = js("window.__testCameraRequest != null")

private fun isPreviewVideoAttached(): Boolean = js("document.getElementById('deveng-wasm-camera-video') != null")

private fun denyCameraRequest(): Unit = js(
    """window.__testCameraRequest.reject(new DOMException('Permission denied', 'NotAllowedError'))""",
)
