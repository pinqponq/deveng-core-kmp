package core.domain.camera.controller

import androidx.compose.ui.graphics.ImageBitmap
import core.domain.camera.SharedImage
import core.domain.camera.enums.AspectRatio
import core.domain.camera.enums.CameraDeviceType
import core.domain.camera.enums.CameraLens
import core.domain.camera.enums.Directory
import core.domain.camera.enums.FlashMode
import core.domain.camera.enums.ImageFormat
import core.domain.camera.enums.QualityPrioritization
import core.domain.camera.enums.TorchMode
import core.domain.camera.plugins.CameraPlugin
import core.domain.camera.result.ImageCaptureResult
import core.domain.camera.utils.MemoryManager
import core.domain.camera.utils.fixOrientation
import core.domain.camera.utils.toByteArray
import core.domain.camera.utils.toUIImage
import core.util.bytearray.toImageBitmap
import core.domain.camera.utils.toCapturePreviewImageBitmap
import core.domain.camera.ios.IosFrontCameraVideoBridge
import core.domain.camera.ios.IosPreviewDbgLog
import core.domain.camera.ios.applyClampedPreviewLayerFrame
import core.domain.camera.ios.clampedPreviewLayerFrameForView
import core.domain.camera.video.VideoCaptureResult
import core.domain.camera.video.VideoConfiguration
import core.domain.camera.video.VideoQuality
import kotlinx.atomicfu.atomic
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCAction
import kotlinx.cinterop.autoreleasepool
import kotlinx.cinterop.useContents
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.AVFoundation.AVCaptureDeviceInput
import platform.AVFoundation.AVCaptureFlashMode
import platform.AVFoundation.AVCaptureFlashModeAuto
import platform.AVFoundation.AVCaptureFlashModeOff
import platform.AVFoundation.AVCaptureFlashModeOn
import platform.AVFoundation.AVCaptureMetadataOutput
import platform.AVFoundation.AVCaptureMetadataOutputObjectsDelegateProtocol
import platform.AVFoundation.AVCaptureMovieFileOutput
import platform.AVFoundation.AVCaptureOutput
import platform.AVFoundation.AVCaptureSessionPreset1280x720
import platform.AVFoundation.AVCaptureSessionPreset1920x1080
import platform.AVFoundation.AVCaptureSessionPreset3840x2160
import platform.AVFoundation.AVCaptureSessionPreset640x480
import platform.AVFoundation.AVCaptureTorchMode
import platform.AVFoundation.AVCaptureTorchModeAuto
import platform.AVFoundation.AVCaptureTorchModeOff
import platform.AVFoundation.AVCaptureTorchModeOn
import platform.AVFoundation.AVCaptureVideoOrientation
import platform.AVFoundation.AVCaptureVideoOrientationLandscapeLeft
import platform.AVFoundation.AVCaptureVideoOrientationLandscapeRight
import platform.AVFoundation.AVCaptureVideoOrientationPortrait
import platform.AVFoundation.AVCaptureVideoOrientationPortraitUpsideDown
import platform.AVFoundation.AVMediaTypeAudio
import platform.Foundation.NSData
import platform.Foundation.NSDate
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.timeIntervalSince1970
import platform.Photos.PHAssetChangeRequest
import platform.Photos.PHPhotoLibrary
import platform.Foundation.NSSelectorFromString
import platform.UIKit.UIDevice
import platform.UIKit.UIDeviceOrientation
import platform.UIKit.UIScreen
import platform.UIKit.UIGestureRecognizerStateBegan
import platform.UIKit.UIGestureRecognizerStateChanged
import platform.UIKit.UIImagePNGRepresentation
import platform.UIKit.UIPinchGestureRecognizer
import platform.UIKit.UITapGestureRecognizer
import platform.UIKit.UIViewController
import platform.darwin.DISPATCH_QUEUE_PRIORITY_DEFAULT
import platform.darwin.DISPATCH_QUEUE_PRIORITY_HIGH
import platform.darwin.DISPATCH_TIME_NOW
import platform.darwin.dispatch_after
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_global_queue
import platform.darwin.dispatch_get_main_queue
import platform.darwin.dispatch_time
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.random.Random

/**
 * Max interval between taps for double-tap; single-tap focus runs after this delay at most.
 * Same 0.20s window as Android `CameraZoomGestureView` companion `DOUBLE_TAP_MAX_INTERVAL_SEC`.
 */
private const val DOUBLE_TAP_MAX_INTERVAL_SEC = 0.20

actual class CameraController(
    internal var flashMode: FlashMode,
    internal var torchMode: TorchMode,
    internal var cameraLens: CameraLens,
    internal var imageFormat: ImageFormat,
    internal var qualityPriority: QualityPrioritization,
    internal var directory: Directory,
    internal var cameraDeviceType: CameraDeviceType,
    internal var aspectRatio: AspectRatio,
    internal var returnFilePath: Boolean,
    internal var plugins: MutableList<CameraPlugin>,
    internal var targetResolution: Pair<Int, Int>? = null,
    internal var targetResolutionFront: Pair<Int, Int>? = null,
) : UIViewController(null, null) {
    actual val usesPhotoCaptureForVideoThumbnail: Boolean = false

    private var isCapturing = atomic(false)
    private val customCameraController = CustomCameraController(
        qualityPrioritization = qualityPriority,
        initialCameraLens = cameraLens,
        aspectRatio = aspectRatio,
        targetResolutionBack = targetResolution,
        targetResolutionFront = targetResolutionFront,
    )
    private var imageCaptureListeners = mutableListOf<(ByteArray) -> Unit>()
    private var metadataOutput = AVCaptureMetadataOutput()
    private var metadataObjectsDelegate: AVCaptureMetadataOutputObjectsDelegateProtocol? = null

    // Video recording
    private var movieFileOutput: AVCaptureMovieFileOutput? = null
    private var videoRecordingDelegate: VideoRecordingDelegate? = null
    private var videoOutputFilePath: String? = null

    /** Lens of the clip waiting for [applyRecordingPostProcessing]; a chained recording may already be running. */
    private var stoppedRecordingUsedFrontLens: Boolean = false

    actual var onPreviewTapListener: ((Float, Float) -> Unit)? = null
    actual var onPreviewDoubleTapListener: (() -> Unit)? = null
    actual var shouldSuppressTapToFocus: ((Float, Float) -> Boolean)? = null

    private val memoryManager = MemoryManager
    private val pendingCaptures = atomic(0)
    private val maxConcurrentCaptures = 3

    /**
     * Single vs double tap: UIKit's [requireGestureRecognizerToFail] keeps single-tap focus correct
     * but delays one-tap focus by the system double-tap timeout (~350ms). We use one 1-tap
     * recognizer and resolve in software: second tap within this window switches lens immediately;
     * otherwise focus runs after this delay (shorter than the default wait).
     */
    private var previewTapSequence = 0L
    private var lastPreviewTapTimeSec = 0.0
    private var lastPreviewTapNx = 0f
    private var lastPreviewTapNy = 0f
    private var lastPreviewDebugLayoutKey: String? = null

    override fun viewDidLoad() {
        super.viewDidLoad()

        memoryManager.initialize()
        IosPreviewDbgLog.logSafe {
            customCameraController.describePreviewPipeline("VC_viewDidLoad", view)
        }
        setupCamera()
        setupNativeGestureRecognizers()
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun setupNativeGestureRecognizers() {
        val singleTap = UITapGestureRecognizer(
            target = this,
            action = NSSelectorFromString("handleNativeSingleTap:"),
        )
        singleTap.numberOfTapsRequired = 1u

        val pinch = UIPinchGestureRecognizer(
            target = this,
            action = NSSelectorFromString("handleNativePinch:"),
        )

        view.addGestureRecognizer(singleTap)
        view.addGestureRecognizer(pinch)
    }

    @ObjCAction
    @OptIn(ExperimentalForeignApi::class)
    fun handleNativeSingleTap(sender: UITapGestureRecognizer) {
        val viewWidth = view.bounds.useContents { size.width }
        val viewHeight = view.bounds.useContents { size.height }
        if (viewWidth <= 0.0 || viewHeight <= 0.0) return
        val location = sender.locationInView(view)
        val nx = (location.useContents { x } / viewWidth).toFloat().coerceIn(0f, 1f)
        val ny = (location.useContents { y } / viewHeight).toFloat().coerceIn(0f, 1f)

        val now = NSDate().timeIntervalSince1970
        val prevTime = lastPreviewTapTimeSec
        if (prevTime > 0.0 && now - prevTime <= DOUBLE_TAP_MAX_INTERVAL_SEC) {
            previewTapSequence++
            lastPreviewTapTimeSec = 0.0
            onPreviewDoubleTapListener?.invoke()
            return
        }

        lastPreviewTapTimeSec = now
        lastPreviewTapNx = nx
        lastPreviewTapNy = ny
        previewTapSequence++
        val scheduledSeq = previewTapSequence
        val delayNs = (DOUBLE_TAP_MAX_INTERVAL_SEC * 1_000_000_000.0).toLong()
        dispatch_after(dispatch_time(DISPATCH_TIME_NOW, delayNs), dispatch_get_main_queue()) {
            if (scheduledSeq != previewTapSequence) return@dispatch_after
            lastPreviewTapTimeSec = 0.0
            if (shouldSuppressTapToFocus?.invoke(lastPreviewTapNx, lastPreviewTapNy) == true) return@dispatch_after
            setFocusPoint(lastPreviewTapNx, lastPreviewTapNy)
            onPreviewTapListener?.invoke(lastPreviewTapNx, lastPreviewTapNy)
        }
    }

    @ObjCAction
    @OptIn(ExperimentalForeignApi::class)
    fun handleNativePinch(sender: UIPinchGestureRecognizer) {
        val state = sender.state
        if (state != UIGestureRecognizerStateBegan && state != UIGestureRecognizerStateChanged) return
        val scale = sender.scale.toFloat()
        sender.setScale(1.0)
        val currentZoom = getZoom()
        val maxZoom = getMaxZoom().coerceAtLeast(1f)
        val newZoom = (currentZoom * scale).coerceIn(1f, maxZoom)
        setZoom(newZoom)
    }

    override fun viewWillAppear(animated: Boolean) {
        super.viewWillAppear(animated)
        memoryManager.updateMemoryStatus()
        customCameraController.setZoom(1f)
        applyClampedPreviewLayerFrameIfNeeded(reason = "viewWillAppear")
        IosPreviewDbgLog.logSafe {
            customCameraController.describePreviewPipeline("VC_viewWillAppear", view, "animated=$animated")
        }
    }

    override fun viewDidAppear(animated: Boolean) {
        super.viewDidAppear(animated)
        dispatch_async(dispatch_get_main_queue()) {
            applyClampedPreviewLayerFrameIfNeeded(reason = "viewDidAppear_deferred")
        }
        IosPreviewDbgLog.logSafe {
            customCameraController.describePreviewPipeline("VC_viewDidAppear", view, "animated=$animated")
        }
    }

    override fun viewDidDisappear(animated: Boolean) {
        super.viewDidDisappear(animated)
        IosPreviewDbgLog.logSafe {
            customCameraController.describePreviewPipeline("VC_viewDidDisappear", view, "animated=$animated")
        }
        lastPreviewDebugLayoutKey = null
        memoryManager.clearBufferPools()
    }

    fun getCameraPreviewLayer() = customCameraController.cameraPreviewLayer

    /** Preview diagnostics for Compose interop lifecycle ([CameraPreviewView]). */
    fun logPreviewDebug(phase: String, extra: String = "") {
        IosPreviewDbgLog.logSafe {
            customCameraController.describePreviewPipeline(phase, view, extra)
        }
    }

    /**
     * Returns whether the capture session is ready for use.
     * Used by plugins to check if they can add outputs.
     */
    fun isSessionReady(): Boolean = customCameraController.captureSession != null

    /**
     * Queues a configuration change to be applied atomically (plugin outputs, etc).
     * Used by plugins (OCR, QRScanner) to safely add outputs without crashes.
     */
    fun queueConfigurationChange(change: () -> Unit) {
        customCameraController.queueConfigurationChange(change)
    }

    /**
     * Safely adds an output within the queued configuration block.
     */
    fun safeAddOutput(output: AVCaptureOutput) {
        customCameraController.safeAddOutput(output)
    }

    internal fun currentVideoOrientation(): AVCaptureVideoOrientation {
        val orientation = UIDevice.currentDevice.orientation
        return when (orientation) {
            UIDeviceOrientation.UIDeviceOrientationPortrait -> AVCaptureVideoOrientationPortrait
            UIDeviceOrientation.UIDeviceOrientationPortraitUpsideDown -> AVCaptureVideoOrientationPortraitUpsideDown
            UIDeviceOrientation.UIDeviceOrientationLandscapeLeft -> AVCaptureVideoOrientationLandscapeRight
            UIDeviceOrientation.UIDeviceOrientationLandscapeRight -> AVCaptureVideoOrientationLandscapeLeft
            else -> AVCaptureVideoOrientationPortrait
        }
    }

    /**
     * Preview stays mirrored on the front lens for selfie UX; recorded files must not be mirrored
     * (same as still capture — [CustomCameraController.captureImage] uses setVideoMirrored(false)).
     */
    @OptIn(ExperimentalForeignApi::class)
    private fun configureMovieFileOutputVideoConnection() {
        val output = movieFileOutput ?: return
        val connections = output.connections as? platform.Foundation.NSArray ?: return
        val count = connections.count.toInt()
        for (i in 0 until count) {
            val connection = connections.objectAtIndex(i.toULong()) as? platform.AVFoundation.AVCaptureConnection
                ?: continue
            if (connection.isVideoOrientationSupported()) {
                connection.videoOrientation = currentVideoOrientation()
            }
            if (connection.isVideoMirroringSupported()) {
                connection.automaticallyAdjustsVideoMirroring = false
                connection.setVideoMirrored(false)
            }
        }
    }

    private fun setupCamera() {
        customCameraController.onSessionReady = {
            IosPreviewDbgLog.logSafe {
                customCameraController.describePreviewPipeline("SESSION_onSessionReady_BEFORE_SETUP", view)
            }
            try {
                customCameraController.setupPreviewLayer(view)
                IosPreviewDbgLog.logSafe {
                    customCameraController.describePreviewPipeline("SESSION_onSessionReady_AFTER_SETUP", view)
                }
            } catch (e: Exception) {
                platform.Foundation.NSLog("CameraK Error: setupPreviewLayer - ${e.message}")
            }

            try {
                if (customCameraController.captureSession?.canAddOutput(metadataOutput) == true) {
                    customCameraController.captureSession?.addOutput(metadataOutput)
                }
            } catch (e: Exception) {
                platform.Foundation.NSLog("CameraK Error: metadata output - ${e.message}")
            }

            // Add movie file output for video recording
            try {
                val movieOutput = AVCaptureMovieFileOutput()
                if (customCameraController.captureSession?.canAddOutput(movieOutput) == true) {
                    customCameraController.captureSession?.addOutput(movieOutput)
                    movieFileOutput = movieOutput
                    configureMovieFileOutputVideoConnection()
                }
            } catch (e: Exception) {
                platform.Foundation.NSLog("CameraK Error: movie output - ${e.message}")
            }

            // Pre-add audio input so recording starts without session reconfiguration stutter
            addAudioInputIfNeeded()

            startSession()
        }

        customCameraController.onMovieOutputConfigurationNeeded = {
            configureMovieFileOutputVideoConnection()
        }

        try {
            customCameraController.setupSession(cameraDeviceType)
        } catch (e: Exception) {
            platform.Foundation.NSLog("CameraK Error: setupSession - ${e.message}")
            return
        }

        customCameraController.onPhotoCapture = { image ->
            image?.let {
                processImageCapture(it)
            }
        }

        customCameraController.onError = { error ->
            platform.Foundation.NSLog("CameraK Error: ${error::class.simpleName} - ${error.message ?: "no message"}")
        }
    }

    private fun writeDataToFile(data: NSData, filePath: String): Boolean =
        NSFileManager.defaultManager.createFileAtPath(filePath, data, null)

    private fun processImageCapture(imageData: NSData) {
        dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_HIGH.toLong(), 0u)) {
            autoreleasepool {
                memoryManager.updateMemoryStatus()

                try {
                    val estimatedSize = imageData.length.toInt()
                    val buffer = if (estimatedSize > 0) {
                        memoryManager.getBuffer(estimatedSize)
                    } else {
                        ByteArray(imageData.length.toInt())
                    }

                    val data = imageData.toByteArray(reuseBuffer = buffer)

                    dispatch_async(dispatch_get_main_queue()) {
                        imageCaptureListeners.forEach { it(data) }
                    }

                    if (buffer.size >= estimatedSize) {
                        memoryManager.recycleBuffer(buffer)
                    }
                } catch (e: Exception) {
                    platform.Foundation.NSLog("CameraK: Error processing image data: ${e.message ?: "Unknown error"}")
                }
            }
        }
    }

    fun setMetadataObjectsDelegate(delegate: AVCaptureMetadataOutputObjectsDelegateProtocol) {
        metadataObjectsDelegate = delegate
        metadataOutput.setMetadataObjectsDelegate(delegate, dispatch_get_main_queue())
    }

    fun updateMetadataObjectTypes(newTypes: List<String>) {
        // Note: This is called from queueConfigurationChange, so session may be in config mode
        // Don't check isRunning() - just set the types
        if (metadataOutput.availableMetadataObjectTypes.isNotEmpty()) {
            // Filter to only supported types
            val supportedTypes = newTypes.filter { type ->
                metadataOutput.availableMetadataObjectTypes.contains(type)
            }
            metadataOutput.metadataObjectTypes = supportedTypes
        } else {
            // Metadata output not fully ready yet, try setting all requested types
            metadataOutput.metadataObjectTypes = newTypes
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun applyClampedPreviewLayerFrameIfNeeded(reason: String) {
        val layer = customCameraController.cameraPreviewLayer ?: return
        val rawBounds = view.bounds.useContents { "${size.width.toInt()}x${size.height.toInt()}" }
        val applied = clampedPreviewLayerFrameForView(view)
        applyClampedPreviewLayerFrame(view, layer)
        if (applied.key != lastPreviewDebugLayoutKey) {
            lastPreviewDebugLayoutKey = applied.key
            IosPreviewDbgLog.logSafe {
                customCameraController.describePreviewPipeline(
                    "VC_previewLayerFrame",
                    view,
                    "reason=$reason rawBounds=$rawBounds applied=${applied.key} " +
                        "clampedWidth=${applied.clampedWidth}",
                )
            }
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    override fun viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()
        applyClampedPreviewLayerFrameIfNeeded(reason = "viewDidLayoutSubviews")
    }

    @Deprecated(
        message = "Use takePictureToFile() instead for better performance",
        replaceWith = ReplaceWith("takePictureToFile()"),
        level = DeprecationLevel.WARNING,
    )
    @OptIn(ExperimentalForeignApi::class)
    actual suspend fun takePicture(): ImageCaptureResult = suspendCancellableCoroutine { continuation ->
        // Atomic counter rejection pattern
        if (pendingCaptures.incrementAndGet() > maxConcurrentCaptures) {
            pendingCaptures.decrementAndGet()
            platform.Foundation.NSLog(
                "CameraK: Burst queue full, dropping frame (${pendingCaptures.value} in progress)",
            )
            continuation.resume(ImageCaptureResult.Error(Exception("Burst queue full, capture rejected")))
            return@suspendCancellableCoroutine
        }

        if (!isCapturing.compareAndSet(expect = false, update = true)) {
            pendingCaptures.decrementAndGet()
            continuation.resume(ImageCaptureResult.Error(Exception("Capture in progress")))
            return@suspendCancellableCoroutine
        }

        // Update memory status before capture
        memoryManager.updateMemoryStatus()

        val captureHandler = object {
            var completed = false

            fun process(image: NSData?, error: String?) {
                if (completed) return
                completed = true

                if (image != null) {
                    dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_HIGH.toLong(), 0u)) {
                        try {
                            autoreleasepool {
                                val result = if (returnFilePath) {
                                    // Same as byte-array path: produce bytes + bitmap; app saves via PhotoSaveUtils
                                    when (imageFormat) {
                                        ImageFormat.JPEG -> {
                                            image.toByteArray().let { bytes ->
                                                val bitmap = bytes.toCapturePreviewImageBitmap()
                                                ImageCaptureResult.Success(bytes, bitmap)
                                            }
                                        }
                                        ImageFormat.PNG -> {
                                            val uiImage = image.toUIImage()
                                            val orientedImage = uiImage.fixOrientation()
                                            UIImagePNGRepresentation(orientedImage)?.toByteArray()?.let { bytes ->
                                                val bitmap = bytes.toCapturePreviewImageBitmap()
                                                ImageCaptureResult.Success(bytes, bitmap)
                                            }
                                        }
                                        else -> null
                                    }
                                } else {

                                    when (imageFormat) {
                                        ImageFormat.JPEG -> {

                                            image.toByteArray().let { bytes ->
                                                val bitmap = bytes.toCapturePreviewImageBitmap()
                                                ImageCaptureResult.Success(bytes, bitmap)
                                            }
                                        }
                                        ImageFormat.PNG -> {
                                            // Convert to PNG - fix orientation
                                            val uiImage = image.toUIImage()
                                            val orientedImage = uiImage.fixOrientation()
                                            UIImagePNGRepresentation(orientedImage)?.toByteArray()?.let { bytes ->
                                                val bitmap = bytes.toCapturePreviewImageBitmap()
                                                ImageCaptureResult.Success(bytes, bitmap)
                                            }
                                        }
                                        else -> null
                                    }
                                }

                                isCapturing.value = false
                                pendingCaptures.decrementAndGet()
                                continuation.resume(
                                    result ?: ImageCaptureResult.Error(Exception("Image processing failed")),
                                )
                            }
                        } catch (e: Exception) {
                            isCapturing.value = false
                            pendingCaptures.decrementAndGet()
                            continuation.resume(ImageCaptureResult.Error(e))
                        }
                    }
                } else {
                    isCapturing.value = false
                    pendingCaptures.decrementAndGet()
                    continuation.resume(ImageCaptureResult.Error(Exception(error ?: "Capture failed")))
                }
            }
        }

        customCameraController.onPhotoCapture = { image ->
            captureHandler.process(image, null)
        }

        customCameraController.onError = { error ->
            captureHandler.process(null, error.toString())
        }

        continuation.invokeOnCancellation {
            isCapturing.value = false
            pendingCaptures.decrementAndGet()
            captureHandler.process(null, "Capture cancelled")
        }

        customCameraController.captureImage(quality = 1.0)
    }

    /**
     * Capture returns bytes + bitmap; app saves via [core.util.image.PhotoSaveUtils.savePhoto].
     */
    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
    actual suspend fun takePictureToFile(): ImageCaptureResult = suspendCancellableCoroutine { continuation ->
        if (pendingCaptures.incrementAndGet() > maxConcurrentCaptures) {
            pendingCaptures.decrementAndGet()
            platform.Foundation.NSLog("CameraK: Burst queue full, dropping frame")
            continuation.resume(ImageCaptureResult.Error(Exception("Burst queue full, capture rejected")))
            return@suspendCancellableCoroutine
        }

        if (!isCapturing.compareAndSet(expect = false, update = true)) {
            pendingCaptures.decrementAndGet()
            continuation.resume(ImageCaptureResult.Error(Exception("Capture in progress")))
            return@suspendCancellableCoroutine
        }

        val captureHandler = object {
            var completed = false

            fun process(image: NSData?, error: String?) {
                if (completed) return
                completed = true

                if (image != null) {
                    dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_HIGH.toLong(), 0u)) {
                        try {
                            autoreleasepool {
                                val result = try {
                                    when (imageFormat) {
                                        ImageFormat.JPEG -> {
                                            image.toByteArray().let { bytes ->
                                                val bitmap = bytes.toCapturePreviewImageBitmap()
                                                ImageCaptureResult.Success(bytes, bitmap)
                                            }
                                        }
                                        ImageFormat.PNG -> {
                                            val uiImage = image.toUIImage()
                                            val orientedImage = uiImage.fixOrientation()
                                            UIImagePNGRepresentation(orientedImage)?.toByteArray()?.let { bytes ->
                                                val bitmap = bytes.toCapturePreviewImageBitmap()
                                                ImageCaptureResult.Success(bytes, bitmap)
                                            }
                                        }
                                        else -> null
                                    }
                                } catch (e: Exception) {
                                    platform.Foundation.NSLog("CameraK: takePictureToFile error: ${e.message ?: "Unknown"}")
                                    ImageCaptureResult.Error(e)
                                }

                                isCapturing.value = false
                                pendingCaptures.decrementAndGet()
                                continuation.resume(
                                    result ?: ImageCaptureResult.Error(Exception("Unsupported image format")),
                                )
                            }
                        } catch (e: Exception) {
                            isCapturing.value = false
                            pendingCaptures.decrementAndGet()
                            continuation.resume(ImageCaptureResult.Error(e))
                        }
                    }
                } else {
                    isCapturing.value = false
                    pendingCaptures.decrementAndGet()
                    continuation.resume(ImageCaptureResult.Error(Exception(error ?: "Capture failed")))
                }
            }
        }

        customCameraController.onPhotoCapture = { image ->
            captureHandler.process(image, null)
        }

        customCameraController.onError = { error ->
            captureHandler.process(null, error.toString())
        }

        continuation.invokeOnCancellation {
            isCapturing.value = false
            pendingCaptures.decrementAndGet()
            captureHandler.process(null, "Capture cancelled")
        }

        customCameraController.captureImage(quality = 1.0)
    }

    actual fun toggleFlashMode() {
        // Same as Android: only ON and OFF (no AUTO)
        flashMode = when (flashMode) {
            FlashMode.ON -> FlashMode.OFF
            else -> FlashMode.ON  // OFF or AUTO -> ON
        }
        customCameraController.setFlashMode(flashMode.toAVCaptureFlashMode())
    }

    actual fun setFlashMode(mode: FlashMode) {
        flashMode = mode
        customCameraController.setFlashMode(mode.toAVCaptureFlashMode())
    }

    actual fun getFlashMode(): FlashMode? {
        // Same as Android: only report ON or OFF (map AUTO to OFF on iOS)
        fun AVCaptureFlashMode.toCameraKFlashMode(): FlashMode? = when (this) {
            AVCaptureFlashModeOn -> FlashMode.ON
            AVCaptureFlashModeOff -> FlashMode.OFF
            AVCaptureFlashModeAuto -> FlashMode.OFF  // iOS treats AUTO as OFF for parity with Android
            else -> null
        }

        return customCameraController.flashMode.toCameraKFlashMode()
    }

    actual fun toggleTorchMode() {
        torchMode = when (torchMode) {
            TorchMode.OFF -> TorchMode.ON
            TorchMode.ON -> TorchMode.AUTO
            TorchMode.AUTO -> TorchMode.OFF
        }
        customCameraController.setTorchMode(torchMode.toAVCaptureTorchMode())
    }

    actual fun setTorchMode(mode: TorchMode) {
        torchMode = mode
        customCameraController.setTorchMode(mode.toAVCaptureTorchMode())
    }

    actual fun getTorchMode(): TorchMode? = torchMode

    actual fun setZoom(zoomRatio: Float) {
        customCameraController.setZoom(zoomRatio)
    }

    actual fun getZoom(): Float = customCameraController.getZoom()

    actual fun getMaxZoom(): Float = customCameraController.getMaxZoom()

    actual fun setFocusPoint(normalizedX: Float, normalizedY: Float) {
        customCameraController.setFocusPoint(normalizedX, normalizedY)
    }

    actual fun getExposureCompensationRange(): Pair<Int, Int> =
        customCameraController.getExposureCompensationRange()

    actual fun setExposureCompensationIndex(index: Int) {
        customCameraController.setExposureTargetBias(index)
    }

    actual fun getExposureCompensationIndex(): Int =
        customCameraController.getExposureTargetBias()

    actual fun toggleCameraLens() {
        memoryManager.updateMemoryStatus()

        if (memoryManager.isUnderMemoryPressure()) {
            memoryManager.clearBufferPools()
        }

        customCameraController.switchCamera()
        configureMovieFileOutputVideoConnection()
        IosPreviewDbgLog.logSafe {
            customCameraController.describePreviewPipeline("UI_toggleCameraLens_AFTER", view)
        }
        // Sync with actual state (controller may have restored previous lens or succeeded on retry)
        cameraLens = customCameraController.getCurrentLens()
        if (cameraLens == CameraLens.FRONT) {
            setFlashMode(FlashMode.OFF)
        }
    }

    actual fun getCameraLens(): CameraLens? = customCameraController.getCurrentLens()

    actual fun getImageFormat(): ImageFormat = imageFormat

    actual fun getQualityPrioritization(): QualityPrioritization = qualityPriority

    actual fun getPreferredCameraDeviceType(): CameraDeviceType = cameraDeviceType

    actual fun setPreferredCameraDeviceType(deviceType: CameraDeviceType) {
        cameraDeviceType = deviceType
        customCameraController.switchToDeviceType(deviceType)
    }

    actual fun startSession() {
        // Note: The actual session start happens in onSessionReady callback (setupCamera).
        // This method is called from CameraKStateHolder.initialize() which may be before
        // viewDidLoad() triggers setupCamera(). The session will start automatically
        // when ready, so we only need to handle the case where session IS ready.
        if (customCameraController.captureSession != null) {
            memoryManager.clearBufferPools()
            customCameraController.startSession()
            initializeControllerPlugins()
        }
        // If captureSession is null, onSessionReady will call startSession() when ready
    }

    actual fun stopSession() {
        IosPreviewDbgLog.logSafe {
            customCameraController.describePreviewPipeline("SESSION_stopSession", view)
        }
        customCameraController.stopSession()
    }

    actual fun addImageCaptureListener(listener: (ByteArray) -> Unit) {
        imageCaptureListeners.add(listener)
    }

    actual fun setPreviewStabilizationEnabled(enabled: Boolean) {}

    actual fun applyCaptureModeSessionPreset(isVideoMode: Boolean) {
        IosPreviewDbgLog.logSafe {
            customCameraController.describePreviewPipeline(
                "UI_applyCaptureModeSessionPreset",
                view,
                "isVideoMode=$isVideoMode",
            )
        }
        customCameraController.applySessionPresetForCaptureMode(isVideoMode)
    }

    actual fun isNightModeSupported(): Boolean = false

    actual fun setNightMode(enabled: Boolean) {}

    actual fun setWideSelfieMode(enabled: Boolean) {
        // iOS: no vendor-specific FOV expansion equivalent. AVFoundation already exposes
        // ultra-wide via setPreferredCameraDeviceType(ULTRA_WIDE) on supported devices.
    }

    actual fun isWideSelfieEnabled(): Boolean = false

    actual fun initializeControllerPlugins() {
        plugins.forEach {
            it.initialize(this)
        }
    }

    actual fun cleanup() {
        IosPreviewDbgLog.logSafe {
            customCameraController.describePreviewPipeline("SESSION_cleanup", view)
        }
        movieFileOutput = null
        videoRecordingDelegate = null
        customCameraController.cleanupSession()
        memoryManager.clearBufferPools()
    }

    actual suspend fun captureRecordingThumbnailFrame(): ImageBitmap? = null

    actual suspend fun extractVideoThumbnailFromFile(
        filePath: String,
        isFrontCamera: Boolean,
    ): ImageBitmap? =
        core.domain.camera.ios.extractVideoThumbnailFromFile(
            filePath = filePath,
            // [applyRecordingPostProcessing] already unmirror-exports when the bridge is set; flip pixels only as fallback.
            compensateMirroredRecording = isFrontCamera && IosFrontCameraVideoBridge.unmirrorRecordedVideoInPlace == null,
        )

    @OptIn(ExperimentalForeignApi::class)
    actual suspend fun startRecording(configuration: VideoConfiguration): String = suspendCancellableCoroutine { cont ->
        val output = movieFileOutput ?: run {
            cont.resumeWithException(IllegalStateException("Movie file output not available"))
            return@suspendCancellableCoroutine
        }

        if (output.isRecording()) {
            cont.resume(videoOutputFilePath ?: "")
            return@suspendCancellableCoroutine
        }

        // Audio input is pre-added at session setup to avoid stutter
        if (configuration.enableAudio) {
            addAudioInputIfNeeded()
        }

        val filePath = createVideoTempFile(configuration)
        videoOutputFilePath = filePath
        val fileURL = NSURL.fileURLWithPath(filePath)

        // Delete existing file at path if any
        NSFileManager.defaultManager.removeItemAtPath(filePath, null)

        val delegate = VideoRecordingDelegate()
        videoRecordingDelegate = delegate

        configureMovieFileOutputVideoConnection()

        dispatch_async(dispatch_get_main_queue()) {
            output.startRecordingToOutputFileURL(fileURL, recordingDelegate = delegate)
        }

        cont.resume(filePath)
    }

    actual suspend fun stopRecording(): VideoCaptureResult = suspendCancellableCoroutine { cont ->
        val output = movieFileOutput
        val delegate = videoRecordingDelegate

        if (output == null || delegate == null || !output.isRecording()) {
            cont.resume(VideoCaptureResult.Error(Exception("No active recording")))
            return@suspendCancellableCoroutine
        }

        // Read here, not in applyRecordingPostProcessing(): a chained recording can switch the lens
        // before the finished clip is post-processed.
        stoppedRecordingUsedFrontLens = customCameraController.getCurrentLens() == CameraLens.FRONT
        delegate.onFinished = { result ->
            dispatch_async(dispatch_get_main_queue()) {
                cont.resume(result)
            }
        }

        dispatch_async(dispatch_get_main_queue()) {
            output.stopRecording()
        }
    }

    actual suspend fun applyRecordingPostProcessing(
        result: VideoCaptureResult,
    ): VideoCaptureResult = suspendCancellableCoroutine { cont ->
        val recordedWithFrontLens = stoppedRecordingUsedFrontLens
        stoppedRecordingUsedFrontLens = false
        if (!recordedWithFrontLens || result !is VideoCaptureResult.Success) {
            cont.resume(result)
            return@suspendCancellableCoroutine
        }
        dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT.toLong(), 0u)) {
            val unmirrored = IosFrontCameraVideoBridge.unmirrorRecordedVideoInPlace?.invoke(result.filePath) == true
            if (!unmirrored) {
                platform.Foundation.NSLog(
                    "CameraK: front video unmirror failed path=${result.filePath}",
                )
            }
            dispatch_async(dispatch_get_main_queue()) {
                cont.resume(result)
            }
        }
    }

    actual suspend fun pauseRecording() {
        movieFileOutput?.pauseRecording()
    }

    actual suspend fun resumeRecording() {
        movieFileOutput?.resumeRecording()
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun addAudioInputIfNeeded() {
        val session = customCameraController.captureSession ?: return
        // Check if audio input already exists
        val hasAudioInput = session.inputs.any { input ->
            val deviceInput = input as? AVCaptureDeviceInput
            deviceInput?.device?.hasMediaType(AVMediaTypeAudio) == true
        }
        if (hasAudioInput) return

        val audioDevice = platform.AVFoundation.AVCaptureDevice.defaultDeviceWithMediaType(AVMediaTypeAudio)
            ?: return
        val audioInput = AVCaptureDeviceInput.deviceInputWithDevice(audioDevice, null) ?: return

        customCameraController.queueConfigurationChange {
            if (session.canAddInput(audioInput)) {
                session.addInput(audioInput)
            }
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun createVideoTempFile(config: VideoConfiguration): String {
        val timestamp = NSDate().timeIntervalSince1970.toLong()
        val fileName = "${config.filePrefix}_$timestamp.mp4"

        return if (config.outputDirectory != null) {
            val dir = config.outputDirectory
            val fileManager = NSFileManager.defaultManager
            if (!fileManager.fileExistsAtPath(dir)) {
                fileManager.createDirectoryAtPath(
                    dir,
                    withIntermediateDirectories = true,
                    attributes = null,
                    error = null,
                )
            }
            "$dir/$fileName"
        } else {
            val tempDir = platform.Foundation.NSTemporaryDirectory()
            "$tempDir$fileName"
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun createTempFile(): String {
        val timestamp = Random.nextLong()
        val fileName = "IMG_$timestamp.${imageFormat.extension}"

        return when (directory) {
            Directory.PICTURES, Directory.DCIM -> {
                // For Photos library, use temp directory first
                val tempDir = platform.Foundation.NSTemporaryDirectory()
                "$tempDir$fileName"
            }
            Directory.DOCUMENTS -> {
                // For Documents, create permanent location
                val documentsDir = platform.Foundation.NSSearchPathForDirectoriesInDomains(
                    platform.Foundation.NSDocumentDirectory,
                    platform.Foundation.NSUserDomainMask,
                    true,
                ).firstOrNull() as? String ?: platform.Foundation.NSTemporaryDirectory()

                // Create CameraK subdirectory
                val cameraKDir = "$documentsDir/CameraK"
                val fileManager = NSFileManager.defaultManager
                if (!fileManager.fileExistsAtPath(cameraKDir)) {
                    fileManager.createDirectoryAtPath(
                        cameraKDir,
                        withIntermediateDirectories = true,
                        attributes = null,
                        error = null,
                    )
                }

                "$cameraKDir/$fileName"
            }
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun saveToPhotosLibrary(filePath: String, imageData: NSData): String? {
        var savedAssetId: String? = null
        var saveError: String? = null

        // Save to Photos library synchronously (blocking the capture flow)
        val semaphore = platform.darwin.dispatch_semaphore_create(0)

        PHPhotoLibrary.sharedPhotoLibrary().performChanges(
            changeBlock = {
                val creationRequest = PHAssetChangeRequest.creationRequestForAssetFromImageAtFileURL(
                    NSURL.fileURLWithPath(filePath),
                )
                savedAssetId = creationRequest?.placeholderForCreatedAsset?.localIdentifier
            },
            completionHandler = { success, error ->
                if (!success || error != null) {
                    saveError = error?.localizedDescription ?: "Failed to save to Photos"
                    platform.Foundation.NSLog("CameraK: Failed to save to Photos: ${saveError ?: "Unknown error"}")
                }
                platform.darwin.dispatch_semaphore_signal(semaphore)
            },
        )

        platform.darwin.dispatch_semaphore_wait(semaphore, platform.darwin.DISPATCH_TIME_FOREVER)

        // Return the asset identifier or error
        return if (saveError == null && savedAssetId != null) {
            "ph://$savedAssetId" // Use custom scheme to indicate Photos library asset
        } else {
            null
        }
    }

    private fun FlashMode.toAVCaptureFlashMode(): AVCaptureFlashMode = when (this) {
        FlashMode.ON -> AVCaptureFlashModeOn
        FlashMode.OFF -> AVCaptureFlashModeOff
        FlashMode.AUTO -> AVCaptureFlashModeOff  // iOS: only ON/OFF like Android; AUTO applied as OFF
    }

    private fun TorchMode.toAVCaptureTorchMode(): AVCaptureTorchMode = when (this) {
        TorchMode.ON -> AVCaptureTorchModeOn
        TorchMode.OFF -> AVCaptureTorchModeOff
        TorchMode.AUTO -> AVCaptureTorchModeAuto
    }
}
