package core.domain.camera.controller

import core.domain.camera.enums.CameraDeviceType
import core.domain.camera.enums.CameraLens
import core.domain.camera.enums.FlashMode
import core.domain.camera.enums.ImageFormat
import core.domain.camera.enums.QualityPrioritization
import core.domain.camera.enums.TorchMode
import androidx.compose.ui.graphics.ImageBitmap
import core.domain.camera.result.ImageCaptureResult
import core.domain.camera.video.VideoCaptureResult
import core.domain.camera.video.VideoConfiguration

/**
 * Interface defining the core functionalities of the CameraController.
 */
expect class CameraController {

    /**
     * When true, a still photo is taken before [startRecording] for the video thumbnail (legacy).
     * Prefer [extractVideoThumbnailFromFile] after stop on iOS; Android uses [captureRecordingThumbnailFrame].
     */
    val usesPhotoCaptureForVideoThumbnail: Boolean

    /**
     * Captures an image and returns it as a ByteArray.
     *
     * @return The result of the image capture operation with ByteArray.
     * @deprecated Use takePictureToFile() for better performance. This method processes images
     *             through decode/encode cycles which adds 2-3 seconds overhead. Will be removed in v2.0.
     */
    @Deprecated(
        message = "Use takePictureToFile() instead for better performance",
        replaceWith = ReplaceWith("takePictureToFile()"),
        level = DeprecationLevel.WARNING,
    )
    suspend fun takePicture(): ImageCaptureResult

    /**
     * Captures an image without saving to disk.
     *
     * Returns [ImageCaptureResult.Success] with [ByteArray][ImageCaptureResult.Success.byteArray]
     * and optional [ImageBitmap][ImageCaptureResult.Success.bitmap] for thumbnail. The app should
     * save using [core.util.image.PhotoSaveUtils.savePhoto] after optionally adding location
     * with [core.util.image.PhotoSaveUtils.addLocationExif].
     *
     * @return ImageCaptureResult.Success(byteArray, bitmap) or an error result
     */
    suspend fun takePictureToFile(): ImageCaptureResult

    /**
     * Toggles the flash mode between ON, OFF, and AUTO.
     */
    fun toggleFlashMode()

    /**
     * Sets the flash mode of the camera
     *
     * @param mode The desired [FlashMode]
     */
    fun setFlashMode(mode: FlashMode)

    /**
     * @return the current [FlashMode] of the camera, if available
     */
    fun getFlashMode(): FlashMode?

    /**
     * Toggles the torch mode between ON, OFF, and AUTO.
     *
     * Note: On Android, AUTO mode is not natively supported by CameraX and will be treated as ON.
     * iOS supports AUTO mode natively through AVFoundation.
     */
    fun toggleTorchMode()

    /**
     * Sets the torch mode of the camera
     *
     * @param mode The desired [TorchMode]
     *
     * Note: On Android, TorchMode.AUTO is not natively supported by CameraX and will be treated as ON.
     * iOS supports AUTO mode natively through AVFoundation.
     */
    fun setTorchMode(mode: TorchMode)

    /**
     * Gets the current torch mode.
     *
     * @return The current [TorchMode] (ON, OFF, AUTO), or null if not available
     *
     * Note: Desktop does not support torch mode and will always return null.
     */
    fun getTorchMode(): TorchMode?

    /**
     * Toggles the camera lens between FRONT and BACK.
     *
     * Note: Desktop does not support camera lens switching (single camera).
     */
    fun toggleCameraLens()

    /**
     * Gets the current camera lens.
     *
     * @return The current [CameraLens] (FRONT or BACK), or null if not available
     */
    fun getCameraLens(): CameraLens?

    /**
     * Gets the current image format.
     *
     * @return The configured [ImageFormat] (JPEG or PNG)
     */
    fun getImageFormat(): ImageFormat

    /**
     * Gets the current quality prioritization setting.
     *
     * @return The configured [QualityPrioritization]
     */
    fun getQualityPrioritization(): QualityPrioritization

    /**
     * Gets the current camera device type.
     *
     * @return The configured [CameraDeviceType]
     */
    fun getPreferredCameraDeviceType(): CameraDeviceType

    /**
     * Switches to a different camera device type at runtime.
     *
     * On iOS this switches between wide-angle, telephoto, ultra-wide, etc.
     * On Android this is a no-op (CameraX handles device selection automatically).
     * On Desktop this is a no-op (single camera).
     *
     * @param deviceType The desired [CameraDeviceType] to switch to.
     */
    fun setPreferredCameraDeviceType(deviceType: CameraDeviceType)

    /**
     * Sets the zoom level.
     *
     * @param zoomRatio The zoom ratio to set. 1.0 is no zoom, values > 1.0 zoom in.
     *                  The actual range depends on the camera hardware.
     *                  On Android: typically 1.0 to maxZoomRatio (often 2.0-10.0)
     *                  On iOS: typically 1.0 to device.maxAvailableVideoZoomFactor
     *                  On Desktop: not supported, no-op
     *
     * Note: Zoom is applied gradually/smoothly on supported platforms.
     */
    fun setZoom(zoomRatio: Float)

    /**
     * Gets the current zoom ratio.
     *
     * @return The current zoom ratio, or 1.0 if zoom is not supported
     */
    fun getZoom(): Float

    /**
     * Gets the maximum zoom ratio supported by the camera.
     *
     * @return The maximum zoom ratio, or 1.0 if zoom is not supported
     */
    fun getMaxZoom(): Float

    /**
     * Sets the focus (and metering) point for tap-to-focus.
     *
     * @param normalizedX X in normalized coordinates (0f = left, 1f = right).
     * @param normalizedY Y in normalized coordinates (0f = top, 1f = bottom).
     *
     * No-op on platforms that do not support tap-to-focus (e.g. Desktop, WASM).
     */
    fun setFocusPoint(normalizedX: Float, normalizedY: Float)

    /**
     * Gets the exposure compensation index range (min, max). Default/auto is usually 0.
     * @return Pair(minIndex, maxIndex), or (0, 0) if not supported.
     */
    fun getExposureCompensationRange(): Pair<Int, Int>

    /**
     * Sets the exposure compensation (brightness) index. Must be within [getExposureCompensationRange].
     * 0 = default/auto. Negative = darker, positive = brighter.
     */
    fun setExposureCompensationIndex(index: Int)

    /**
     * Gets the current exposure compensation index.
     */
    fun getExposureCompensationIndex(): Int

    /**
     * Starts the camera session.
     */
    fun startSession()

    /**
     * Stops the camera session.
     */
    fun stopSession()

    /**
     * Adds a listener for image capture events.
     *
     * @param listener The listener to add, receiving image data as [ByteArray].
     */
    fun addImageCaptureListener(listener: (ByteArray) -> Unit)

    /**
     * Enables or disables preview stabilization (EIS). Only takes effect when the device reports
     * support; no-op otherwise. On Android this rebuilds the Preview use case and rebinds — expect
     * a brief viewfinder interruption. Intended to be called when the UI switches between photo and
     * video capture modes so that stabilization (and its small FOV crop) is active only during video.
     */
    fun setPreviewStabilizationEnabled(enabled: Boolean)

    /**
     * Aligns the native capture session with photo vs video UI mode (iOS: session preset).
     * Call when the user switches between photo and video in the camera UI — not when recording
     * starts or stops. No-op on platforms that do not multiplex still/video through one preset.
     */
    fun applyCaptureModeSessionPreset(isVideoMode: Boolean)

    /**
     * Returns true if the device supports a real night photography extension (e.g. CameraX
     * ExtensionMode.NIGHT on Android). When false, the night mode button has no hardware backing
     * and callers can decide to hide it or fall back to a software approach.
     */
    fun isNightModeSupported(): Boolean

    /**
     * Enables or disables real night mode. On Android this rebinds the camera with
     * ExtensionMode.NIGHT when [isNightModeSupported] is true; no-op on other platforms.
     *
     * @param enabled true to activate night mode, false to return to the default pipeline.
     */
    fun setNightMode(enabled: Boolean)

    /**
     * Enables or disables wide-selfie mode (vendor-specific FOV expansion on the front camera).
     *
     * On Samsung/compatible devices, the front camera applies aggressive lens distortion
     * correction that crops the sensor's edge pixels. Wide-selfie mode overrides
     * `SCALER_CROP_REGION` via Camera2Interop to use the full pre-correction active array,
     * giving back the wider field of view used by native Camera and Snapchat.
     *
     * No-op on iOS, Desktop, WASM, and devices that don't expose the relevant Camera2 keys.
     *
     * @param enabled Whether wide-selfie mode should be enabled.
     */
    fun setWideSelfieMode(enabled: Boolean)

    /**
     * Returns whether wide-selfie mode is currently enabled.
     */
    fun isWideSelfieEnabled(): Boolean

    /**
     * Initializes all registered plugins.
     */
    fun initializeControllerPlugins()

    /**
     * Cleans up resources when the controller is no longer needed.
     * Should be called when disposing the controller to prevent memory leaks.
     *
     * Note: After calling cleanup(), the controller should not be used again.
     */
    fun cleanup()

    /**
     * Callback invoked when the user taps the camera preview.
     * Passes normalized (0..1) X and Y coordinates of the tap.
     * Used on iOS to forward native UIKit taps back to Compose for the focus ring.
     * On other platforms this is unused (Compose gesture overlay works directly).
     */
    var onPreviewTapListener: ((normalizedX: Float, normalizedY: Float) -> Unit)?

    /**
     * Callback invoked when the user double-taps the camera preview.
     * Used on iOS to forward native UIKit double-taps back to Compose.
     */
    var onPreviewDoubleTapListener: (() -> Unit)?

    /**
     * When non-null, invoked with normalized tap coordinates (0..1) before tap-to-focus.
     * Return true to skip [setFocusPoint] and the focus ring (e.g. taps near overlay chrome).
     * Preview gesture layers on Android, Desktop, and iOS consult this when present.
     */
    var shouldSuppressTapToFocus: ((normalizedX: Float, normalizedY: Float) -> Boolean)?

    // ═══════════════════════════════════════════════════════════════
    // Video Recording
    // ═══════════════════════════════════════════════════════════════

    /**
     * Captures a still frame for video thumbnails from the live preview (Android).
     */
    suspend fun captureRecordingThumbnailFrame(): ImageBitmap?

    /**
     * Decodes the first video frame for gallery/chrome thumbnails (iOS). No-op on other platforms.
     */
    suspend fun extractVideoThumbnailFromFile(
        filePath: String,
        isFrontCamera: Boolean = false,
    ): ImageBitmap?

    /**
     * Starts video recording to a file.
     *
     * Safe to call while photo capture is active — both use cases coexist.
     * The output file format is MP4 (H.264 video + AAC audio) on all platforms.
     *
     * @param configuration Recording settings (quality, audio, duration limit, output path).
     * @return The actual output file path where the recording is being written.
     */
    suspend fun startRecording(configuration: VideoConfiguration = VideoConfiguration()): String

    /**
     * Stops the active video recording and finalizes the output file.
     *
     * Suspends until the file is fully written and closed and the capture pipeline is free to start
     * another recording. Platform fix-ups that rewrite the finished file (see
     * [applyRecordingPostProcessing]) are deliberately left out so back-to-back recordings are not
     * delayed by them; callers that hand the file to the user must apply them before doing so.
     *
     * @return [VideoCaptureResult.Success] with file path and duration, or [VideoCaptureResult.Error].
     */
    suspend fun stopRecording(): VideoCaptureResult

    /**
     * Applies platform fix-ups the finished recording needs before it can be shown or shared —
     * currently the front-lens flip that makes playback match the mirrored selfie preview (Android
     * and iOS; identity elsewhere). Skipped on Android when the recording was started with
     * [core.domain.camera.video.VideoConfiguration.shouldMirrorFrontLensToMatchPreview] off.
     *
     * Rewrites the file in place and may take seconds for a long clip, so it runs after
     * [stopRecording] has already released the capture pipeline.
     *
     * @param result The result returned by [stopRecording].
     * @return The result to hand to the caller; errors pass through untouched.
     */
    suspend fun applyRecordingPostProcessing(result: VideoCaptureResult): VideoCaptureResult

    /**
     * Pauses the active video recording.
     *
     * Audio and video capture are suspended; the output file remains open.
     * No-op if not currently recording.
     *
     * Note: Desktop implementation is best-effort (frame-drop based).
     */
    suspend fun pauseRecording()

    /**
     * Resumes a paused video recording.
     *
     * No-op if not currently paused.
     */
    suspend fun resumeRecording()
}
