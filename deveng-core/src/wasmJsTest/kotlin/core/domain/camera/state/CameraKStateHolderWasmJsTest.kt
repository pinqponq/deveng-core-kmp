@file:OptIn(ExperimentalWasmJsInterop::class)

package core.domain.camera.state

import core.domain.camera.controller.WasmCameraControllerBuilder
import core.domain.camera.enums.Directory
import core.domain.camera.enums.ImageFormat
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertIs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

class CameraKStateHolderWasmJsTest {

    private val holderScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @BeforeTest
    fun setUp() {
        holdCameraRequests()
    }

    @AfterTest
    fun tearDown() {
        holderScope.cancel()
        restoreCameraRequests()
    }

    @Test
    fun initialize_whileBrowserOpensCamera_staysInitializing() = runTest {
        val holder = createHolder()

        holderScope.launch { runCatching { holder.initialize() } }
        waitUntil { isCameraRequestPending() }

        assertIs<CameraKState.Initializing>(holder.cameraState.value)
    }

    @Test
    fun initialize_afterBrowserAnswersCameraRequest_becomesReady() = runTest {
        val holder = createHolder()
        holderScope.launch { runCatching { holder.initialize() } }
        waitUntil { isCameraRequestPending() }

        denyCameraRequest()
        waitUntil { holder.cameraState.value is CameraKState.Ready }

        assertIs<CameraKState.Ready>(holder.cameraState.value)
    }

    private fun createHolder() = CameraKStateHolder(
        cameraConfiguration = CameraConfiguration(),
        controllerFactory = {
            WasmCameraControllerBuilder()
                .setImageFormat(ImageFormat.JPEG)
                .setDirectory(Directory.PICTURES)
                .build()
        },
        coroutineScope = holderScope,
    )

    private suspend fun waitUntil(condition: () -> Boolean) = withContext(Dispatchers.Default) {
        withTimeout(5_000) {
            while (!condition()) delay(10)
        }
    }
}

/** Replaces getUserMedia with a request that stays open until the test answers it. */
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

private fun denyCameraRequest(): Unit = js(
    """window.__testCameraRequest.reject(new DOMException('Permission denied', 'NotAllowedError'))""",
)
