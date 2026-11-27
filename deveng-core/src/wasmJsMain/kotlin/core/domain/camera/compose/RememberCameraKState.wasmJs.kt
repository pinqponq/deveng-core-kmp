package core.domain.camera.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import core.domain.camera.controller.WasmCameraControllerBuilder
import core.domain.camera.state.CameraConfiguration
import core.domain.camera.state.CameraKState
import core.domain.camera.state.CameraKStateHolder

/**
 * WASM/JS: browser camera via [WasmCameraControllerBuilder] and [getUserMedia](https://developer.mozilla.org/en-US/docs/Web/API/MediaDevices/getUserMedia).
 */
@Composable
actual fun rememberCameraKState(
    config: CameraConfiguration,
    setupPlugins: suspend (CameraKStateHolder) -> Unit,
    onHolder: (CameraKStateHolder) -> Unit,
): State<CameraKState> {
    val scope = rememberCoroutineScope()

    val stateHolder =
        remember(config) {
            CameraKStateHolder(
                cameraConfiguration = config,
                controllerFactory = {
                    WasmCameraControllerBuilder()
                        .apply {
                            setImageFormat(config.imageFormat)
                            setDirectory(config.directory)
                            setCameraLens(config.cameraLens)
                            config.targetResolution?.let { (width, height) ->
                                setResolution(width, height)
                            }
                            config.targetResolutionFront?.let { (width, height) ->
                                setResolutionFront(width, height)
                            }
                        }.build()
                },
                coroutineScope = scope,
            )
        }

    LaunchedEffect(stateHolder) {
        onHolder(stateHolder)
        setupPlugins(stateHolder)
        stateHolder.initialize()
    }

    DisposableEffect(stateHolder) {
        onDispose {
            stateHolder.shutdown()
        }
    }

    return stateHolder.cameraState.collectAsState()
}
