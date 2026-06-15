package core.domain.camera.controller

import androidx.compose.ui.graphics.ImageBitmap
import core.domain.camera.enums.CameraDeviceType
import core.domain.camera.enums.CameraLens
import core.domain.camera.enums.Directory
import core.domain.camera.enums.FlashMode
import core.domain.camera.enums.ImageFormat
import core.domain.camera.enums.QualityPrioritization
import core.domain.camera.enums.TorchMode
import core.domain.camera.plugins.CameraPlugin
import core.domain.camera.result.ImageCaptureResult
import core.domain.camera.video.VideoCaptureResult
import core.domain.camera.video.VideoConfiguration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.bytedeco.ffmpeg.global.avcodec
import org.bytedeco.javacv.FFmpegFrameRecorder
import org.bytedeco.javacv.FrameGrabber
import org.bytedeco.javacv.Java2DFrameConverter
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.imageio.ImageIO

/**
 * Interface defining the core functionalities of the CameraController.
 */
actual class CameraController(
    internal var plugins: MutableList<CameraPlugin>,
    private val imageFormat: ImageFormat,
    private val directory: Directory,
    private val horizontalFlip: Boolean = false,
    private val customGrabber: FrameGrabber? = null,
    private val targetResolution: Pair<Int, Int>? = null,
) {
    actual val usesPhotoCaptureForVideoThumbnail: Boolean = false

    private var cameraGrabber: CameraGrabber? = null
    private val frameChannel = Channel<BufferedImage>(Channel.CONFLATED)

    /**
     * Fan-out of camera frames for plugins and other non-preview consumers.
     *
     * Uses `replay = 0` and `extraBufferCapacity = 1` with `DROP_OLDEST` overflow — slow
     * consumers see the most-recent frame instead of blocking the grabber loop. Emissions
     * happen on the grabber's IO dispatcher in [CameraGrabber.start], in addition to
     * [frameChannel] (which the preview continues to consume).
     */
    private val frameFlow: MutableSharedFlow<BufferedImage> = MutableSharedFlow(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    private val qualityPriority: QualityPrioritization = QualityPrioritization.QUALITY

    // Video recording
    private var frameRecorder: FFmpegFrameRecorder? = null

    @Volatile private var isCurrentlyRecording = false

    @Volatile private var isPausedRecording = false
    private var recordingJob: Job? = null
    private var recordingOutputPath: String? = null
    private var recordingStartMs: Long = 0L

    actual var onPreviewTapListener: ((Float, Float) -> Unit)? = null
    actual var onPreviewDoubleTapListener: (() -> Unit)? = null
    actual var shouldSuppressTapToFocus: ((Float, Float) -> Boolean)? = null

    private var listener: (ByteArray) -> Unit = {
        // default no-op listener
    }

    /**
     * Captures an image.
     *
     * @return The result of the image capture operation.
     */
    @Deprecated(
        message = "Use takePictureToFile() instead for better performance",
        replaceWith = ReplaceWith("takePictureToFile()"),
        level = DeprecationLevel.WARNING,
    )
    actual suspend fun takePicture(): ImageCaptureResult {
        TODO("Not yet implemented")
    }

    actual suspend fun takePictureToFile(): ImageCaptureResult {
        return withContext(Dispatchers.IO) {
            val currentImage = cameraGrabber?.grabCurrentFrame()

            if (currentImage == null) {
                return@withContext ImageCaptureResult.Error(IllegalStateException("No image available"))
            }

            val outputStream = ByteArrayOutputStream()
            return@withContext try {
                ImageIO.write(currentImage, "jpg", outputStream)
                listener(outputStream.toByteArray())
                ImageCaptureResult.Success(outputStream.toByteArray())
            } catch (e: Exception) {
                e.printStackTrace()
                ImageCaptureResult.Error(e)
            } finally {
                outputStream.close()
            }
        }
    }

    /**
     * Toggles the flash mode between ON, OFF, and AUTO.
     */
    actual fun toggleFlashMode() {
        // flash mode not available on desktop
    }

    /**
     * Sets the flash mode of the camera
     *
     * @param mode The desired [FlashMode]
     */
    actual fun setFlashMode(mode: FlashMode) {
        // flash mode not available on desktop
    }

    /**
     * @return the current [FlashMode] of the camera, if available
     */
    actual fun getFlashMode(): FlashMode? = FlashMode.OFF

    /**
     * Toggles the torch mode between ON, OFF, and AUTO.
     *
     * Note: Torch is not available on desktop hardware.
     */
    actual fun toggleTorchMode() {
        // torch not available on desktop
    }

    /**
     * Sets the torch mode of the camera
     *
     * @param mode The desired [TorchMode]
     *
     * Note: Torch is not available on desktop hardware.
     */
    actual fun setTorchMode(mode: TorchMode) {
        // torch not available on desktop
    }

    /**
     * Gets the current torch mode.
     *
     * @return null as Desktop doesn't support torch mode
     */
    actual fun getTorchMode(): TorchMode? = null

    /**
     * Toggles the camera lens between FRONT and BACK.
     */
    actual fun toggleCameraLens() {
        // camera lens not available on desktop
    }

    /**
     * Gets the current camera lens.
     *
     * @return null as Desktop doesn't support camera lens switching
     */
    actual fun getCameraLens(): CameraLens? = null

    /**
     * Gets the current image format.
     *
     * @return The configured [ImageFormat]
     */
    actual fun getImageFormat(): ImageFormat = imageFormat

    /**
     * Gets the current quality prioritization setting.
     *
     * @return The configured [QualityPrioritization] (defaults to QUALITY on Desktop)
     */
    actual fun getQualityPrioritization(): QualityPrioritization = qualityPriority

    /**
     * Gets the current camera device type.
     *
     * @return The configured [CameraDeviceType] (always DEFAULT on Desktop)
     */
    actual fun getPreferredCameraDeviceType(): CameraDeviceType = CameraDeviceType.DEFAULT

    actual fun setPreferredCameraDeviceType(deviceType: CameraDeviceType) {
        // No-op on desktop — single camera
    }

    /**
     * Sets the zoom level.
     *
     * Note: Zoom is not supported on Desktop.
     */
    actual fun setZoom(zoomRatio: Float) {
        // zoom not available on desktop
    }

    /**
     * Gets the current zoom ratio.
     *
     * @return 1.0 as Desktop doesn't support zoom
     */
    actual fun getZoom(): Float = 1.0f

    /**
     * Gets the maximum zoom ratio.
     *
     * @return 1.0 as Desktop doesn't support zoom
     */
    actual fun getMaxZoom(): Float = 1.0f

    /**
     * Tap-to-focus not supported on Desktop.
     */
    actual fun setFocusPoint(normalizedX: Float, normalizedY: Float) {}

    actual fun getExposureCompensationRange(): Pair<Int, Int> = Pair(0, 0)

    actual fun setExposureCompensationIndex(index: Int) {}

    actual fun getExposureCompensationIndex(): Int = 0

    /**
     * Starts the camera session.
     */
    actual fun startSession() {
        CoroutineScope(Dispatchers.Default).launch {
            // If there is a custom grabber, use it, else use the default camera grabber
            // Which attempts to use the default camera
            cameraGrabber = CameraGrabber(
                frameChannel = frameChannel,
                errorHandler = {
                    System.err.println("CameraK: Camera error: ${it.message}")
                    it.printStackTrace()
                },
                targetResolution = targetResolution,
                frameFlow = frameFlow,
            ).apply {
                setHorizontalFlip(horizontalFlip)
                start(this@launch, customGrabber)
            }
        }
    }

    /**
     * Stops the camera session.
     */
    actual fun stopSession() {
        cameraGrabber?.stop()
        frameChannel.close()
    }

    /**
     * Adds a listener for image capture events.
     *
     * @param listener The listener to add, receiving image data as [ByteArray].
     */
    actual fun addImageCaptureListener(listener: (ByteArray) -> Unit) {
        this.listener = listener
    }

    actual fun setPreviewStabilizationEnabled(enabled: Boolean) {}

    actual fun applyCaptureModeSessionPreset(isVideoMode: Boolean) {}

    actual fun isNightModeSupported(): Boolean = false

    actual fun setNightMode(enabled: Boolean) {}

    actual fun setWideSelfieMode(enabled: Boolean) {}

    actual fun isWideSelfieEnabled(): Boolean = false

    actual fun initializeControllerPlugins() {
        plugins.forEach {
            it.initialize(this)
        }
    }

    actual fun cleanup() {
        stopVideoRecorderIfActive()
        cameraGrabber?.stop()
        frameChannel.close()
    }

    fun getFrameChannel() = frameChannel

    /**
     * Read-only view of the frame fan-out flow. Intended for plugins
     * (e.g. [core.domain.camera.scanner.QrScannerPlugin]) that need to observe the live
     * camera stream without competing with the preview's channel consumer.
     *
     * The flow has `replay = 0`; plugins only see frames emitted after they start
     * collecting. If no camera session is active, the flow is simply idle.
     */
    fun getFrameFlow(): SharedFlow<BufferedImage> = frameFlow.asSharedFlow()


    actual suspend fun captureRecordingThumbnailFrame(): ImageBitmap? = null

    actual suspend fun extractVideoThumbnailFromFile(filePath: String, isFrontCamera: Boolean): ImageBitmap? = null

    actual suspend fun startRecording(configuration: VideoConfiguration): String = withContext(Dispatchers.IO) {
        val outputPath = createVideoOutputPath(configuration)
        recordingOutputPath = outputPath

        System.err.println(
            "CameraK: desktop videoFpsProbe: no hardware probe — using 60fps recorder timeline and ~16ms frame pacing",
        )

        val recorder = FFmpegFrameRecorder(
            outputPath,
            configuration.quality.width,
            configuration.quality.height,
            if (configuration.enableAudio) 1 else 0,
        ).apply {
            videoCodec = avcodec.AV_CODEC_ID_H264
            format = "mp4"
            frameRate = 60.0
            videoBitrate = configuration.quality.bitrateBps
            if (configuration.enableAudio) {
                audioCodec = avcodec.AV_CODEC_ID_AAC
                sampleRate = 44100
                audioBitrate = 128_000
            }
            start()
        }
        frameRecorder = recorder
        isCurrentlyRecording = true
        isPausedRecording = false
        recordingStartMs = System.currentTimeMillis()

        // Launch recording coroutine that grabs frames independently
        recordingJob = CoroutineScope(Dispatchers.IO).launch {
            val converter = Java2DFrameConverter()
            try {
                while (isActive && isCurrentlyRecording) {
                    if (!isPausedRecording) {
                        val frame = cameraGrabber?.grabCurrentFrame()
                        if (frame != null) {
                            try {
                                val videoFrame = converter.convert(frame)
                                recorder.record(videoFrame)
                            } catch (e: Exception) {
                                System.err.println("CameraK recording frame error: ${e.message}")
                            }
                        }
                    }
                    delay(16L) // ~60fps pacing (camera may deliver fewer frames on some hardware)
                }
            } catch (e: Exception) {
                System.err.println("CameraK recording loop error: ${e.message}")
            }
        }

        outputPath
    }

    actual suspend fun stopRecording(): VideoCaptureResult = withContext(Dispatchers.IO) {
        isCurrentlyRecording = false
        recordingJob?.cancel()
        recordingJob = null
        val durationMs = System.currentTimeMillis() - recordingStartMs
        return@withContext try {
            frameRecorder?.stop()
            frameRecorder?.release()
            frameRecorder = null
            VideoCaptureResult.Success(recordingOutputPath ?: "", durationMs)
        } catch (e: Exception) {
            VideoCaptureResult.Error(e)
        }
    }

    actual suspend fun pauseRecording() {
        isPausedRecording = true
    }

    actual suspend fun resumeRecording() {
        isPausedRecording = false
    }

    private fun stopVideoRecorderIfActive() {
        if (isCurrentlyRecording) {
            isCurrentlyRecording = false
            recordingJob?.cancel()
            recordingJob = null
            try {
                frameRecorder?.stop()
                frameRecorder?.release()
            } catch (e: Exception) {
                System.err.println("CameraK: Error stopping recorder: ${e.message}")
            }
            frameRecorder = null
        }
    }

    private fun createVideoOutputPath(config: VideoConfiguration): String {
        val timestamp = LocalDateTime.now()
            .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
        val dir = if (config.outputDirectory != null) {
            File(config.outputDirectory).also { it.mkdirs() }
        } else {
            File("captured_videos").also { it.mkdirs() }
        }
        return File(dir, "${config.filePrefix}_$timestamp.mp4").absolutePath
    }
}
