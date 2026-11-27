package core.domain.camera.builder

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
 * iOS-specific implementation of [CameraControllerBuilder].
 */
class IOSCameraControllerBuilder : CameraControllerBuilder {

    private var flashMode: FlashMode = FlashMode.OFF
    private var torchMode: TorchMode = TorchMode.OFF
    private var cameraLens: CameraLens = CameraLens.BACK
    private var imageFormat: ImageFormat? = null
    private var directory: Directory? = null
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

    override fun setImageFormat(imageFormat: ImageFormat): CameraControllerBuilder {
        this.imageFormat = imageFormat
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

    override fun setDirectory(directory: Directory): CameraControllerBuilder {
        this.directory = directory
        return this
    }

    /**
     * Sets the preferred camera device type (wide-angle, telephoto, etc.).
     * Note: Availability depends on device hardware.
     *
     * @param deviceType The desired camera device type.
     * @return This builder instance for chaining.
     */
    override fun setPreferredCameraDeviceType(deviceType: CameraDeviceType): CameraControllerBuilder {
        this.cameraDeviceType = deviceType
        return this
    }

    override fun addPlugin(plugin: CameraPlugin): CameraControllerBuilder {
        plugins.add(plugin)
        return this
    }

    /**
     * Builds the configured iOS camera controller.
     *
     * @return A fully configured [CameraController] instance.
     * @throws InvalidConfigurationException If required configuration is missing.
     */
    override fun build(): CameraController {
        val format = imageFormat ?: throw InvalidConfigurationException("ImageFormat must be set.")
        val dir = directory ?: throw InvalidConfigurationException("Directory must be set.")

        val cameraController = CameraController(
            flashMode = flashMode,
            torchMode = torchMode,
            cameraLens = cameraLens,
            imageFormat = format,
            directory = dir,
            plugins = plugins,
            qualityPriority = qualityPriority,
            cameraDeviceType = cameraDeviceType,
            returnFilePath = returnFilePath,
            aspectRatio = aspectRatio,
            targetResolution = targetResolution,
            targetResolutionFront = targetResolutionFront,
        )

        return cameraController
    }
}
