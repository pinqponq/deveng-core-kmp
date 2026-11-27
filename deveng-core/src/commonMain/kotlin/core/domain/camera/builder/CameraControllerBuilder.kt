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

/**
 * Builder interface for constructing a [CameraController] with customizable configurations and plugins.
 */
interface CameraControllerBuilder {
    fun setFlashMode(flashMode: FlashMode): CameraControllerBuilder

    fun setCameraLens(cameraLens: CameraLens): CameraControllerBuilder

    /**
     * Sets the camera device type (e.g., wide-angle, telephoto, ultra-wide).
     *
     * Note: Availability depends on device hardware. If the requested type is not available,
     * the platform will fall back to the default camera.
     *
     * @param deviceType The desired camera device type
     * @return The current instance of [CameraControllerBuilder]
     */
    fun setPreferredCameraDeviceType(deviceType: CameraDeviceType): CameraControllerBuilder

    fun setImageFormat(imageFormat: ImageFormat): CameraControllerBuilder

    fun setDirectory(directory: Directory): CameraControllerBuilder

    /**
     * Adds a [CameraPlugin] to the [CameraController].
     *
     * @param plugin The plugin to add.
     * @return The current instance of [CameraControllerBuilder].
     */
    fun addPlugin(plugin: CameraPlugin): CameraControllerBuilder

    /**
     * Builds and returns a configured instance of [CameraController].
     *
     * @throws InvalidConfigurationException If mandatory parameters are missing or configurations are incompatible.
     * @return A fully configured [CameraController] instance.
     */
    fun build(): CameraController

    fun setTorchMode(torchMode: TorchMode): CameraControllerBuilder

    /**
     * Sets the quality prioritization for the captured image.
     */
    fun setQualityPrioritization(prioritization: QualityPrioritization): CameraControllerBuilder

    /**
     * Configure whether takePicture() writes to a temp file first (true) or processes in memory (false).
     * Capture always returns [ImageCaptureResult.Success] with [ByteArray]; the app saves via [core.util.image.PhotoSaveUtils.savePhoto].
     */
    fun setReturnFilePath(returnFilePath: Boolean): CameraControllerBuilder

    /**
     * Sets the aspect ratio for preview and capture.
     * Supported values map to platform defaults (16:9, 4:3). 9:16 uses 16:9 with rotation; 1:1 falls back to closest available.
     */
    fun setAspectRatio(aspectRatio: AspectRatio): CameraControllerBuilder

    /**
     * Sets a target capture resolution (width x height) for preview/capture when the platform supports it.
     * Platforms may fall back to the closest supported resolution if an exact match is unavailable.
     */
    fun setResolution(width: Int, height: Int): CameraControllerBuilder

    /**
     * Upper bound for **front** still capture. When omitted, [setResolution] applies to both lenses.
     */
    fun setResolutionFront(width: Int, height: Int): CameraControllerBuilder
}
