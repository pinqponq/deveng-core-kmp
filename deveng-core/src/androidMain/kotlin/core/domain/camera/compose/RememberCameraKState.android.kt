package core.domain.camera.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import core.domain.camera.builder.createAndroidCameraControllerBuilder
import core.domain.camera.state.CameraConfiguration
import core.domain.camera.state.CameraKState
import core.domain.camera.state.CameraKStateHolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Android implementation of [rememberCameraKState].
 * Automatically manages Context and LifecycleOwner dependencies.
 */
@Composable
actual fun rememberCameraKState(
    config: CameraConfiguration,
    setupPlugins: suspend (CameraKStateHolder) -> Unit,
    onHolder: (CameraKStateHolder) -> Unit,
): State<CameraKState> {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    val stateHolder =
        remember(config) {
            CameraKStateHolder(
                cameraConfiguration = config,
                controllerFactory = {
                    withContext(Dispatchers.Main) {
                        createAndroidCameraControllerBuilder(context, lifecycleOwner)
                            .apply {
                                setFlashMode(config.flashMode)
                                setTorchMode(config.torchMode)
                                setCameraLens(config.cameraLens)
                                setImageFormat(config.imageFormat)
                                setQualityPrioritization(config.qualityPrioritization)
                                setPreferredCameraDeviceType(config.cameraDeviceType)
                                setAspectRatio(config.aspectRatio)
                                setDirectory(config.directory)
                                config.targetResolution?.let { (width, height) ->
                                    setResolution(width, height)
                                }
                                config.targetResolutionFront?.let { (width, height) ->
                                    setResolutionFront(width, height)
                                }
                            }.build()
                    }
                },
                coroutineScope = scope,
            )
        }

    // Initialize controller and plugins
    LaunchedEffect(stateHolder) {
        onHolder(stateHolder)
        setupPlugins(stateHolder)
        stateHolder.initialize()
    }

    // Cleanup on disposal
    DisposableEffect(stateHolder) {
        onDispose {
            stateHolder.shutdown()
        }
    }

    return stateHolder.cameraState.collectAsStateWithLifecycle()
}
