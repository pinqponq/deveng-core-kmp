package core.domain.camera.controller

import core.domain.camera.ios.IosStillCaptureResolutionPicker
import core.domain.camera.utils.capNSDataJpegToMaxPhotoDimensions
import core.domain.camera.enums.AspectRatio
import core.domain.camera.enums.CameraDeviceType
import core.domain.camera.enums.CameraLens
import core.domain.camera.enums.QualityPrioritization
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.AVFoundation.*
import platform.Foundation.NSArray
import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.Foundation.NSLog
import platform.UIKit.UIDevice
import platform.UIKit.UIScreen
import platform.UIKit.UIView
import platform.darwin.DISPATCH_QUEUE_PRIORITY_HIGH
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_global_queue
import core.domain.camera.ios.IosCaptureVideoOrientation
import core.domain.camera.ios.IosPreviewDbgLog
import core.domain.camera.ios.applyClampedPreviewLayerFrame
import core.domain.camera.ios.clampedPreviewLayerFrameForView
import platform.darwin.dispatch_get_main_queue
import platform.CoreGraphics.CGPointMake
import platform.CoreMedia.CMTimeMake
import platform.CoreMedia.CMVideoFormatDescriptionGetDimensions
import kotlin.collections.emptyList
import kotlin.concurrent.Volatile

/**
 * Convert CameraDeviceType enum to AVFoundation device type string
 */
private fun CameraDeviceType.toAVCaptureDeviceType(): String? = when (this) {
    CameraDeviceType.WIDE_ANGLE -> AVCaptureDeviceTypeBuiltInWideAngleCamera
    CameraDeviceType.TELEPHOTO -> AVCaptureDeviceTypeBuiltInTelephotoCamera
    CameraDeviceType.ULTRA_WIDE -> AVCaptureDeviceTypeBuiltInUltraWideCamera
    CameraDeviceType.MACRO -> null // Macro camera would need iOS 15+ check
    CameraDeviceType.DEFAULT -> AVCaptureDeviceTypeBuiltInWideAngleCamera
}

class CustomCameraController(
    val qualityPrioritization: QualityPrioritization,
    private var initialCameraLens: CameraLens = CameraLens.BACK,
    private val aspectRatio: AspectRatio = AspectRatio.RATIO_9_16,
    private val targetResolutionBack: Pair<Int, Int>? = null,
    private val targetResolutionFront: Pair<Int, Int>? = null,
) : NSObject(),
    AVCapturePhotoCaptureDelegateProtocol {
    var captureSession: AVCaptureSession? = null
    private var backCamera: AVCaptureDevice? = null
    private var frontCamera: AVCaptureDevice? = null
    private var currentCamera: AVCaptureDevice? = null
    private var photoOutput: AVCapturePhotoOutput? = null
    var cameraPreviewLayer: AVCaptureVideoPreviewLayer? = null
    private var isUsingFrontCamera = false

    var onPhotoCapture: ((NSData?) -> Unit)? = null
    var onError: ((CameraException) -> Unit)? = null
    var onSessionReady: (() -> Unit)? = null

    /** Current lens based on actual session state (for sync with wrapper). */
    fun getCurrentLens(): CameraLens = if (isUsingFrontCamera) CameraLens.FRONT else CameraLens.BACK

    var flashMode: AVCaptureFlashMode = AVCaptureFlashModeOff  // iOS: only ON/OFF like Android
    var torchMode: AVCaptureTorchMode = AVCaptureTorchModeAuto

    private var highQualityEnabled = false

    // Configuration queue for plugin outputs (Apple WWDC pattern)
    private val pendingConfigurations = mutableListOf<() -> Unit>()

    @Volatile
    private var isConfiguring = false

    sealed class CameraException(message: String? = null) : Exception(message) {
        class DeviceNotAvailable : CameraException()
        class ConfigurationError(message: String) : CameraException(message)
        class CaptureError(message: String) : CameraException(message)
    }

    /**
     * Sets up the camera session with a specific device type.
     *
     * This allows selecting a particular camera (e.g. wide-angle, telephoto, or macro) at runtime,
     * which is especially useful on iPhones with multiple rear cameras (iPhone 13 and newer).
     *
     * If cameraDeviceType is null or unavailable, falls back to any available camera device.
     *
     * Example device types:
     * - AVCaptureDeviceTypeBuiltInWideAngleCamera
     * - AVCaptureDeviceTypeBuiltInTelephotoCamera
     * - AVCaptureDeviceTypeBuiltInUltraWideCamera
     * - AVCaptureDeviceTypeBuiltInMacroCamera
     */
    fun setupSession(cameraDeviceType: CameraDeviceType = CameraDeviceType.DEFAULT) {
        try {
            // Perform heavy setup off the main thread to reduce UI stalls (#73)
            dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_HIGH.toLong(), 0u)) {
                captureSession = AVCaptureSession()
                captureSession?.beginConfiguration()

                // Start with a fast preset; prefer target resolution if provided
                val initialPreset = effectiveTargetResolution()?.toPreset() ?: AVCaptureSessionPresetHigh
                captureSession?.sessionPreset = initialPreset

                if (!setupInputs(cameraDeviceType)) {
                    NSLog("CameraK Error: setupSession setupInputs returned false -> DeviceNotAvailable")
                    dispatch_async(dispatch_get_main_queue()) {
                        cleanupSession()
                        onError?.invoke(CameraException.DeviceNotAvailable())
                    }
                    return@dispatch_async
                }

                setupPhotoOutput()
                captureSession?.commitConfiguration()

                // Switch to target resolution/aspect ratio preset on main queue once initial setup completes
                dispatch_async(dispatch_get_main_queue()) {
                    applySessionPresetForCaptureMode(captureModeIsVideo)
                    // Prefer stabilization and full-quality capture path unless caller explicitly chose SPEED.
                    highQualityEnabled = qualityPrioritization != QualityPrioritization.SPEED
                    onSessionReady?.invoke()
                }
            }
        } catch (e: CameraException) {
            NSLog("CameraK Error: setupSession caught CameraException: ${e::class.simpleName} - ${e.message}")
            cleanupSession()
            onError?.invoke(e)
        } catch (e: Exception) {
            NSLog("CameraK Error: setupSession caught Exception: ${e.message}")
            cleanupSession()
            onError?.invoke(CameraException.ConfigurationError(e.message ?: "Unknown error"))
        }
    }

    private fun effectiveTargetResolution(): Pair<Int, Int>? {
        val useFront = when {
            currentCamera != null -> isUsingFrontCamera
            else -> initialCameraLens == CameraLens.FRONT
        }
        return if (useFront) {
            targetResolutionFront ?: targetResolutionBack
        } else {
            targetResolutionBack
        }
    }

    /**
     * Maps short/long sides to an AVCaptureSessionPreset tier.
     * [reference] is usually Swift-reported best size under cap; falls back to [cap].
     */
    private fun presetForShortLong(short: Int, long: Int): String? = when {
        long >= 3840 && short >= 2160 -> AVCaptureSessionPreset3840x2160
        long >= 3648 && short >= 2052 -> AVCaptureSessionPreset3840x2160
        long >= 3088 && short >= 1737 -> AVCaptureSessionPresetPhoto ?: AVCaptureSessionPreset3840x2160
        long >= 1920 && short >= 1080 -> AVCaptureSessionPreset1920x1080
        long >= 1280 && short >= 720 -> AVCaptureSessionPreset1280x720
        else -> null
    }

    private fun Pair<Int, Int>.toPreset(): String? {
        val short = minOf(first, second)
        val long = maxOf(first, second)
        return presetForShortLong(short, long)
    }

    private fun resolveSessionPresetForCurrentLens(): String? {
        val cap = effectiveTargetResolution()
        val deviceBest = IosStillCaptureResolutionPicker.logAndPickBest(
            isFrontCamera = isUsingFrontCamera,
            cap = cap,
        )
        val reference = deviceBest ?: cap
        return reference?.toPreset() ?: cap?.toPreset()
    }

    /** Last UI capture mode; used when switching lenses so preset matches Photo vs Video tab. */
    var captureModeIsVideo: Boolean = false

    /** Invoked after session preset changes so movie-file mirroring can be re-applied. */
    var onMovieOutputConfigurationNeeded: (() -> Unit)? = null

    /**
     * Session preset follows explicit Photo / Video UI mode (not record start/stop).
     * Video: 1080p (~1080×1920 portrait). Photo: device best still preset under cap.
     */
    fun applySessionPresetForCaptureMode(isVideoMode: Boolean) {
        IosPreviewDbgLog.logSafe {
            describePreviewPipeline(
                "SESSION_applyCaptureMode_BEFORE",
                view = null,
                extra = "isVideoMode=$isVideoMode previous=$captureModeIsVideo",
            )
        }
        captureModeIsVideo = isVideoMode
        if (isVideoMode) {
            applySessionPresetForVideoMode()
        } else {
            applySessionPresetForPhotoMode()
        }
        IosPreviewDbgLog.logSafe {
            describePreviewPipeline(
                "SESSION_applyCaptureMode_AFTER",
                view = null,
                extra = "isVideoMode=$isVideoMode",
            )
        }
    }

    private fun applySessionPresetForPhotoMode() {
        val preset = resolveSessionPresetForCurrentLens() ?: aspectRatio.toSessionPreset() ?: return
        applySessionPreset(preset, reason = "photoMode")
        restoreVideoFrameDurationToAutomatic()
    }

    private fun applySessionPresetForVideoMode() {
        val videoPreset = AVCaptureSessionPreset1920x1080 ?: AVCaptureSessionPreset1280x720 ?: return
        applySessionPreset(videoPreset, reason = "videoMode")
        applyPreferredVideoRecordingFrameRatePrefer60Else30()
    }

    private fun applySessionPreset(preset: String, reason: String) {
        val session = captureSession ?: return
        session.beginConfiguration()
        if (session.canSetSessionPreset(preset)) {
            session.sessionPreset = preset
            NSLog(
                "CameraK",
                "sessionPreset=$preset reason=$reason lens=${if (isUsingFrontCamera) "front" else "back"}",
            )
        } else {
            NSLog(
                "CameraK",
                "sessionPreset not supported preset=$preset reason=$reason lens=${if (isUsingFrontCamera) "front" else "back"}",
            )
        }
        session.commitConfiguration()
        onMovieOutputConfigurationNeeded?.invoke()
        IosPreviewDbgLog.logSafe {
            describePreviewPipeline(
                "SESSION_applySessionPreset",
                view = null,
                extra = "preset=$preset reason=$reason applied=${session.sessionPreset}",
            )
        }
    }

    private fun setupPhotoOutput() {
        photoOutput = AVCapturePhotoOutput()
        photoOutput?.setHighResolutionCaptureEnabled(false)

        // Always raise the ceiling to QUALITY so per-capture settings can request night mode
        // when the user enables it. Per-capture prioritization still respects qualityPrioritization
        // for non-night captures, so SPEED/BALANCED users see no slowdown.
        photoOutput?.setMaxPhotoQualityPrioritization(AVCapturePhotoQualityPrioritizationQuality)

        if (qualityPrioritization == QualityPrioritization.QUALITY ||
            qualityPrioritization == QualityPrioritization.NONE
        ) {
            photoOutput?.setHighResolutionCaptureEnabled(true)
        }

        photoOutput?.setPreparedPhotoSettingsArray(emptyList<String>(), completionHandler = { settings, error ->
            if (error != null) {
                NSLog("CameraK Error: setupPhotoOutput setPreparedPhotoSettingsArray error: ${error.localizedDescription}")
                onError?.invoke(CameraException.ConfigurationError(error.localizedDescription))
            }
        })

        val canAdd = captureSession?.canAddOutput(photoOutput!!) == true
        if (canAdd) {
            captureSession?.addOutput(photoOutput!!)
        } else {
            NSLog("CameraK Error: setupPhotoOutput Cannot add photo output (canAddOutput=false)")
            throw CameraException.ConfigurationError("Cannot add photo output")
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun setupInputs(cameraDeviceType: CameraDeviceType): Boolean {
        val deviceTypeString = cameraDeviceType.toAVCaptureDeviceType()
        val deviceTypes = deviceTypeString?.let { listOf(it) } ?: listOfNotNull(
            AVCaptureDeviceTypeBuiltInWideAngleCamera,
            AVCaptureDeviceTypeBuiltInTelephotoCamera,
            AVCaptureDeviceTypeBuiltInUltraWideCamera,
        )

        val discoverySession = AVCaptureDeviceDiscoverySession.discoverySessionWithDeviceTypes(
            deviceTypes,
            AVMediaTypeVideo,
            AVCaptureDevicePositionUnspecified,
        )

        val devices = discoverySession.devices.ifEmpty {
            AVCaptureDevice.defaultDeviceWithMediaType(AVMediaTypeVideo)?.let { listOf<Any?>(it) } ?: emptyList()
        }

        devices.forEach { device ->
            val cam = device as AVCaptureDevice
            when (cam.position) {
                AVCaptureDevicePositionBack -> backCamera = cam
                AVCaptureDevicePositionFront -> frontCamera = cam
            }
        }

        fun findByTypeAndPosition(type: String?, position: Long?): AVCaptureDevice? = devices.firstOrNull { dev ->
            val cam = dev as AVCaptureDevice
            (type == null || cam.deviceType == type) && (position == null || cam.position == position)
        } as? AVCaptureDevice

        val requestedType = cameraDeviceType.toAVCaptureDeviceType()
        val desiredPosition = when (initialCameraLens) {
            CameraLens.FRONT -> AVCaptureDevicePositionFront
            CameraLens.BACK -> AVCaptureDevicePositionBack
        }

        currentCamera =
            findByTypeAndPosition(requestedType, desiredPosition)
                ?: findByTypeAndPosition(requestedType, null)
                ?: when (initialCameraLens) {
                    CameraLens.FRONT -> frontCamera ?: backCamera
                    CameraLens.BACK -> backCamera ?: frontCamera
                }
                ?: run {
                    NSLog("CameraK Error: setupInputs no currentCamera found (requestedType=$requestedType desiredPosition=$desiredPosition)")
                    return false
                }

        isUsingFrontCamera = (currentCamera == frontCamera)

        return try {
            val input = AVCaptureDeviceInput.deviceInputWithDevice(currentCamera!!, null)
            if (input == null) {
                NSLog("CameraK Error: setupInputs deviceInputWithDevice(currentCamera) returned null")
                return false
            }

            val canAdd = captureSession?.canAddInput(input) == true
            if (canAdd) {
                captureSession?.addInput(input)
                applyContinuousAutofocusAndExposure(currentCamera)
                true
            } else {
                NSLog("CameraK Error: setupInputs canAddInput returned false")
                false
            }
        } catch (e: Exception) {
            NSLog("CameraK Error: setupInputs exception: ${e.message}")
            throw CameraException.ConfigurationError(e.message ?: "Unknown error")
        }
    }

    /**
     * Queues a configuration change to be applied atomically (Apple WWDC pattern).
     * Used by plugins to safely add outputs without crashing.
     *
     * If session is already running, processes configurations immediately.
     * Otherwise queues for batch processing at startSession().
     *
     * @param change Lambda to execute within beginConfiguration/commitConfiguration block
     */
    fun queueConfigurationChange(change: () -> Unit) {
        pendingConfigurations.add(change)

        // If session is already running, process immediately
        if (captureSession?.isRunning() == true && !isConfiguring) {
            processPendingConfigurations()
        }
    }

    /**
     * Processes all queued configuration changes in a single transaction.
     * Must be called on main thread or after session is ready.
     * Prevents "startRunning may not be called between beginConfiguration and commitConfiguration" crash.
     */
    private fun processPendingConfigurations() {
        if (isConfiguring || pendingConfigurations.isEmpty() || captureSession == null) {
            return
        }

        isConfiguring = true

        try {
            val session = captureSession ?: return

            session.beginConfiguration()

            val changesToApply = pendingConfigurations.toList()
            pendingConfigurations.clear()

            for (change in changesToApply) {
                try {
                    change()
                } catch (e: Exception) {
                    NSLog("CameraK: Error processing configuration change: ${e.message}")
                }
            }

            session.commitConfiguration()
        } finally {
            isConfiguring = false
        }
    }

    /**
     * Safely adds an output to the capture session.
     * Should be called from within queueConfigurationChange block.
     */
    fun safeAddOutput(output: AVCaptureOutput) {
        val session = captureSession
        if (session != null && session.canAddOutput(output)) {
            session.addOutput(output)
        }
    }

    fun startSession() {
        processPendingConfigurations()

        if (captureSession == null) return

        if (captureSession?.isRunning() == false) {
            dispatch_async(
                dispatch_get_global_queue(
                    DISPATCH_QUEUE_PRIORITY_HIGH.toLong(),
                    0u,
                ),
            ) {
                captureSession?.startRunning()
            }
        }
    }

    fun stopSession() {
        if (captureSession?.isRunning() == true) {
            captureSession?.stopRunning()
        }
    }

    private fun AspectRatio.toSessionPreset(): String = when (this) {
        AspectRatio.RATIO_16_9, AspectRatio.RATIO_9_16 -> (
            AVCaptureSessionPreset1920x1080
                ?: AVCaptureSessionPresetPhoto
            )!!
        AspectRatio.RATIO_1_1 -> AVCaptureSessionPresetPhoto!!
        AspectRatio.RATIO_4_3 -> AVCaptureSessionPresetPhoto!!
    }

    fun cleanupSession() {
        IosPreviewDbgLog.logSafe { describePreviewPipeline("SESSION_cleanupSession_BEFORE", view = null) }
        stopSession()
        cameraPreviewLayer?.removeFromSuperlayer()
        cameraPreviewLayer = null
        captureSession = null
        photoOutput = null
        currentCamera = null
        backCamera = null
        frontCamera = null
    }

    @OptIn(ExperimentalForeignApi::class)
    fun setupPreviewLayer(view: UIView) {
        val session = captureSession ?: return
        val existing = cameraPreviewLayer
        if (existing != null) {
            IosPreviewDbgLog.logSafe {
                describePreviewPipeline("PREVIEW_setupPreviewLayer_REMOVE_OLD", view)
            }
            existing.removeFromSuperlayer()
        }

        val videoOrientation = IosCaptureVideoOrientation.refresh(view)
        val newPreviewLayer = AVCaptureVideoPreviewLayer(session = session).apply {
            videoGravity = AVLayerVideoGravityResizeAspectFill
            connection?.videoOrientation = videoOrientation
        }

        view.layer.addSublayer(newPreviewLayer)
        cameraPreviewLayer = newPreviewLayer
        applyClampedPreviewLayerFrame(view, newPreviewLayer)
        val applied = clampedPreviewLayerFrameForView(view)
        IosPreviewDbgLog.logSafe {
            describePreviewPipeline(
                "PREVIEW_setupPreviewLayer_DONE",
                view,
                "appliedFrame=${applied.key} clampedWidth=${applied.clampedWidth}",
            )
        }
    }

    /**
     * Orientation the preview layer and the capture outputs are configured with — see
     * [IosCaptureVideoOrientation] for why it follows the interface and not the device.
     */
    fun currentVideoOrientation(): AVCaptureVideoOrientation = IosCaptureVideoOrientation.current

    fun setFlashMode(mode: AVCaptureFlashMode) {
        val supportedFlashModes = photoOutput?.supportedFlashModes() as? List<*>
        if (supportedFlashModes?.contains(mode) == true) {
            flashMode = mode
        } else {
            platform.Foundation.NSLog("CameraK: Flash mode not supported on this device, using OFF")
            flashMode = AVCaptureFlashModeOff
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    fun setTorchMode(mode: AVCaptureTorchMode) {
        torchMode = mode
        val camera = currentCamera
        camera?.let { cam ->
            if (cam.hasTorch) {
                try {
                    cam.lockForConfiguration(null)
                    cam.torchMode = mode
                    cam.unlockForConfiguration()
                } catch (e: Exception) {
                    NSLog("CameraK Error: setTorchMode exception: ${e.message}")
                    onError?.invoke(CameraException.ConfigurationError("Failed to set torch mode"))
                }
            }
        }
    }

    /**
     * Sets the zoom level smoothly.
     * @param zoomFactor The desired zoom level (1.0 = no zoom)
     */
    @OptIn(ExperimentalForeignApi::class)
    fun setZoom(zoomFactor: Float) {
        currentCamera?.let { camera ->
            val clampedZoom = zoomFactor.coerceIn(1.0f, getMaxZoom())
            try {
                camera.lockForConfiguration(null)
                camera.videoZoomFactor = clampedZoom.toDouble()
                camera.unlockForConfiguration()
                IosPreviewDbgLog.logSafe {
                    describePreviewPipeline(
                        "DEVICE_setZoom",
                        view = null,
                        extra = "requested=$zoomFactor applied=$clampedZoom",
                    )
                }
            } catch (e: Exception) {
                NSLog("CameraK Error: setZoom exception: ${e.message}")
                onError?.invoke(CameraException.ConfigurationError("Failed to set zoom: ${e.message}"))
            }
        }
    }

    /**
     * Gets the current zoom level.
     * @return Current zoom factor (1.0 = no zoom)
     */
    fun getZoom(): Float = currentCamera?.videoZoomFactor?.toFloat() ?: 1.0f

    /**
     * Gets the maximum zoom level supported by the current camera.
     * @return Maximum zoom factor
     */
    fun getMaxZoom(): Float = currentCamera?.activeFormat?.videoMaxZoomFactor?.toFloat() ?: 1.0f

    /**
     * Match system Camera app: continuous AF/AE on preview so the scene stays sharp without tapping.
     * Call after each camera input is attached.
     */
    @OptIn(ExperimentalForeignApi::class)
    private fun applyContinuousAutofocusAndExposure(camera: AVCaptureDevice?) {
        val cam = camera ?: return
        try {
            cam.lockForConfiguration(null)
            if (cam.isFocusPointOfInterestSupported()) {
                cam.focusPointOfInterest = CGPointMake(0.5, 0.5)
            }
            when {
                cam.isFocusModeSupported(AVCaptureFocusModeContinuousAutoFocus) -> {
                    cam.focusMode = AVCaptureFocusModeContinuousAutoFocus
                }
                cam.isFocusModeSupported(AVCaptureFocusModeAutoFocus) -> {
                    cam.focusMode = AVCaptureFocusModeAutoFocus
                }
            }
            if (cam.isExposurePointOfInterestSupported()) {
                cam.exposurePointOfInterest = CGPointMake(0.5, 0.5)
            }
            when {
                cam.isExposureModeSupported(AVCaptureExposureModeContinuousAutoExposure) -> {
                    cam.exposureMode = AVCaptureExposureModeContinuousAutoExposure
                }
                cam.isExposureModeSupported(AVCaptureExposureModeAutoExpose) -> {
                    cam.exposureMode = AVCaptureExposureModeAutoExpose
                }
            }
            cam.unlockForConfiguration()
        } catch (e: Exception) {
            NSLog("CameraK Error: applyContinuousAutofocusAndExposure: ${e.message}")
            try { cam.unlockForConfiguration() } catch (_: Exception) { }
        }
    }

    /**
     * Map tap (normalized to preview bounds 0..1) to [AVCaptureDevice] POI.
     * Must use [AVCaptureVideoPreviewLayer.captureDevicePointConvertedFromLayerPoint] so aspect-fill crop
     * matches system Camera; otherwise exposure meters the wrong region and highlights stay blown out.
     */
    @OptIn(ExperimentalForeignApi::class)
    private fun devicePointOfInterestFromNormalizedTap(normalizedX: Float, normalizedY: Float): Pair<Double, Double> {
        val layer = cameraPreviewLayer
        if (layer != null) {
            try {
                layer.bounds.useContents {
                    val w = size.width
                    val h = size.height
                    if (w > 0.0 && h > 0.0) {
                        val lx = normalizedX.coerceIn(0f, 1f).toDouble() * w
                        val ly = normalizedY.coerceIn(0f, 1f).toDouble() * h
                        val layerPoint = CGPointMake(lx, ly)
                        layer.captureDevicePointOfInterestForPoint(layerPoint).useContents {
                            return Pair(x, y)
                        }
                    }
                }
            } catch (e: Exception) {
                NSLog("CameraK Error: captureDevicePointOfInterestForPoint failed: ${e.message}")
            }
        }
        val x = normalizedX.coerceIn(0f, 1f).toDouble()
        val y = normalizedY.coerceIn(0f, 1f).toDouble()
        return Pair(x, y)
    }

    /**
     * Tap-to-focus (normalized 0..1 in preview/view). Uses continuous AF/AE with updated POI when supported (like iOS Camera);
     * falls back to one-shot auto focus / auto expose.
     */
    @OptIn(ExperimentalForeignApi::class)
    fun setFocusPoint(normalizedX: Float, normalizedY: Float) {
        val camera = currentCamera ?: return
        val (x, y) = devicePointOfInterestFromNormalizedTap(normalizedX, normalizedY)
        val point = CGPointMake(x.coerceIn(0.0, 1.0), y.coerceIn(0.0, 1.0))
        try {
            camera.lockForConfiguration(null)
            if (camera.isFocusPointOfInterestSupported()) {
                camera.focusPointOfInterest = point
                when {
                    camera.isFocusModeSupported(AVCaptureFocusModeContinuousAutoFocus) -> {
                        camera.focusMode = AVCaptureFocusModeContinuousAutoFocus
                    }
                    camera.isFocusModeSupported(AVCaptureFocusModeAutoFocus) -> {
                        camera.focusMode = AVCaptureFocusModeAutoFocus
                    }
                }
            }
            if (camera.isExposurePointOfInterestSupported()) {
                camera.exposurePointOfInterest = point
                when {
                    camera.isExposureModeSupported(AVCaptureExposureModeContinuousAutoExposure) -> {
                        camera.exposureMode = AVCaptureExposureModeContinuousAutoExposure
                    }
                    camera.isExposureModeSupported(AVCaptureExposureModeAutoExpose) -> {
                        camera.exposureMode = AVCaptureExposureModeAutoExpose
                    }
                }
            }
            camera.unlockForConfiguration()
        } catch (e: Exception) {
            NSLog("CameraK Error: setFocusPoint exception: ${e.message}")
            try { camera.unlockForConfiguration() } catch (_: Exception) { }
        }
    }

    /** Clamped exposure range so minimum doesn't go to black (align with Android UX). iOS raw range can be ±8 EV. */
    private val exposureRangeClamp = -2 to 2

    /**
     * Exposure compensation (brightness) range in EV units. Clamped to [exposureRangeClamp] so the
     * slider doesn't allow "until black" like Android (typical ±2 EV in steps).
     * Returns (0, 0) if device does not support exposure target bias.
     */
    @OptIn(ExperimentalForeignApi::class)
    fun getExposureCompensationRange(): Pair<Int, Int> {
        val camera = currentCamera ?: return Pair(0, 0)
        if (!camera.isExposureModeSupported(AVCaptureExposureModeContinuousAutoExposure)) return Pair(0, 0)
        val rawMin = camera.minExposureTargetBias.toInt()
        val rawMax = camera.maxExposureTargetBias.toInt()
        if (rawMin >= rawMax) return Pair(0, 0)
        val (clampMin, clampMax) = exposureRangeClamp
        val minBias = maxOf(rawMin, clampMin)
        val maxBias = minOf(rawMax, clampMax)
        return if (minBias < maxBias) Pair(minBias, maxBias) else Pair(0, 0)
    }

    /**
     * Resets fixed frame duration so photo preview is not locked to recording fps.
     */
    @OptIn(ExperimentalForeignApi::class)
    fun restoreVideoFrameDurationToAutomatic() {
        val camera = currentCamera ?: return
        camera.lockForConfiguration(null)
        try {
            val ranges = camera.activeFormat.videoSupportedFrameRateRanges as? NSArray
            val range =
                if (ranges != null && ranges.count.toInt() > 0) {
                    ranges.objectAtIndex(0u) as? AVFrameRateRange
                } else {
                    null
                }
            if (range != null) {
                camera.activeVideoMinFrameDuration = range.minFrameDuration
                camera.activeVideoMaxFrameDuration = range.maxFrameDuration
            }
        } catch (e: Exception) {
            NSLog("CameraK: restoreVideoFrameDurationToAutomatic failed: ${e.message}")
        } finally {
            camera.unlockForConfiguration()
        }
    }

    /**
     * Locks the device and sets fixed video frame duration to **60 fps** when the active format
     * reports support; otherwise **30 fps**. Applied when entering video UI mode, not on record tap.
     */
    @OptIn(ExperimentalForeignApi::class)
    fun applyPreferredVideoRecordingFrameRatePrefer60Else30() {
        val camera = currentCamera ?: run {
            NSLog("CameraK: videoFpsFormatProbe skipped (no currentCamera)")
            return
        }
        camera.lockForConfiguration(null)
        try {
            val format = camera.activeFormat
            val nsRanges = format.videoSupportedFrameRateRanges as? NSArray
            val count = nsRanges?.count?.toInt() ?: 0
            var supports60 = false
            if (nsRanges != null && count > 0) {
                for (i in 0 until count) {
                    val r = nsRanges.objectAtIndex(i.toULong()) as? AVFrameRateRange ?: continue
                    if (r.maxFrameRate >= 59.0) {
                        supports60 = true
                        break
                    }
                }
            }
            val fps = if (supports60) 60 else 30
            val oneFrame = CMTimeMake(1, fps)
            camera.activeVideoMinFrameDuration = oneFrame
            camera.activeVideoMaxFrameDuration = oneFrame
            val rangeLog = buildString {
                if (nsRanges == null || count == 0) {
                    append("noRangesOrEmpty")
                } else {
                    for (i in 0 until count) {
                        val r = nsRanges.objectAtIndex(i.toULong()) as? AVFrameRateRange ?: continue
                        if (isNotEmpty()) append(", ")
                        append("[min=${r.minFrameRate},max=${r.maxFrameRate}]")
                    }
                }
            }
            NSLog(
                "CameraK: videoFpsFormatProbe supports60=$supports60 videoSupportedFrameRateRanges=$rangeLog chosenFps=$fps",
            )
        } catch (e: Exception) {
            NSLog("CameraK: applyPreferredVideoRecordingFrameRatePrefer60Else30 failed: ${e.message}")
        } finally {
            camera.unlockForConfiguration()
        }
    }

    /**
     * Sets exposure target bias (brightness) in EV units. Clamped to reported range (so within clamp).
     */
    @OptIn(ExperimentalForeignApi::class)
    fun setExposureTargetBias(bias: Int) {
        val camera = currentCamera ?: return
        if (!camera.isExposureModeSupported(AVCaptureExposureModeContinuousAutoExposure)) return
        val (minBias, maxBias) = getExposureCompensationRange()
        if (minBias == 0 && maxBias == 0) return
        val clamped = bias.toFloat().coerceIn(minBias.toFloat(), maxBias.toFloat())
        camera.lockForConfiguration(null)
        camera.setExposureTargetBias(clamped) { }
        camera.unlockForConfiguration()
    }

    /**
     * Current exposure target bias in EV units.
     */
    @OptIn(ExperimentalForeignApi::class)
    fun getExposureTargetBias(): Int = currentCamera?.exposureTargetBias?.toInt() ?: 0

    /**
     * Capture an image with specified quality
     * @param quality Used with still-image stabilization threshold (1.0 = always prefer max processing when enabled).
     */
    fun captureImage(quality: Double = 1.0) {
        if (photoOutput == null || captureSession?.isRunning() != true) {
            NSLog("CameraK Error: captureImage not ready photoOutput=${photoOutput != null} sessionRunning=${captureSession?.isRunning() == true}")
            onError?.invoke(CameraException.ConfigurationError("Camera not ready for capture"))
            return
        }

        val settings = AVCapturePhotoSettings.photoSettingsWithFormat(
            mapOf(
                AVVideoCodecKey to AVVideoCodecJPEG,
            ),
        )

        settings.setHighResolutionPhotoEnabled(false)

        when (qualityPrioritization) {
            QualityPrioritization.QUALITY, QualityPrioritization.NONE -> {
                settings.setHighResolutionPhotoEnabled(true)
                settings.photoQualityPrioritization = AVCapturePhotoQualityPrioritizationQuality
            }

            QualityPrioritization.BALANCED -> {
                settings.photoQualityPrioritization = AVCapturePhotoQualityPrioritizationBalanced
            }

            QualityPrioritization.SPEED -> {
                settings.photoQualityPrioritization = AVCapturePhotoQualityPrioritizationSpeed
            }
        }

        val supportedFlashModes = photoOutput?.supportedFlashModes() as? List<*>
        if (supportedFlashModes?.contains(this.flashMode) == true) {
            settings.flashMode = this.flashMode
        } else {
            settings.flashMode = AVCaptureFlashModeOff
        }

        if (highQualityEnabled && quality > 0.8) {
            settings.setAutoStillImageStabilizationEnabled(true)
        } else {
            settings.setAutoStillImageStabilizationEnabled(false)
        }

        applyShutterSoundSuppression(settings)

        // Set the photo output connection orientation to match current device orientation
        // This ensures the captured photo has the correct orientation metadata
        photoOutput?.connectionWithMediaType(AVMediaTypeVideo)?.let { connection ->
            if (connection.isVideoOrientationSupported()) {
                connection.videoOrientation = currentVideoOrientation()
            }
            if (connection.isVideoMirroringSupported()) {
                connection.automaticallyAdjustsVideoMirroring = false
                connection.setVideoMirrored(false)
            }
        }

        photoOutput?.capturePhotoWithSettings(settings, delegate = this)
    }

    fun captureImage() {
        captureImage(quality = 1.0)
    }

    /**
     * Suppresses the system shutter click when the device/region allows it (see
     * [AVCapturePhotoOutput.isShutterSoundSuppressionSupported]).
     */
    @OptIn(ExperimentalForeignApi::class)
    private fun applyShutterSoundSuppression(settings: AVCapturePhotoSettings) {
        val output = photoOutput ?: return
        if (output.isShutterSoundSuppressionSupported()) {
            settings.setShutterSoundSuppressionEnabled(true)
        }
    }

    /**
     * Switches to a specific camera device type (e.g. wide-angle, telephoto, ultra-wide)
     * while keeping the same camera position (front/back).
     */
    @OptIn(ExperimentalForeignApi::class)
    fun switchToDeviceType(deviceType: CameraDeviceType) {
        val session = captureSession ?: return
        val targetType = deviceType.toAVCaptureDeviceType() ?: return

        // Determine current position
        val position = if (isUsingFrontCamera) {
            AVCaptureDevicePositionFront
        } else {
            AVCaptureDevicePositionBack
        }

        // Discover device matching the requested type and position
        val discoverySession = AVCaptureDeviceDiscoverySession.discoverySessionWithDeviceTypes(
            listOf(targetType),
            AVMediaTypeVideo,
            position,
        )

        val newDevice = discoverySession.devices.firstOrNull() as? AVCaptureDevice ?: run {
            // Fallback: try any position
            val fallback = AVCaptureDeviceDiscoverySession.discoverySessionWithDeviceTypes(
                listOf(targetType),
                AVMediaTypeVideo,
                AVCaptureDevicePositionUnspecified,
            )
            fallback.devices.firstOrNull() as? AVCaptureDevice
        } ?: return

        val wasRunning = session.isRunning()
        if (wasRunning) {
            session.stopRunning()
        }

        session.beginConfiguration()

        try {
            // Remove current input
            session.inputs.firstOrNull()?.let { input ->
                session.removeInput(input as AVCaptureInput)
            }

            val newInput = AVCaptureDeviceInput.deviceInputWithDevice(newDevice, null)
                ?: throw Exception("Failed to create input for device type")

            if (session.canAddInput(newInput)) {
                session.addInput(newInput)
                currentCamera = newDevice
                if (newDevice.position == AVCaptureDevicePositionBack) {
                    backCamera = newDevice
                } else {
                    frontCamera = newDevice
                }
            }

            cameraPreviewLayer?.connection?.let { connection ->
                if (connection.isVideoMirroringSupported()) {
                    connection.automaticallyAdjustsVideoMirroring = false
                    connection.setVideoMirrored(isUsingFrontCamera)
                }
            }

            session.commitConfiguration()
            applyContinuousAutofocusAndExposure(currentCamera)
        } catch (_: Exception) {
            session.commitConfiguration()
        }

        if (wasRunning) {
            dispatch_async(
                dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_HIGH.toLong(), 0u),
            ) {
                session.startRunning()
            }
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    fun switchCamera() {
        guard(captureSession != null) { return@guard }

        val session = captureSession!!
        val wasRunning = session.isRunning()

        if (wasRunning) {
            session.stopRunning()
        }

        // Remember current camera so we can restore if add fails
        val previousCamera = currentCamera

        fun trySwitch(): Boolean {
            session.beginConfiguration()

            session.inputs?.firstOrNull()?.let { input ->
                session.removeInput(input as AVCaptureInput)
            }

            isUsingFrontCamera = !isUsingFrontCamera
            currentCamera = if (isUsingFrontCamera) frontCamera else backCamera

            val newCamera = currentCamera
            if (newCamera == null) {
                NSLog("CameraK Error: toggleCameraLens newCamera is null (front=$frontCamera back=$backCamera)")
                isUsingFrontCamera = !isUsingFrontCamera
                currentCamera = previousCamera
                return false
            }

            val newInput = AVCaptureDeviceInput.deviceInputWithDevice(newCamera, null)
            if (newInput == null) {
                NSLog("CameraK Error: toggleCameraLens deviceInputWithDevice returned null")
                isUsingFrontCamera = !isUsingFrontCamera
                currentCamera = previousCamera
                return false
            }

            val canAdd = session.canAddInput(newInput)
            if (canAdd) {
                session.addInput(newInput)
                cameraPreviewLayer?.connection?.let { connection ->
                    if (connection.isVideoMirroringSupported()) {
                        connection.automaticallyAdjustsVideoMirroring = false
                        connection.setVideoMirrored(isUsingFrontCamera)
                    }
                }
                session.commitConfiguration()
                return true
            }

            // canAddInput false: re-add previous input so session is not left without a camera
            session.commitConfiguration()
            val prevInput = previousCamera?.let { AVCaptureDeviceInput.deviceInputWithDevice(it, null) }
            if (prevInput != null && session.canAddInput(prevInput)) {
                session.beginConfiguration()
                session.addInput(prevInput)
                session.commitConfiguration()
                isUsingFrontCamera = !isUsingFrontCamera
                currentCamera = previousCamera
            } else {
                // Could not restore; revert state so retry will try the same target (back) again, not flip to front
                isUsingFrontCamera = !isUsingFrontCamera
                currentCamera = previousCamera
            }
            return false
        }

        try {
            var success = trySwitch()
            if (!success && previousCamera != null) {
                // Retry once (iOS sometimes accepts the new input on second attempt)
                success = trySwitch()
            }

            if (success) {
                applySessionPresetForCaptureMode(captureModeIsVideo)
                // Force preview layer to pick up new input (iOS can leave connection stale)
                cameraPreviewLayer?.let { layer ->
                    layer.session = null
                    layer.session = session
                }
                IosPreviewDbgLog.logSafe {
                    describePreviewPipeline("UI_switchCamera_SUCCESS", view = null)
                }
                processPendingConfigurations()
                // Re-apply torch after camera switch (back has torch, front does not)
                setTorchMode(torchMode)
                applyContinuousAutofocusAndExposure(currentCamera)
                if (wasRunning) {
                    dispatch_async(
                        dispatch_get_global_queue(
                            DISPATCH_QUEUE_PRIORITY_HIGH.toLong(),
                            0u,
                        ),
                    ) {
                        session.startRunning()
                    }
                }
            } else {
                if (wasRunning) {
                    dispatch_async(
                        dispatch_get_global_queue(
                            DISPATCH_QUEUE_PRIORITY_HIGH.toLong(),
                            0u,
                        ),
                    ) {
                        session.startRunning()
                    }
                }
                NSLog("CameraK Error: toggleCameraLens failed after retry lens=${getCurrentLens()}")
                onError?.invoke(CameraException.ConfigurationError("Cannot add input"))
            }
        } catch (e: CameraException) {
            NSLog("CameraK Error: toggleCameraLens CameraException: ${e::class.simpleName} - ${e.message}")
            session.commitConfiguration()
            if (wasRunning) {
                session.startRunning()
            }
            onError?.invoke(e)
        } catch (e: Exception) {
            NSLog("CameraK Error: toggleCameraLens Exception: ${e.message}")
            session.commitConfiguration()
            if (wasRunning) {
                session.startRunning()
            }
            onError?.invoke(CameraException.ConfigurationError(e.message ?: "Unknown error"))
        }
    }

    override fun captureOutput(
        output: AVCapturePhotoOutput,
        didFinishProcessingPhoto: AVCapturePhoto,
        error: NSError?,
    ) {
        if (error != null) {
            onError?.invoke(CameraException.CaptureError(error.localizedDescription))
            return
        }

        dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_HIGH.toLong(), 0u)) {
            val imageData = didFinishProcessingPhoto.fileDataRepresentation()
            val tr = effectiveTargetResolution()
            val cappedData = if (tr != null && imageData != null) {
                capNSDataJpegToMaxPhotoDimensions(imageData, tr.first, tr.second).also { capped ->
                    if (capped.length != imageData.length) {
                        NSLog(
                            "CameraK",
                            "captureCapped lens=${if (isUsingFrontCamera) "front" else "back"} cap=$tr",
                        )
                    }
                }
            } else {
                imageData
            }

            dispatch_async(dispatch_get_main_queue()) {
                onPhotoCapture?.invoke(cappedData)
            }
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    fun describePreviewPipeline(phase: String, view: UIView?, extra: String = ""): String =
        runCatching { describePreviewPipelineUnsafe(phase, view, extra) }
            .getOrElse { error -> "phase=$phase | probeError=${error.message ?: error::class.simpleName}" }

    @OptIn(ExperimentalForeignApi::class)
    private fun describePreviewPipelineUnsafe(
        phase: String,
        view: UIView?,
        extra: String,
    ): String = buildString {
        append("phase=$phase ")
        if (view != null) {
            val bounds = view.bounds.useContents { "${size.width.toInt()}x${size.height.toInt()}" }
            val frame = view.frame.useContents {
                "origin=(${origin.x.toInt()},${origin.y.toInt()}) size=${size.width.toInt()}x${size.height.toInt()}"
            }
            val safe = view.safeAreaInsets.useContents {
                "safe(t=${top.toInt()},b=${bottom.toInt()},l=${left.toInt()},r=${right.toInt()})"
            }
            val sublayerCount = view.layer.sublayers?.size ?: 0
            append(
                "view(bounds=$bounds frame=$frame $safe sublayerCount=$sublayerCount " +
                    "window=${view.window != null}) ",
            )
        } else {
            append("view=null ")
        }
        cameraPreviewLayer?.let { layer ->
            val frame = layer.frame.useContents { "${size.width.toInt()}x${size.height.toInt()}" }
            val bounds = layer.bounds.useContents { "${size.width.toInt()}x${size.height.toInt()}" }
            val pos = layer.position.useContents { "(${x.toInt()},${y.toInt()})" }
            val conn = layer.connection
            append(
                "previewLayer(frame=$frame bounds=$bounds pos=$pos gravity=${layer.videoGravity} " +
                    "scale=${layer.contentsScale} hidden=${layer.hidden} opacity=${layer.opacity} " +
                    "sessionAttached=${layer.session != null} " +
                    "conn(orientation=${conn?.videoOrientation})) ",
            )
        } ?: append("previewLayer=null ")
        val session = captureSession
        val running = session?.let { runCatching { if (it.isRunning()) "true" else "false" }.getOrNull() }
        append(
            "session(preset=${session?.sessionPreset ?: "null"} running=$running " +
                "lens=${if (isUsingFrontCamera) "front" else "back"} captureModeIsVideo=$captureModeIsVideo) ",
        )
        append("zoom=${getZoom()} maxZoom=${getMaxZoom()} ")
        currentCamera?.activeFormat?.let { format ->
            val dims = CMVideoFormatDescriptionGetDimensions(format.formatDescription)
            val dimStr = dims.useContents { "${width}x${height}" }
            append("deviceFormat=$dimStr ")
        }
        val screen = UIScreen.mainScreen
        val screenBounds = screen.bounds.useContents { "${size.width.toInt()}x${size.height.toInt()}" }
        append(
            "screen(bounds=$screenBounds scale=${screen.scale} " +
                "deviceOrientation=${UIDevice.currentDevice.orientation} " +
                "captureOrientation=${IosCaptureVideoOrientation.current})",
        )
        if (extra.isNotEmpty()) append(" | $extra")
    }

    private inline fun guard(condition: Boolean, crossinline block: () -> Unit) {
        if (!condition) block()
    }
}
