package core.domain.camera.builder

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import core.domain.camera.controller.CameraController
import core.domain.camera.enums.AspectRatio
import core.domain.camera.enums.CameraDeviceType
import core.domain.camera.enums.CameraLens
import core.domain.camera.enums.Directory
import core.domain.camera.enums.FlashMode
import core.domain.camera.enums.ImageFormat
import core.domain.camera.enums.QualityPrioritization
import core.domain.camera.enums.TorchMode
import core.domain.camera.plugins.CameraPlugin
import core.domain.camera.utils.InvalidConfigurationException

/**
 * Android-specific implementation of [CameraControllerBuilder].
 *
 * @param context The Android [Context], typically an Activity or Application context.
 * @param lifecycleOwner The [LifecycleOwner], usually the hosting Activity or Fragment.
 */
class AndroidCameraControllerBuilder(private val context: Context, private val lifecycleOwner: LifecycleOwner) :
    CameraControllerBuilder {

    private var flashMode: FlashMode = FlashMode.OFF
    private var cameraLens: CameraLens = CameraLens.BACK
    private var imageFormat: ImageFormat? = null
    private var directory: Directory? = null
    private var torchMode: TorchMode = TorchMode.AUTO
    private var qualityPriority: QualityPrioritization = QualityPrioritization.QUALITY
    private var cameraDeviceType: CameraDeviceType = CameraDeviceType.DEFAULT
    private var returnFilePath: Boolean = false
    private var aspectRatio: AspectRatio = AspectRatio.RATIO_9_16
    private var targetResolution: Pair<Int, Int>? = null
    private var targetResolutionFront: Pair<Int, Int>? = null
    private val plugins = mutableListOf<CameraPlugin>()

    override fun setFlashMode(flashMode: FlashMode): CameraControllerBuilder {
        this.flashMode = flashMode
        return this
    }

    override fun setCameraLens(cameraLens: CameraLens): CameraControllerBuilder {
        this.cameraLens = cameraLens
        return this
    }

    /**
     * Sets the preferred camera device type (telephoto, ultra-wide, macro, etc.).
     *
     * The controller will attempt to select the specified camera type using Camera2 Interop.
     * If the requested type is not available on the device, it will gracefully fall back
     * to the default camera.
     *
     * @param deviceType The desired camera device type
     * @return This builder instance for chaining
     */
    override fun setPreferredCameraDeviceType(deviceType: CameraDeviceType): CameraControllerBuilder {
        this.cameraDeviceType = deviceType
        return this
    }

    override fun setTorchMode(torchMode: TorchMode): CameraControllerBuilder {
        this.torchMode = torchMode
        return this
    }

    override fun setQualityPrioritization(prioritization: QualityPrioritization): CameraControllerBuilder {
        this.qualityPriority = prioritization
        return this
    }

    override fun setReturnFilePath(returnFilePath: Boolean): CameraControllerBuilder {
        this.returnFilePath = returnFilePath
        return this
    }

    override fun setAspectRatio(aspectRatio: AspectRatio): CameraControllerBuilder {
        this.aspectRatio = aspectRatio
        return this
    }

    override fun setResolution(width: Int, height: Int): CameraControllerBuilder {
        this.targetResolution = width to height
        return this
    }

    override fun setResolutionFront(width: Int, height: Int): CameraControllerBuilder {
        this.targetResolutionFront = width to height
        return this
    }

    override fun setImageFormat(imageFormat: ImageFormat): CameraControllerBuilder {
        this.imageFormat = imageFormat
        return this
    }

    override fun setDirectory(directory: Directory): CameraControllerBuilder {
        this.directory = directory
        return this
    }

    override fun addPlugin(plugin: CameraPlugin): CameraControllerBuilder {
        plugins.add(plugin)
        return this
    }

    override fun build(): CameraController {
        val format = imageFormat ?: throw InvalidConfigurationException("ImageFormat must be set.")
        val dir = directory ?: throw InvalidConfigurationException("Directory must be set.")

        /* if (flashMode == FlashMode.ON && cameraLens == CameraLens.FRONT) {
             throw InvalidConfigurationException("Flash mode ON is not supported with the front camera.")
         }*/
        val cameraController = CameraController(
            context = context,
            lifecycleOwner = lifecycleOwner,
            flashMode = flashMode,
            cameraLens = cameraLens,
            imageFormat = format,
            directory = dir,
            plugins = plugins,
            torchMode = torchMode,
            qualityPriority = qualityPriority,
            cameraDeviceType = cameraDeviceType,
            returnFilePath = returnFilePath,
            aspectRatio = aspectRatio,
            targetResolution = targetResolution,
            targetResolutionFront = targetResolutionFront,
        )
        plugins.forEach {
            it.initialize(cameraController)
        }

        return cameraController
    }
}
