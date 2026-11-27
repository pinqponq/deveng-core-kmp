package core.domain.camera.controller

import core.domain.camera.builder.CameraControllerBuilder
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
 * WASM/JS [CameraController] builder using [getUserMedia](https://developer.mozilla.org/en-US/docs/Web/API/MediaDevices/getUserMedia).
 */
class WasmCameraControllerBuilder : CameraControllerBuilder {
    private var cameraLens: CameraLens = CameraLens.BACK
    private var imageFormat: ImageFormat? = null
    private var directory: Directory? = null
    private var qualityPriority: QualityPrioritization = QualityPrioritization.QUALITY
    private val plugins = mutableListOf<CameraPlugin>()

    override fun setFlashMode(flashMode: FlashMode): CameraControllerBuilder = this

    override fun setCameraLens(cameraLens: CameraLens): CameraControllerBuilder = apply { this.cameraLens = cameraLens }

    override fun setPreferredCameraDeviceType(deviceType: CameraDeviceType): CameraControllerBuilder = this

    override fun setImageFormat(imageFormat: ImageFormat): CameraControllerBuilder = apply { this.imageFormat = imageFormat }

    override fun setDirectory(directory: Directory): CameraControllerBuilder = apply { this.directory = directory }

    override fun addPlugin(plugin: CameraPlugin): CameraControllerBuilder = apply { plugins.add(plugin) }

    override fun build(): CameraController {
        val format = imageFormat ?: throw InvalidConfigurationException("ImageFormat must be set.")
        val dir = directory ?: throw InvalidConfigurationException("Directory must be set.")
        return CameraController(
            plugins = plugins.toMutableList(),
            imageFormat = format,
            directory = dir,
            qualityPriority = qualityPriority,
            initialLens = cameraLens,
        )
    }

    override fun setTorchMode(torchMode: TorchMode): CameraControllerBuilder = this

    override fun setQualityPrioritization(prioritization: QualityPrioritization): CameraControllerBuilder = apply {
        this.qualityPriority = prioritization
    }

    override fun setReturnFilePath(returnFilePath: Boolean): CameraControllerBuilder = this

    override fun setAspectRatio(aspectRatio: AspectRatio): CameraControllerBuilder = this

    override fun setResolution(width: Int, height: Int): CameraControllerBuilder = this

    override fun setResolutionFront(width: Int, height: Int): CameraControllerBuilder = this
}
