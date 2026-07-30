package core.domain.camera.controller

import android.os.Handler
import android.os.Looper
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat as AndroidImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.hardware.display.DisplayManager
import android.media.ExifInterface
import android.os.Build
import android.os.Environment
import android.util.Log
import android.util.Range
import android.util.Size
import android.view.Display
import android.view.Surface
import androidx.annotation.OptIn
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalZeroShutterLag
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import core.domain.camera.android.FrontCameraVideoExportHelper
import androidx.camera.core.Preview
import androidx.camera.core.UseCaseGroup
import androidx.camera.extensions.ExtensionMode
import androidx.camera.extensions.ExtensionsManager
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
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
import core.domain.camera.utils.InvalidConfigurationException
import core.util.bytearray.toImageBitmap
import core.domain.camera.utils.MemoryManager
import core.domain.camera.utils.compressToByteArray
import core.domain.camera.video.VideoCaptureResult
import core.domain.camera.video.VideoConfiguration
import core.domain.camera.video.VideoQuality
import core.util.image.JpegDebugProbe
import core.util.video.VideoFileDbgProbe
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Android-specific implementation of [CameraController] using CameraX.
 */
actual class CameraController(
    val context: Context,
    val lifecycleOwner: LifecycleOwner,
    internal var flashMode: FlashMode,
    internal var torchMode: TorchMode,
    internal var cameraLens: CameraLens,
    internal var imageFormat: ImageFormat,
    internal var qualityPriority: QualityPrioritization,
    internal var directory: Directory,
    internal var cameraDeviceType: CameraDeviceType,
    internal var returnFilePath: Boolean,
    internal var aspectRatio: AspectRatio,
    internal var plugins: MutableList<CameraPlugin>,
    internal var targetResolution: Pair<Int, Int>? = null,
    internal var targetResolutionFront: Pair<Int, Int>? = null,
) {
    actual val usesPhotoCaptureForVideoThumbnail: Boolean = false

    private var cameraProvider: ProcessCameraProvider? = null
    private var imageCapture: ImageCapture? = null
    private var preview: Preview? = null
    private var camera: Camera? = null
    var imageAnalyzer: ImageAnalysis? = null
    private var previewView: PreviewView? = null
    var onDeviceTypeSwitchTransition: ((Boolean) -> Unit)? = null
    private var isSwitchingDeviceType = false

    /** When true and the bound camera reports support, [Preview] is built with preview stabilization (EIS). */
    private var previewStabilizationEnabled = false

    // Multiple analyzer support: plugins register their analyzers here
    private val registeredAnalyzers = mutableListOf<ImageAnalysis.Analyzer>()

    // Video recording
    private var videoCapture: VideoCapture<Recorder>? = null
    private var activeRecording: Recording? = null
    private var recordingOutputFile: File? = null
    private var recordingUsedFrontLens: Boolean = false

    /** Lens of the clip waiting for [applyRecordingPostProcessing]; a chained recording may already be running. */
    private var stoppedRecordingUsedFrontLens: Boolean = false
    private val recordingFinalizeChannel = Channel<VideoCaptureResult>(Channel.CONFLATED)

    /**
     * Omit [VideoCapture] in photo mode so [ImageCapture] is not capped at ~1080p by the
     * preview+video+still use-case intersection. Video mode keeps video attached.
     */
    private var attachVideoToCameraSession: Boolean = false

    /** Mirrors Photo / Video tab; drives [attachVideoToCameraSession] for both lenses. */
    private var captureModeIsVideo: Boolean = false

    private var bindAwaitContinuation: CancellableContinuation<Unit>? = null

    actual var onPreviewTapListener: ((Float, Float) -> Unit)? = null
    actual var onPreviewDoubleTapListener: (() -> Unit)? = null
    actual var shouldSuppressTapToFocus: ((Float, Float) -> Boolean)? = null

    private val imageCaptureListeners = mutableListOf<(ByteArray) -> Unit>()

    private val memoryManager = MemoryManager
    private val pendingCaptures = atomic(0)
    private val maxConcurrentCaptures = 3

    private val imageProcessingExecutor = Executors.newFixedThreadPool(2)

    fun bindCamera(previewView: PreviewView, onCameraReady: () -> Unit = {}) {
        Log.d("CameraK", "==> bindCamera() called for deviceType: $cameraDeviceType")
        this.previewView = previewView

        memoryManager.initialize(context)

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                val provider = cameraProvider ?: return@addListener
                val extensionsFuture = ExtensionsManager.getInstanceAsync(context, provider)
                extensionsFuture.addListener(
                    {
                        val extensionsManager = try {
                            extensionsFuture.get()
                        } catch (e: Exception) {
                            Log.w("CameraK", "ExtensionsManager unavailable: ${e.message}")
                            null
                        }
                        completeBinding(previewView, onCameraReady, extensionsManager)
                    },
                    ContextCompat.getMainExecutor(context),
                )
            } catch (exc: Exception) {
                Log.e("CameraK", "==> Use case binding failed for $cameraDeviceType: ${exc.message}")
                exc.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(context))
    }

    /**
     * When the OEM exposes CameraX vendor extensions, prefer HDR (highlight recovery) then AUTO
     * for preview + still capture. Falls back to [baseSelector] if unavailable or bind fails.
     */
    private fun extensionEnabledSelectorOrFallback(
        baseSelector: CameraSelector,
        extensionsManager: ExtensionsManager?,
    ): CameraSelector {
        if (extensionsManager == null) {
            Log.d(
                "CameraK",
                "ExtensionsManager is null; extensions disabled " +
                    "(lens=$cameraLens, deviceType=$cameraDeviceType, aspectRatio=$aspectRatio, " +
                    "targetResolution=$targetResolution, qualityPriority=$qualityPriority, " +
                    "flashMode=$flashMode, torchMode=$torchMode, imageFormat=$imageFormat)",
            )
            return baseSelector
        }

        Log.d(
            "CameraK",
            "Checking extensions with specs " +
                "(lens=$cameraLens, deviceType=$cameraDeviceType, aspectRatio=$aspectRatio, " +
                "targetResolution=$targetResolution, qualityPriority=$qualityPriority, " +
                "flashMode=$flashMode, torchMode=$torchMode, imageFormat=$imageFormat)",
        )

        val allModes = listOf(
            "HDR" to ExtensionMode.HDR,
            "AUTO" to ExtensionMode.AUTO,
            "NIGHT" to ExtensionMode.NIGHT,
            "BOKEH" to ExtensionMode.BOKEH,
            "FACE_RETOUCH" to ExtensionMode.FACE_RETOUCH,
        )
        val availabilitySummary = buildString {
            allModes.forEachIndexed { index, (name, mode) ->
                val available = try {
                    extensionsManager.isExtensionAvailable(baseSelector, mode)
                } catch (e: Exception) {
                    Log.w("CameraK", "$name availability query failed: ${e.message}")
                    false
                }
                append("$name=$available")
                if (index != allModes.lastIndex) append(", ")
            }
        }
        Log.d("CameraK", "Extension availability -> $availabilitySummary")

        val modes = listOf(
            "HDR" to ExtensionMode.HDR,
            "NIGHT" to ExtensionMode.NIGHT,
            "AUTO" to ExtensionMode.AUTO,
        )
        for ((modeName, modeValue) in modes) {
            try {
                if (extensionsManager.isExtensionAvailable(baseSelector, modeValue)) {
                    val enabled = extensionsManager.getExtensionEnabledCameraSelector(baseSelector, modeValue)
                    Log.d("CameraK", "Vendor extension enabled: mode=$modeName")
                    return enabled
                }
            } catch (e: Exception) {
                Log.w("CameraK", "Extension query failed mode=$modeName: ${e.message}")
            }
        }
        Log.d("CameraK", "No vendor extension selected; fallback to base selector")
        return baseSelector
    }

    /**
     * AE [CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES] often caps at 30 even when the scaler map allows 60 fps
     * video (typical on Samsung + CameraX extension paths). Uses [StreamConfigurationMap] min frame
     * duration for camera outputs used by the video pipeline.
     */
    @OptIn(ExperimentalCamera2Interop::class)
    private fun streamConfigurationMapSuggests60FpsVideo(
        c2: Camera2CameraInfo,
    ): Pair<Boolean, String> {
        val map = c2.getCameraCharacteristic(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            ?: return false to "noScalerStreamConfigurationMap"
        /** Max frame duration in ns still counted as "60 fps capable" (1/60 s, with small slack). */
        val maxNsFor60 = 1_000_000_000L / 60L + 1L
        val formats = listOf(
            android.graphics.ImageFormat.PRIVATE,
            android.graphics.ImageFormat.YUV_420_888,
        )
        var bestMinNs: Long = Long.MAX_VALUE
        val examples = mutableListOf<String>()
        for (format in formats) {
            val sizes = try {
                map.getOutputSizes(format)
            } catch (_: Exception) {
                null
            } ?: continue
            val sorted = sizes.sortedByDescending { sz -> sz.width.toLong() * sz.height }
            for (size in sorted.take(48)) {
                val minNs = try {
                    map.getOutputMinFrameDuration(format, size)
                } catch (_: Exception) {
                    0L
                }
                if (minNs <= 0L) continue
                if (minNs < bestMinNs) {
                    bestMinNs = minNs
                }
                if (minNs <= maxNsFor60) {
                    if (examples.size < 4) {
                        examples += "f=$format ${size.width}x${size.height} minNs=$minNs"
                    }
                }
            }
        }
        if (examples.isNotEmpty()) {
            return true to "60capableStreams=${examples.joinToString("; ")}"
        }
        val implied = if (bestMinNs < Long.MAX_VALUE && bestMinNs > 0) {
            (1_000_000_000.0 / bestMinNs.toDouble()).toInt()
        } else {
            0
        }
        val bestStr = if (bestMinNs < Long.MAX_VALUE) bestMinNs.toString() else "n/a"
        return false to "no60Stream bestMinFrameDurationNs=$bestStr impliedMaxFps~$implied"
    }

    /**
     * Prefer 60 fps when either AE ranges include 60, or the scaler stream map reports a video-class
     * output that can sustain 60 fps (min frame duration ≤ 1/60 s). Otherwise 30.
     */
    @OptIn(ExperimentalCamera2Interop::class)
    private fun pickVideoRecordingTargetFps(
        provider: ProcessCameraProvider,
        selector: CameraSelector,
    ): Int = try {
        val matching = provider.availableCameraInfos.filter { selector.filter(listOf(it)).isNotEmpty() }
        if (matching.isEmpty()) {
            Log.w("CameraK", "videoFpsProbe: no CameraInfo matched selector → chosenTargetFps=30")
            return 30
        }
        var anyAe60 = false
        var anyStream60 = false
        val perCamera = mutableListOf<String>()
        for (cameraInfo in matching) {
            val c2 = Camera2CameraInfo.from(cameraInfo)
            val cameraId = try {
                c2.cameraId
            } catch (_: Exception) {
                "?"
            }
            val ranges = c2.getCameraCharacteristic(
                CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES,
            )
            val aeRangeStr: String
            val ae60: Boolean
            if (ranges == null || ranges.isEmpty()) {
                aeRangeStr = "nullOrEmpty"
                ae60 = false
            } else {
                aeRangeStr = ranges.joinToString(separator = ", ") { "[${it.lower},${it.upper}]" }
                ae60 = ranges.any { rng -> rng.contains(60) }
            }
            if (ae60) {
                anyAe60 = true
            }
            val (stream60, streamDetail) = streamConfigurationMapSuggests60FpsVideo(c2)
            if (stream60) {
                anyStream60 = true
            }
            perCamera += "cameraId=$cameraId lens=${cameraInfo.lensFacing} aeSupports60=$ae60 " +
                "streamSupports60=$stream60 streamDetail=$streamDetail aeTargetFpsRanges=$aeRangeStr"
        }
        Log.d("CameraK", "videoFpsProbe: ${perCamera.joinToString(" | ")}")
        val chosen = if (anyAe60 || anyStream60) 60 else 30
        Log.d(
            "CameraK",
            "videoFpsProbe: summary anyAe60=$anyAe60 anyStream60=$anyStream60 chosenTargetFps=$chosen",
        )
        chosen
    } catch (e: Exception) {
        Log.w("CameraK", "videoFpsProbe: exception (${e.message}) → chosenTargetFps=30")
        30
    }

    @OptIn(ExperimentalCamera2Interop::class)
    private fun checkPreviewStabilizationSupport(
        provider: ProcessCameraProvider,
        selector: CameraSelector,
    ): Boolean = try {
        val matching = provider.availableCameraInfos.filter { selector.filter(listOf(it)).isNotEmpty() }
        val details = matching.map { cameraInfo ->
            val supported = Preview.getPreviewCapabilities(cameraInfo).isStabilizationSupported
            val cameraId = try {
                Camera2CameraInfo.from(cameraInfo).cameraId
            } catch (_: Exception) {
                "?"
            }
            "cameraId=$cameraId lens=${cameraInfo.lensFacing} previewStabilizationSupported=$supported"
        }
        Log.d("CameraK", "previewStabProbe: ${details.joinToString(" | ")}")
        matching.any { cameraInfo -> Preview.getPreviewCapabilities(cameraInfo).isStabilizationSupported }
    } catch (e: Exception) {
        Log.w("CameraK", "Preview stabilization check failed: ${e.message}")
        false
    }

    private fun completeBinding(
        previewView: PreviewView,
        onCameraReady: () -> Unit,
        extensionsManager: ExtensionsManager?,
    ) {
        try {
            cameraProvider?.unbindAll()
            Log.d("CameraK", "==> Unbind all existing cameras")

            // Preview: always follow [aspectRatio] so PreviewView fills the UI (portrait still-capture
            // bounds must not constrain the viewfinder stream — that caused a large top letterbox).
            val previewResolutionSelector = createPreviewResolutionSelector()
            var imageCaptureResolutionSelector = createImageCaptureResolutionSelector()

            val displayRotation = (context.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager)
                ?.getDisplay(Display.DEFAULT_DISPLAY)
                ?.rotation
                ?: Surface.ROTATION_0

            val cameraSelector = createCameraSelector()
            val bindSelector = extensionEnabledSelectorOrFallback(cameraSelector, extensionsManager)
            // Probe with the base selector — extension filters can hide 60 fps AE/stream capabilities.
            val videoTargetFps = cameraProvider?.let { pickVideoRecordingTargetFps(it, cameraSelector) } ?: 30
            /**
             * Samsung (and some OEMs): vendor extension + VideoCapture often muxes ~25fps in the MP4
             * even when [VideoCapture.Builder.setTargetFrameRate] requests 60. Prefer a base
             * (non-extension) bind for the lifecycle when we explicitly target 60 fps.
             */
            val lifecycleBindSelector = if (videoTargetFps == 60 && bindSelector != cameraSelector) {
                Log.w(
                    "CameraK",
                    "videoFpsBind: chosenTargetFps=60 → binding WITHOUT vendor extension for this session " +
                        "(retry with extension if bind fails — extension may limit mux fps).",
                )
                cameraSelector
            } else {
                bindSelector
            }
            val supportsPreviewStab = cameraProvider?.let { provider ->
                checkPreviewStabilizationSupport(provider, lifecycleBindSelector)
            } ?: false
            Log.d(
                "CameraK",
                "==> Camera selector: deviceType=$cameraDeviceType " +
                    "(extensions=${bindSelector != cameraSelector}) " +
                    "lifecycleBindExtensions=${lifecycleBindSelector != cameraSelector} " +
                    "previewStabSupported=$supportsPreviewStab previewStabOn=$previewStabilizationEnabled",
            )

            val previewBuilder = Preview.Builder()
                .setResolutionSelector(previewResolutionSelector)
                .setTargetRotation(displayRotation)
            if (previewStabilizationEnabled && supportsPreviewStab) {
                previewBuilder.setPreviewStabilizationEnabled(true)
            }
            applyWideSelfieInterop(previewBuilder)
            preview = previewBuilder
                .build()
                .also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

            configureCaptureUseCase(imageCaptureResolutionSelector, displayRotation)
            val videoQuality = when (cameraLens) {
                CameraLens.FRONT -> VideoQuality.FHD
                CameraLens.BACK -> VideoQuality.FHD
            }
            configureVideoCaptureUseCase(
                quality = videoQuality,
                targetFrameRateFps = videoTargetFps,
                bindSelector = lifecycleBindSelector,
            )
            val includeVideo = shouldIncludeVideoInCameraSession()
            Log.d(
                "CameraK",
                "[RindleVideoDbg] bind session lens=$cameraLens includeVideo=$includeVideo " +
                    "videoQuality=$videoQuality (${videoQuality.width}x${videoQuality.height} cap) " +
                    "captureModeIsVideo=$captureModeIsVideo targetFps=$videoTargetFps",
            )

            // Do not force a ViewPort crop rect here.
            // On tall displays this can crop left/right aggressively; keeping use cases uncropped
            // and letting PreviewView FIT_END handle layout preserves more horizontal FoV.

            fun buildUseCaseGroup(): UseCaseGroup {
                val builder = UseCaseGroup.Builder()
                    .addUseCase(preview!!)
                    .addUseCase(imageCapture!!)
                if (includeVideo) {
                    videoCapture?.let { builder.addUseCase(it) }
                }
                imageAnalyzer?.let { builder.addUseCase(it) }
                return builder.build()
            }

            var useCaseGroup = buildUseCaseGroup()

            camera = try {
                cameraProvider?.bindToLifecycle(
                    lifecycleOwner,
                    lifecycleBindSelector,
                    useCaseGroup,
                )
            } catch (bindExc: Exception) {
                when {
                    lifecycleBindSelector != cameraSelector -> {
                        Log.w(
                            "CameraK",
                            "bindToLifecycle with vendor extension failed (${bindExc.message}); retrying without extension",
                        )
                        cameraProvider?.unbindAll()
                        cameraProvider?.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            useCaseGroup,
                        )
                    }
                    videoTargetFps == 60 && bindSelector != cameraSelector && lifecycleBindSelector == cameraSelector -> {
                        Log.w(
                            "CameraK",
                            "bindToLifecycle without extension failed for 60fps (${bindExc.message}); " +
                                "retrying WITH vendor extension (mux fps may drop)",
                        )
                        cameraProvider?.unbindAll()
                        cameraProvider?.bindToLifecycle(
                            lifecycleOwner,
                            bindSelector,
                            useCaseGroup,
                        )
                    }
                    else -> {
                        Log.w(
                            "CameraK",
                            "bindToLifecycle failed for lens=$cameraLens (${bindExc.message}); " +
                                "retrying with safe still-capture resolution",
                        )
                        cameraProvider?.unbindAll()
                        imageCaptureResolutionSelector = createSafeImageCaptureResolutionSelector()
                        configureCaptureUseCase(imageCaptureResolutionSelector, displayRotation)
                        useCaseGroup = buildUseCaseGroup()
                        cameraProvider?.bindToLifecycle(
                            lifecycleOwner,
                            lifecycleBindSelector,
                            useCaseGroup,
                        ) ?: throw bindExc
                    }
                }
            }
            Log.d("CameraK", "==> Camera successfully bound with deviceType: $cameraDeviceType")

            applyImageCaptureFlashAndTorchForCurrentState()

            finishCameraReady(onCameraReady)
            if (isSwitchingDeviceType) {
                isSwitchingDeviceType = false
                onDeviceTypeSwitchTransition?.invoke(false)
            }
        } catch (exc: Exception) {
            Log.e("CameraK", "==> Use case binding failed for $cameraDeviceType: ${exc.message}")
            exc.printStackTrace()
            camera = null
            if (isSwitchingDeviceType) {
                isSwitchingDeviceType = false
                onDeviceTypeSwitchTransition?.invoke(false)
            }
            finishCameraReady(onCameraReady)
        }
    }

    private fun shouldIncludeVideoInCameraSession(): Boolean = attachVideoToCameraSession

    private fun updateVideoAttachmentForCaptureMode() {
        attachVideoToCameraSession = captureModeIsVideo
    }

    private fun finishCameraReady(onCameraReady: () -> Unit) {
        onCameraReady()
        bindAwaitContinuation?.let { cont ->
            if (cont.isActive) {
                cont.resume(Unit)
            }
            bindAwaitContinuation = null
        }
    }

    private suspend fun rebindCameraSession() {
        val view = previewView ?: return
        suspendCancellableCoroutine { cont ->
            bindAwaitContinuation = cont
            cont.invokeOnCancellation {
                if (bindAwaitContinuation === cont) {
                    bindAwaitContinuation = null
                }
            }
            bindCamera(view)
        }
    }

    /** Preview stream: [aspectRatio] only so the viewfinder matches the screen layout. */
    private fun createPreviewResolutionSelector(): ResolutionSelector {
        memoryManager.updateMemoryStatus()
        if (!attachVideoToCameraSession) {
            // Photo-only session: lower preview bandwidth so still capture can exceed 1080p.
            return ResolutionSelector.Builder()
                .setAspectRatioStrategy(AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY)
                .setResolutionStrategy(
                    ResolutionStrategy(
                        Size(1280, 720),
                        ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER,
                    ),
                )
                .build()
        }
        return ResolutionSelector.Builder()
            .setAspectRatioStrategy(aspectRatio.toCameraXAspectRatioStrategy())
            .build()
    }

    /** Per-lens still-capture cap; front falls back to back when [targetResolutionFront] is unset. */
    private fun stillCaptureResolutionCap(): Pair<Int, Int>? =
        when (cameraLens) {
            CameraLens.FRONT -> targetResolutionFront ?: targetResolution
            CameraLens.BACK -> targetResolution
        }

    /**
     * Still capture: when a lens cap is set, it is a per-axis upper bound on the **stored** size.
     * Only [supportedSizes] from CameraX may be returned — merging Camera2-only sizes breaks
     * bindToLifecycle when switching to the front camera (preview + video + still intersection).
     */
    private fun createImageCaptureResolutionSelector(): ResolutionSelector =
        createImageCaptureResolutionSelector(preferQualityRanking = true)

    /** Guaranteed bindable: highest resolution CameraX allows for this use-case group. */
    private fun createSafeImageCaptureResolutionSelector(): ResolutionSelector {
        memoryManager.updateMemoryStatus()
        return ResolutionSelector.Builder()
            .setAspectRatioStrategy(AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY)
            .setResolutionStrategy(ResolutionStrategy.HIGHEST_AVAILABLE_STRATEGY)
            .build()
    }

    private fun createImageCaptureResolutionSelector(preferQualityRanking: Boolean): ResolutionSelector {
        memoryManager.updateMemoryStatus()
        val cap = stillCaptureResolutionCap()
        if (cap == null || !preferQualityRanking) {
            return createSafeImageCaptureResolutionSelector()
        }
        val shortCap = minOf(cap.first, cap.second)
        val longCap = maxOf(cap.first, cap.second)
        val targetAspect = 9.0 / 16.0
        val lensLabel = cameraLens.name.lowercase()
        val photoOnlySession = !attachVideoToCameraSession
        val resolutionStrategy = if (photoOnlySession) {
            ResolutionStrategy(
                Size(shortCap, longCap),
                ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER,
            )
        } else {
            ResolutionStrategy.HIGHEST_AVAILABLE_STRATEGY
        }
        return ResolutionSelector.Builder()
            .setAspectRatioStrategy(AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY)
            .setResolutionFilter { supportedSizes, _ ->
                val camera2Sizes = queryCamera2StillJpegSizesForCurrentLens()
                val ranked = rankStillCaptureSizes(supportedSizes, shortCap, longCap, targetAspect)
                if (ranked.isNotEmpty()) {
                    val picked = ranked.first()
                    val camera2Best = camera2Sizes.maxByOrNull { it.width.toLong() * it.height }
                    Log.d(
                        "CameraK",
                        "imageCaptureResolution: lens=$lensLabel photoOnlySession=$photoOnlySession " +
                            "cap=${shortCap}x${longCap} target9:16 " +
                            "cameraX=${supportedSizes.size} camera2=${camera2Sizes.size} " +
                            "camera2Best=${camera2Best?.width}x${camera2Best?.height} " +
                            "picked=${picked.width}x${picked.height} " +
                            "supported=${supportedSizes.sortedByDescending { it.width * it.height }.take(12).joinToString { "${it.width}x${it.height}" }} " +
                            "candidates=${ranked.take(8).joinToString { "${it.width}x${it.height}" }}",
                    )
                    ranked
                } else {
                    Log.w(
                        "CameraK",
                        "imageCaptureResolution: lens=$lensLabel no ranked sizes; using cameraX list " +
                            "(cameraX=${supportedSizes.size})",
                    )
                    supportedSizes
                }
            }
            .setResolutionStrategy(resolutionStrategy)
            .build()
    }

    private fun rankStillCaptureSizes(
        sizes: List<Size>,
        shortCap: Int,
        longCap: Int,
        targetAspect: Double,
    ): List<Size> {
        val filtered = sizes.filter { size ->
            minOf(size.width, size.height) <= shortCap &&
                maxOf(size.width, size.height) <= longCap
        }
        return filtered.sortedWith(
            compareBy<Size> { size ->
                val shortSide = minOf(size.width, size.height).toDouble()
                val longSide = maxOf(size.width, size.height).toDouble()
                kotlin.math.abs((shortSide / longSide) - targetAspect)
            }.thenByDescending { size ->
                size.width.toLong() * size.height
            }.thenByDescending { size ->
                // Prefer portrait storage (height ≥ width) over 1920×1080 + EXIF rotate.
                if (size.height >= size.width) 1 else 0
            }.thenByDescending { size ->
                maxOf(size.width, size.height).toLong()
            },
        )
    }

    /**
     * All JPEG still sizes for the active lens from Camera2 (not limited to CameraX use-case
     * intersection). Includes [StreamConfigurationMap.getHighResolutionOutputSizes] on API 23+.
     */
    private fun queryCamera2StillJpegSizesForCurrentLens(): List<Size> {
        return try {
            val manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val facing = when (cameraLens) {
                CameraLens.FRONT -> CameraCharacteristics.LENS_FACING_FRONT
                CameraLens.BACK -> CameraCharacteristics.LENS_FACING_BACK
            }
            val sizes = mutableListOf<Size>()
            for (cameraId in manager.cameraIdList) {
                val characteristics = manager.getCameraCharacteristics(cameraId)
                if (characteristics.get(CameraCharacteristics.LENS_FACING) != facing) continue
                val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                    ?: continue
                map.getOutputSizes(AndroidImageFormat.JPEG)?.let { sizes.addAll(it) }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    map.getHighResolutionOutputSizes(AndroidImageFormat.JPEG)?.let { sizes.addAll(it) }
                }
            }
            sizes.distinctBy { it.width to it.height }
        } catch (e: Exception) {
            Log.w("CameraK", "queryCamera2StillJpegSizes(${cameraLens.name}): ${e.message}")
            emptyList()
        }
    }

    private fun AspectRatio.toCameraXAspectRatioStrategy(): AspectRatioStrategy = when (this) {
        AspectRatio.RATIO_16_9, AspectRatio.RATIO_9_16 -> AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY
        AspectRatio.RATIO_4_3 -> AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY
        AspectRatio.RATIO_1_1 -> AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY // closest available
    }

    /**
     * Creates a camera selector based on lens facing and device type.
     *
     * Uses Camera2 Interop to access physical camera characteristics for proper
     * device type selection (telephoto, ultra-wide, macro). Falls back gracefully
     * to the default camera if the requested type is not available.
     *
     * @return CameraSelector configured for the current lens and device type
     */
    @OptIn(ExperimentalCamera2Interop::class)
    private fun createCameraSelector(): CameraSelector {
        val builder = CameraSelector.Builder()
            .requireLensFacing(cameraLens.toCameraXLensFacing())

        when (cameraDeviceType) {
            CameraDeviceType.WIDE_ANGLE, CameraDeviceType.DEFAULT -> {
                // Default camera, no filter needed
            }
            CameraDeviceType.TELEPHOTO -> {
                builder.addCameraFilter { cameraInfos ->
                    cameraInfos.filter { cameraInfo ->
                        try {
                            val camera2Info = Camera2CameraInfo.from(cameraInfo)
                            val focalLengths = camera2Info.getCameraCharacteristic(
                                CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS,
                            )?.toList() ?: emptyList()
                            focalLengths.any { it > 4.0f }
                        } catch (e: Exception) {
                            false
                        }
                    }.ifEmpty {
                        Log.w("CameraK", "Telephoto camera not available, using default")
                        cameraInfos.take(1)
                    }
                }
            }
            CameraDeviceType.ULTRA_WIDE -> {
                builder.addCameraFilter { cameraInfos ->
                    cameraInfos.filter { cameraInfo ->
                        try {
                            val camera2Info = Camera2CameraInfo.from(cameraInfo)
                            val focalLengths = camera2Info.getCameraCharacteristic(
                                CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS,
                            )?.toList() ?: emptyList()
                            focalLengths.any { it < 2.5f }
                        } catch (e: Exception) {
                            false
                        }
                    }.ifEmpty {
                        Log.w("CameraK", "Ultra-wide camera not available, using default")
                        cameraInfos.take(1)
                    }
                }
            }
            CameraDeviceType.MACRO -> {
                builder.addCameraFilter { cameraInfos ->
                    cameraInfos.filter { cameraInfo ->
                        try {
                            val camera2Info = Camera2CameraInfo.from(cameraInfo)
                            val minFocusDistance = camera2Info.getCameraCharacteristic(
                                CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE,
                            ) ?: 0f
                            minFocusDistance > 0f && minFocusDistance < 0.2f
                        } catch (e: Exception) {
                            false
                        }
                    }.ifEmpty {
                        Log.w("CameraK", "Macro camera not available, using default")
                        cameraInfos.take(1)
                    }
                }
            }
        }

        return builder.build()
    }

    /**
     * Wide selfie is intentionally a no-op on Android for now.
     *
     * On tested Samsung devices, CameraX/Camera2 public APIs expose only the standard front
     * stream (`zoomRatioRange` starts at 1.0) and no vendor/private keys are available.
     * Keep this hook so UI state remains consistent while avoiding unstable device-specific hacks.
     */
    private fun <T> applyWideSelfieInterop(builder: T) where T : Any {
        // Intentionally left blank.
    }

    /**
     * Prefer zero-shutter-lag so the saved frame matches the moment the user tapped capture,
     * not a later preview frame after camera motion. ZSL is incompatible with flash ON/AUTO.
     */
    @OptIn(ExperimentalZeroShutterLag::class)
    private fun resolveStillImageCaptureMode(): Int = when (qualityPriority) {
        QualityPrioritization.QUALITY, QualityPrioritization.NONE ->
            ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY
        QualityPrioritization.SPEED, QualityPrioritization.BALANCED ->
            if (flashMode == FlashMode.OFF) {
                ImageCapture.CAPTURE_MODE_ZERO_SHUTTER_LAG
            } else {
                ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY
            }
    }

    @OptIn(ExperimentalZeroShutterLag::class, ExperimentalCamera2Interop::class)
    private fun configureCaptureUseCase(
        resolutionSelector: ResolutionSelector,
        displayRotation: Int,
    ) {
        val builder = ImageCapture.Builder()
            .setFlashMode(flashMode.toCameraXFlashMode())
            .setCaptureMode(resolveStillImageCaptureMode())
            .setResolutionSelector(resolutionSelector)
            .setTargetRotation(displayRotation)
        applyWideSelfieInterop(builder)
        applySilentStillCaptureRequestOptions(builder)
        imageCapture = builder.build()
    }

    /**
     * Best-effort shutter mute for still capture (Camera2 vendor/HAL keys; not guaranteed on all OEMs).
     */
    @OptIn(ExperimentalCamera2Interop::class)
    private fun applySilentStillCaptureRequestOptions(builder: ImageCapture.Builder) {
        runCatching {
            Camera2Interop.Extender(builder)
                .setCaptureRequestOption(CAPTURE_REQUEST_PLAY_SHUTTER_SOUND, 0)
        }.onFailure { e ->
            Log.w("CameraK", "Silent still capture request options not applied: ${e.message}")
        }
    }

    private companion object {
        /** Used on some Android builds (AOSP / OEM) to disable the still-capture shutter click. */
        private val CAPTURE_REQUEST_PLAY_SHUTTER_SOUND: CaptureRequest.Key<Int> =
            CaptureRequest.Key("android.playShutterSound", Int::class.java)
    }

    /** Syncs still-capture flash on [imageCapture] and preview torch from [torchMode] only. */
    private fun applyImageCaptureFlashAndTorchForCurrentState() {
        imageCapture?.flashMode = flashMode.toCameraXFlashMode()
        applyPreviewTorchPolicy()
    }

    /**
     * Controls preview LED torch only from [torchMode]. Still photo flash ([flashMode]) is applied
     * via [ImageCapture.flashMode] and fires at capture time — never mirror flash as preview torch
     * or it behaves like a video light instead of a burst at shutter.
     */
    private fun applyPreviewTorchPolicy() {
        val control = camera?.cameraControl ?: return
        val userTorch = torchMode == TorchMode.ON || torchMode == TorchMode.AUTO
        control.enableTorch(userTorch)
    }

    fun updateImageAnalyzer() {
        camera?.let {
            cameraProvider?.unbind(imageAnalyzer)
            imageAnalyzer?.let { analyzer ->
                cameraProvider?.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.Builder().requireLensFacing(cameraLens.toCameraXLensFacing())
                        .build(),
                    analyzer,
                )
            }
        } ?: throw InvalidConfigurationException("Camera not initialized.")
    }

    /**
     * Registers an [ImageAnalysis.Analyzer] to receive camera frames.
     * Multiple analyzers can be registered simultaneously — they share a single
     * [ImageAnalysis] use case via ref-counted frame dispatch.
     */
    fun registerImageAnalyzer(analyzer: ImageAnalysis.Analyzer) {
        registeredAnalyzers.add(analyzer)
        rebuildMultiplexedAnalyzer()
    }

    /**
     * Unregisters a previously registered analyzer.
     */
    fun unregisterImageAnalyzer(analyzer: ImageAnalysis.Analyzer) {
        registeredAnalyzers.remove(analyzer)
        rebuildMultiplexedAnalyzer()
    }

    private fun rebuildMultiplexedAnalyzer() {
        if (registeredAnalyzers.isEmpty()) {
            if (imageAnalyzer != null) {
                try { cameraProvider?.unbind(imageAnalyzer) } catch (_: Exception) {}
                imageAnalyzer = null
            }
            return
        }

        val composite = MultiplexingAnalyzer(registeredAnalyzers.toList())
        imageAnalyzer = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .apply {
                setAnalyzer(ContextCompat.getMainExecutor(context), composite)
            }

        try {
            updateImageAnalyzer()
        } catch (_: Exception) {
            // Camera may not be initialized yet; will be bound at bindCamera time
        }
    }

    @Deprecated(
        message = "Use takePictureToFile() instead for better performance",
        replaceWith = ReplaceWith("takePictureToFile()"),
        level = DeprecationLevel.WARNING,
    )
    actual suspend fun takePicture(): ImageCaptureResult = suspendCancellableCoroutine { cont ->
        if (pendingCaptures.incrementAndGet() > maxConcurrentCaptures) {
            pendingCaptures.decrementAndGet()
            Log.w("CameraK", "Burst queue full, dropping frame (${pendingCaptures.value} in progress)")
            cont.resume(ImageCaptureResult.Error(Exception("Burst queue full, capture rejected")))
            return@suspendCancellableCoroutine
        }

        // Update memory status before capture
        memoryManager.updateMemoryStatus()

        // Maximum JPEG quality when re-encoding is required
        performCapture(cont, quality = 100)

        cont.invokeOnCancellation {
            pendingCaptures.decrementAndGet()
        }
    }

    /**
     * Fast capture method that returns file path directly without ByteArray processing.
     * Significantly faster than takePicture() - skips decode/encode cycles.
     */
    actual suspend fun takePictureToFile(): ImageCaptureResult = suspendCancellableCoroutine { cont ->
        if (pendingCaptures.incrementAndGet() > maxConcurrentCaptures) {
            pendingCaptures.decrementAndGet()
            Log.w("CameraK", "Burst queue full, dropping frame")
            cont.resume(ImageCaptureResult.Error(Exception("Burst queue full, capture rejected")))
            return@suspendCancellableCoroutine
        }

        performCaptureToFile(cont)

        cont.invokeOnCancellation {
            pendingCaptures.decrementAndGet()
        }
    }

    /**
     * Perform fast file-based capture without saving to gallery.
     * Writes to a cache file, then returns bytes + bitmap and deletes the cache file.
     * Use [core.util.image.PhotoSaveUtils.savePhoto] to save after optionally adding location via [core.util.image.PhotoSaveUtils.addLocationExif].
     */
    private fun performCaptureToFile(continuation: CancellableContinuation<ImageCaptureResult>) {
        val cacheFile = java.io.File.createTempFile("capture_", ".${imageFormat.extension}", context.cacheDir)
        val isFront = cameraLens == CameraLens.FRONT
        val metadata = ImageCapture.Metadata().apply { isReversedHorizontal = isFront }
        val outputOptions = ImageCapture.OutputFileOptions.Builder(cacheFile)
            .setMetadata(metadata)
            .build()

        imageCapture?.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val path = cacheFile.absolutePath
                    imageProcessingExecutor.execute {
                        val byteArray = try {
                            cacheFile.readBytes().also { bytes ->
                                Log.d(
                                    "CameraK",
                                    "captureSaved lens=${cameraLens.name} ${JpegDebugProbe.describe(bytes)}",
                                )
                            }
                        } catch (e: Exception) {
                            cacheFile.delete()
                            Handler(Looper.getMainLooper()).post {
                                pendingCaptures.decrementAndGet()
                                continuation.resume(ImageCaptureResult.Error(e))
                            }
                            return@execute
                        }
                        val bitmap = try {
                            decodeThumbnailBitmapWithExifRotation(
                                path = path,
                                ignoreMirrorInExif = isFront,
                            )
                        } catch (e: Exception) {
                            Log.e("CameraK", "Failed to decode bitmap from file", e)
                            null
                        }
                        cacheFile.delete()
                        Handler(Looper.getMainLooper()).post {
                            pendingCaptures.decrementAndGet()
                            continuation.resume(ImageCaptureResult.Success(byteArray, bitmap))
                        }
                    }
                }

                override fun onError(exc: ImageCaptureException) {
                    Log.e("CameraK", "Image capture failed: ${exc.message}", exc)
                    pendingCaptures.decrementAndGet()
                    cacheFile.delete()
                    continuation.resume(ImageCaptureResult.Error(exc))
                }
            },
        ) ?: run {
            pendingCaptures.decrementAndGet()
            cacheFile.delete()
            continuation.resume(ImageCaptureResult.Error(Exception("ImageCapture use case is not initialized.")))
        }
    }

    /**
     * Decodes a JPEG file to [ImageBitmap] and applies EXIF orientation so the thumbnail
     * matches the correct display orientation (e.g. EXIF 6 = 90° CW is applied to pixels).
     *
     * @param ignoreMirrorInExif When true, skips EXIF steps that horizontally flip pixels.
     * Use for **front** captures where [ImageCapture.Metadata.isReversedHorizontal] was already
     * applied on encode; re-applying EXIF mirror would double-flip the thumbnail.
     */
    private fun decodeThumbnailBitmapWithExifRotation(
        path: String,
        ignoreMirrorInExif: Boolean = false,
    ): ImageBitmap? {
        var bitmap = BitmapFactory.decodeFile(path, null) ?: return null
        try {
            val orientation = ExifInterface(path).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL,
            )
            if (orientation != ExifInterface.ORIENTATION_NORMAL &&
                orientation != ExifInterface.ORIENTATION_UNDEFINED
            ) {
                val matrix = Matrix()
                when (orientation) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                    ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                    ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                    ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> {
                        if (!ignoreMirrorInExif) matrix.postScale(-1f, 1f)
                    }
                    ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
                    ExifInterface.ORIENTATION_TRANSPOSE -> {
                        matrix.postRotate(90f)
                        if (!ignoreMirrorInExif) matrix.postScale(-1f, 1f)
                    }
                    ExifInterface.ORIENTATION_TRANSVERSE -> {
                        matrix.postRotate(270f)
                        if (!ignoreMirrorInExif) matrix.postScale(-1f, 1f)
                    }
                }
                if (!matrix.isIdentity) {
                    val transformed = Bitmap.createBitmap(
                        bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true,
                    )
                    bitmap.recycle()
                    bitmap = transformed
                }
            }
            return bitmap.asImageBitmap()
        } catch (e: Exception) {
            bitmap.recycle()
            throw e
        }
    }

    /**
     * Perform the actual image capture with constant quality
     */
    private fun performCapture(continuation: CancellableContinuation<ImageCaptureResult>, quality: Int) {
        val outputOptions = ImageCapture.OutputFileOptions.Builder(createTempFile()).build()

        imageCapture?.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    if (returnFilePath) {
                        // File path mode: Return actual file path from savedUri
                        val fileUri = output.savedUri
                        if (fileUri != null) {
                            // Convert content:// URI to actual file path
                            val filePath = if (fileUri.scheme == "file") {
                                fileUri.path
                            } else {
                                // For content:// URIs, get the real path
                                val projection = arrayOf(android.provider.MediaStore.Images.Media.DATA)
                                context.contentResolver.query(fileUri, projection, null, null, null)?.use { cursor ->
                                    val columnIndex = cursor.getColumnIndexOrThrow(
                                        android.provider.MediaStore.Images.Media.DATA,
                                    )
                                    if (cursor.moveToFirst()) cursor.getString(columnIndex) else null
                                } ?: fileUri.path
                            }

                            if (filePath != null) {
                                imageProcessingExecutor.execute {
                                    val byteArray = try {
                                        java.io.File(filePath).readBytes()
                                    } catch (e: Exception) {
                                        Handler(Looper.getMainLooper()).post {
                                            pendingCaptures.decrementAndGet()
                                            continuation.resume(ImageCaptureResult.Error(e))
                                        }
                                        return@execute
                                    }
                                    val bitmap = try {
                                        decodeThumbnailBitmapWithExifRotation(filePath)
                                    } catch (e: Exception) {
                                        Log.e("CameraK", "Failed to decode bitmap from file", e)
                                        null
                                    }
                                    Handler(Looper.getMainLooper()).post {
                                        pendingCaptures.decrementAndGet()
                                        continuation.resume(ImageCaptureResult.Success(byteArray, bitmap))
                                    }
                                }
                            } else {
                                Log.e("CameraK", "Failed to get file path from capture result")
                                continuation.resume(ImageCaptureResult.Error(Exception("Failed to get file path")))
                                pendingCaptures.decrementAndGet()
                            }
                        } else {
                            Log.e("CameraK", "Capture result has no savedUri")
                            continuation.resume(ImageCaptureResult.Error(Exception("No file URI returned")))
                            pendingCaptures.decrementAndGet()
                        }
                    } else {
                        // ByteArray mode: Process and return ByteArray
                        imageProcessingExecutor.execute {
                            try {
                                val byteArray = processImageOutput(output, quality)

                                if (byteArray != null) {
                                    imageCaptureListeners.forEach { it(byteArray) }
                                    val bitmap = try {
                                        byteArray.toImageBitmap()
                                    } catch (e: Exception) {
                                        Log.e("CameraK", "Failed to decode bitmap", e)
                                        null
                                    }
                                    continuation.resume(ImageCaptureResult.Success(byteArray, bitmap))
                                } else {
                                    Log.e("CameraK", "Failed to convert image to ByteArray")
                                    continuation.resume(
                                        ImageCaptureResult.Error(Exception("Failed to convert image to ByteArray.")),
                                    )
                                }
                            } finally {
                                pendingCaptures.decrementAndGet()
                            }
                        }
                    }
                }

                override fun onError(exc: ImageCaptureException) {
                    Log.e("CameraK", "Image capture failed: ${exc.message}", exc)
                    pendingCaptures.decrementAndGet()
                    continuation.resume(ImageCaptureResult.Error(exc))
                }
            },
        ) ?: run {
            pendingCaptures.decrementAndGet()
            continuation.resume(ImageCaptureResult.Error(Exception("ImageCapture use case is not initialized.")))
        }
    }

    /**
     * Process the saved image output with optimized approach.
     *
     * CameraX should apply targetRotation to the output file, but some devices (Samsung)
     * strip EXIF data or apply rotation inconsistently. We verify EXIF orientation first.
     *
     * Fast path: Direct file read when EXIF orientation is correct (NORMAL)
     * Slow path: Process when format conversion or rotation is needed
     */
    private fun processImageOutput(output: ImageCapture.OutputFileResults, quality: Int): ByteArray? = try {
        output.savedUri?.let { uri ->
            val tempFile = java.io.File.createTempFile("temp_image", ".jpg", context.cacheDir)
            context.contentResolver.openInputStream(uri)?.use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            try {
                // Check EXIF orientation - Samsung devices may have incorrect orientation
                val exif = ExifInterface(tempFile.absolutePath)
                val orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL,
                )

                val needsRotation = orientation != ExifInterface.ORIENTATION_NORMAL &&
                    orientation != ExifInterface.ORIENTATION_UNDEFINED
                val needsProcessing = imageFormat == ImageFormat.PNG || needsRotation

                if (!needsProcessing) {
                    // Fast path: EXIF orientation is correct, read file bytes directly
                    // This avoids decode→rotate→re-encode cycle (saves 2-3 seconds and quality loss)
                    tempFile.readBytes().also { tempFile.delete() }
                } else {
                    // Slow path: format conversion or rotation; optional downsample only during burst
                    val options = BitmapFactory.Options().apply {
                        if (pendingCaptures.value > 1) {
                            inJustDecodeBounds = true
                            BitmapFactory.decodeFile(tempFile.absolutePath, this)
                            inSampleSize = calculateSampleSizeForBurst()
                            inJustDecodeBounds = false
                        }
                    }

                    val originalBitmap = BitmapFactory.decodeFile(tempFile.absolutePath, options)
                    tempFile.delete()

                    // Apply rotation if needed (Samsung device fix)
                    val rotatedBitmap = if (originalBitmap != null && needsRotation) {
                        val rotationAngle = when (orientation) {
                            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                            else -> 0f
                        }

                        if (rotationAngle != 0f) {
                            Matrix().apply { postRotate(rotationAngle) }.let { matrix ->
                                Bitmap.createBitmap(
                                    originalBitmap,
                                    0,
                                    0,
                                    originalBitmap.width,
                                    originalBitmap.height,
                                    matrix,
                                    true,
                                ).also { originalBitmap.recycle() }
                            }
                        } else {
                            originalBitmap
                        }
                    } else {
                        originalBitmap
                    }

                    // Convert format and compress
                    rotatedBitmap?.compressToByteArray(
                        format = when (imageFormat) {
                            ImageFormat.JPEG -> Bitmap.CompressFormat.JPEG
                            ImageFormat.PNG -> Bitmap.CompressFormat.PNG
                        },
                        quality = quality,
                        recycleInput = true,
                    )
                }
            } catch (e: Exception) {
                tempFile.delete()
                throw e
            }
        }
    } catch (e: Exception) {
        Log.e("CameraK", "Error processing image output: ${e.message}", e)
        null
    }

    /**
     * Full resolution by default; 2x downsample only when multiple captures are in flight.
     */
    private fun calculateSampleSizeForBurst(): Int = if (pendingCaptures.value > 1) 2 else 1

    actual fun toggleFlashMode() {
        flashMode = when (flashMode) {
            FlashMode.ON -> FlashMode.OFF
            else -> FlashMode.ON  // OFF or AUTO -> ON
        }
        applyImageCaptureFlashAndTorchForCurrentState()
    }

    actual fun setFlashMode(mode: FlashMode) {
        flashMode = mode
        applyImageCaptureFlashAndTorchForCurrentState()
    }

    actual fun getFlashMode(): FlashMode? = flashMode

    actual fun toggleTorchMode() {
        torchMode = when (torchMode) {
            TorchMode.OFF -> TorchMode.ON
            TorchMode.ON -> TorchMode.AUTO
            TorchMode.AUTO -> TorchMode.OFF
        }
        if (torchMode == TorchMode.AUTO) {
            Log.w("CameraK", "TorchMode.AUTO not natively supported, using ON")
        }
        applyPreviewTorchPolicy()
    }

    actual fun setTorchMode(mode: TorchMode) {
        torchMode = mode
        if (mode == TorchMode.AUTO) {
            Log.w("CameraK", "TorchMode.AUTO not natively supported, using ON")
        }
        applyPreviewTorchPolicy()
    }

    actual fun setZoom(zoomRatio: Float) {
        val zoomState = camera?.cameraInfo?.zoomState?.value
        val minZoom = zoomState?.minZoomRatio ?: 1f
        val maxZoom = zoomState?.maxZoomRatio ?: 1f
        camera?.cameraControl?.setZoomRatio(zoomRatio.coerceIn(minZoom, maxZoom))
    }

    actual fun getZoom(): Float = camera?.cameraInfo?.zoomState?.value?.zoomRatio ?: 1f

    actual fun getMaxZoom(): Float = camera?.cameraInfo?.zoomState?.value?.maxZoomRatio ?: 1f

    actual fun setFocusPoint(normalizedX: Float, normalizedY: Float) {
        val view = previewView ?: return
        val w = view.width.toFloat()
        val h = view.height.toFloat()
        if (w <= 0f || h <= 0f) return
        val viewX = normalizedX.coerceIn(0f, 1f) * w
        val viewY = normalizedY.coerceIn(0f, 1f) * h
        try {
            val factory = view.meteringPointFactory
            val point = factory.createPoint(viewX, viewY)
            val action = FocusMeteringAction.Builder(point).build()
            camera?.cameraControl?.startFocusAndMetering(action)
        } catch (e: Exception) {
            Log.w("CameraK", "setFocusPoint failed: ${e.message}")
        }
    }

    actual fun getExposureCompensationRange(): Pair<Int, Int> {
        val state = camera?.cameraInfo?.exposureState ?: return Pair(0, 0)
        if (!state.isExposureCompensationSupported) return Pair(0, 0)
        val range = state.exposureCompensationRange
        return Pair(range.lower, range.upper)
    }

    actual fun setExposureCompensationIndex(index: Int) {
        val (min, max) = getExposureCompensationRange()
        if (min == 0 && max == 0) return
        val clamped = index.coerceIn(min, max)
        camera?.cameraControl?.setExposureCompensationIndex(clamped)
    }

    actual fun getExposureCompensationIndex(): Int =
        camera?.cameraInfo?.exposureState?.exposureCompensationIndex ?: 0

    actual fun getTorchMode(): TorchMode? = torchMode

    actual fun toggleCameraLens() {
        memoryManager.updateMemoryStatus()

        if (memoryManager.isUnderMemoryPressure()) {
            memoryManager.clearBufferPools()
            System.gc()
        }

        val previousLens = cameraLens
        val nextLens = if (cameraLens == CameraLens.BACK) CameraLens.FRONT else CameraLens.BACK
        cameraLens = nextLens
        attachVideoToCameraSession = captureModeIsVideo
        if (cameraLens == CameraLens.FRONT) {
            flashMode = FlashMode.OFF
        }
        val view = previewView
        if (view == null) return
        bindCamera(view) {
            if (camera == null) {
                Log.e("CameraK", "toggleCameraLens: bind failed for $nextLens; reverting to $previousLens")
                cameraLens = previousLens
                attachVideoToCameraSession = captureModeIsVideo
                bindCamera(view)
            }
        }
    }

    actual fun getCameraLens(): CameraLens? = cameraLens

    actual fun getImageFormat(): ImageFormat = imageFormat

    actual fun getQualityPrioritization(): QualityPrioritization = qualityPriority

    actual fun getPreferredCameraDeviceType(): CameraDeviceType = cameraDeviceType

    actual fun setPreferredCameraDeviceType(deviceType: CameraDeviceType) {
        if (cameraDeviceType == deviceType) return
        cameraDeviceType = deviceType
        isSwitchingDeviceType = true
        onDeviceTypeSwitchTransition?.invoke(true)
        previewView?.let { bindCamera(it) } ?: run {
            isSwitchingDeviceType = false
            onDeviceTypeSwitchTransition?.invoke(false)
        }
    }

    actual fun startSession() {
        memoryManager.updateMemoryStatus()
        memoryManager.clearBufferPools()
        initializeControllerPlugins()
    }

    actual fun stopSession() {
        cameraProvider?.unbindAll()
        memoryManager.clearBufferPools()
    }

    actual fun addImageCaptureListener(listener: (ByteArray) -> Unit) {
        imageCaptureListeners.add(listener)
    }

    actual fun applyCaptureModeSessionPreset(isVideoMode: Boolean) {
        if (captureModeIsVideo == isVideoMode) return
        captureModeIsVideo = isVideoMode
        val wantAttach = isVideoMode
        if (attachVideoToCameraSession == wantAttach) return
        updateVideoAttachmentForCaptureMode()
        Log.d(
            "CameraK",
            "applyCaptureModeSessionPreset isVideoMode=$isVideoMode lens=$cameraLens attachVideo=$attachVideoToCameraSession",
        )
        previewView?.let { view ->
            Handler(Looper.getMainLooper()).post { bindCamera(view) }
        }
    }

    actual fun setPreviewStabilizationEnabled(enabled: Boolean) {
        if (previewStabilizationEnabled == enabled) return
        previewStabilizationEnabled = enabled
        previewView?.let { bindCamera(it) }
    }

    actual fun isNightModeSupported(): Boolean = false

    actual fun setNightMode(enabled: Boolean) {}

    private var wideSelfieEnabled = false

    actual fun setWideSelfieMode(enabled: Boolean) {
        if (wideSelfieEnabled == enabled) return
        wideSelfieEnabled = enabled
    }

    actual fun isWideSelfieEnabled(): Boolean = wideSelfieEnabled

    actual fun initializeControllerPlugins() {
        plugins.forEach { it.initialize(this) }
    }

    private fun createTempFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())

        // Use configured directory
        val storageDir = when (directory) {
            Directory.PICTURES -> android.os.Environment.getExternalStoragePublicDirectory(
                android.os.Environment.DIRECTORY_PICTURES,
            )
            Directory.DCIM -> android.os.Environment.getExternalStoragePublicDirectory(
                android.os.Environment.DIRECTORY_DCIM,
            )
            Directory.DOCUMENTS -> context.getExternalFilesDir(null) ?: context.filesDir
        }

        // Create CameraK subdirectory
        val cameraKDir = File(storageDir, "CameraK")
        if (!cameraKDir.exists()) {
            cameraKDir.mkdirs()
        }

        return File(
            cameraKDir,
            "IMG_$timeStamp.${imageFormat.extension}",
        )
    }

    /**
     * Notifies Android's MediaStore about a new image file so it appears in Gallery.
     * Uses MediaScannerConnection for compatibility across Android versions.
     */
    private fun notifyMediaStore(file: File) {
        try {
            android.media.MediaScannerConnection.scanFile(
                context,
                arrayOf(file.absolutePath),
                arrayOf(
                    when (imageFormat) {
                        ImageFormat.JPEG -> "image/jpeg"
                        ImageFormat.PNG -> "image/png"
                    },
                ),
                null,
            )
            Log.d("CameraK", "MediaStore notified: ${file.absolutePath}")
        } catch (e: Exception) {
            Log.e("CameraK", "Failed to notify MediaStore: ${e.message}")
        }
    }

    /**
     * Same mapping as CameraK; uses ImageCapture constants (same CameraX version).
     */
    private fun FlashMode.toCameraXFlashMode(): Int = when (this) {
        FlashMode.ON -> ImageCapture.FLASH_MODE_ON
        FlashMode.OFF -> ImageCapture.FLASH_MODE_OFF
        FlashMode.AUTO -> ImageCapture.FLASH_MODE_AUTO
    }

    private fun CameraLens.toCameraXLensFacing(): Int = when (this) {
        CameraLens.FRONT -> CameraSelector.LENS_FACING_FRONT
        CameraLens.BACK -> CameraSelector.LENS_FACING_BACK
    }

    // ═══════════════════════════════════════════════════════════════
    // Video Recording
    // ═══════════════════════════════════════════════════════════════

    /**
     * Enables EIS on the encoded video stream when [previewStabilizationEnabled] is true (video mode).
     * Preview-only stabilization does not match native camera quality on many OEMs.
     */
    private fun VideoCapture.Builder<Recorder>.applyVideoStabilizationWhenEnabled(
        bindSelector: CameraSelector,
        targetFrameRateFps: Int,
    ): VideoCapture.Builder<Recorder> {
        if (!previewStabilizationEnabled) return this
        // CameraX/HAL typically stabilizes encoded video only at <= 30 fps; 60 fps + EIS often muxes at 30.
        if (targetFrameRateFps > 30) {
            Log.d(
                "CameraK",
                "videoStabilization: skipped (targetFps=$targetFrameRateFps); use preview EIS only for 60 fps",
            )
            return this
        }
        val provider = cameraProvider ?: return this
        return try {
            val cameraInfo = provider.getCameraInfo(bindSelector)
            val supported = Recorder.getVideoCapabilities(cameraInfo).isStabilizationSupported
            Log.d("CameraK", "videoStabilization: deviceSupported=$supported targetFps=$targetFrameRateFps")
            if (supported) {
                setVideoStabilizationEnabled(true)
            } else {
                this
            }
        } catch (e: Exception) {
            Log.w("CameraK", "videoStabilization: not applied (${e.message})")
            this
        }
    }

    private fun configureVideoCaptureUseCase(
        quality: VideoQuality = VideoQuality.FHD,
        targetFrameRateFps: Int,
        bindSelector: CameraSelector,
    ) {
        try {
            val cameraXQuality = when (quality) {
                VideoQuality.SD -> Quality.SD
                VideoQuality.HD -> Quality.HD
                VideoQuality.FHD -> Quality.FHD
                VideoQuality.UHD -> Quality.UHD
            }
            val recorderBuilder = Recorder.Builder()
                .setQualitySelector(
                    QualitySelector.from(
                        cameraXQuality,
                        FallbackStrategy.higherQualityOrLowerThan(cameraXQuality),
                    ),
                )
            val recorder = if (targetFrameRateFps == 60) {
                recorderBuilder.setTargetVideoEncodingBitRate(22_000_000)
            } else {
                recorderBuilder
            }.build()
            /**
             * Prefer a fixed 60 when probing chose 60 and video EIS is off (EIS forces ~30 fps).
             * Fall back to [30, 60] only if [60, 60] build fails on the device.
             */
            val fpsRange = if (targetFrameRateFps == 60) {
                Range.create(60, 60)
            } else {
                Range.create(targetFrameRateFps, targetFrameRateFps)
            }
            var appliedMode = "explicitFps"
            videoCapture = try {
                val built = VideoCapture.Builder(recorder)
                    .applyVideoStabilizationWhenEnabled(bindSelector, targetFrameRateFps)
                    .setTargetFrameRate(fpsRange)
                    .build()
                Log.d(
                    "CameraK",
                    "videoCaptureBuild: success setTargetFrameRate requested=$targetFrameRateFps " +
                        "cameraXQuality=$cameraXQuality " +
                        "range=[${fpsRange.lower},${fpsRange.upper}] " +
                        if (targetFrameRateFps == 60) "recorderBitrateBps=22000000 " else "",
                )
                built
            } catch (e: Exception) {
                if (targetFrameRateFps == 60) {
                    Log.w("CameraK", "videoCaptureBuild: [60,60] failed (${e.message}) → trying [30,60] then 30")
                    try {
                        appliedMode = "fallbackRange30to60After60FixedFailed"
                        VideoCapture.Builder(recorder)
                            .applyVideoStabilizationWhenEnabled(bindSelector, targetFrameRateFps)
                            .setTargetFrameRate(Range.create(30, 60))
                            .build()
                            .also {
                                Log.d("CameraK", "videoCaptureBuild: success setTargetFrameRate range=[30,60]")
                            }
                    } catch (eRange: Exception) {
                        try {
                            appliedMode = "fallbackExplicit30After60Failed"
                            VideoCapture.Builder(recorder)
                                .applyVideoStabilizationWhenEnabled(bindSelector, 30)
                                .setTargetFrameRate(Range.create(30, 30))
                                .build()
                                .also {
                                    Log.d("CameraK", "videoCaptureBuild: success setTargetFrameRate fps=30")
                                }
                        } catch (e2: Exception) {
                            appliedMode = "defaultNoTargetFpsAfter30Failed"
                            Log.w(
                                "CameraK",
                                "videoCaptureBuild: 30 fps failed (${e2.message}) → builder without fps hint",
                            )
                            VideoCapture.Builder(recorder)
                                .applyVideoStabilizationWhenEnabled(bindSelector, 30)
                                .build()
                        }
                    }
                } else {
                    appliedMode = "defaultNoTargetFps"
                    Log.w(
                        "CameraK",
                        "videoCaptureBuild: ${targetFrameRateFps} fps failed (${e.message}) → builder without fps hint",
                    )
                    VideoCapture.Builder(recorder)
                        .applyVideoStabilizationWhenEnabled(bindSelector, targetFrameRateFps)
                        .build()
                }
            }
            Log.d(
                "CameraK",
                "videoCaptureBuild: summary lens=$cameraLens quality=$quality " +
                    "(${quality.width}x${quality.height}) requestedFps=$targetFrameRateFps appliedMode=$appliedMode",
            )
        } catch (e: Exception) {
            Log.w("CameraK", "VideoCapture use case not supported: ${e.message}")
            videoCapture = null
        }
    }

    actual suspend fun captureRecordingThumbnailFrame(): ImageBitmap? =
        suspendCancellableCoroutine { continuation ->
            Handler(Looper.getMainLooper()).post {
                val bitmap = previewView?.bitmap?.copy(Bitmap.Config.ARGB_8888, false)
                continuation.resume(bitmap?.asImageBitmap())
            }
        }

    actual suspend fun extractVideoThumbnailFromFile(filePath: String, isFrontCamera: Boolean): ImageBitmap? = null

    @android.annotation.SuppressLint("MissingPermission")
    actual suspend fun startRecording(configuration: VideoConfiguration): String {
        return suspendCancellableCoroutine { cont ->
        val vc = videoCapture ?: run {
            cont.resumeWithException(IllegalStateException("VideoCapture use case not available"))
            return@suspendCancellableCoroutine
        }

        recordingUsedFrontLens = cameraLens == CameraLens.FRONT
        Log.d(
            "CameraK",
            "[RindleVideoDbg] phase=RECORD_START lens=$cameraLens front=$recordingUsedFrontLens " +
                "includeVideoInSession=${shouldIncludeVideoInCameraSession()}",
        )
        val outputFile = createVideoOutputFile(configuration)
        recordingOutputFile = outputFile
        val outputOptions = FileOutputOptions.Builder(outputFile).build()

        val hasAudioPermission = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.RECORD_AUDIO,
        ) == PackageManager.PERMISSION_GRANTED

        val pendingRecording = vc.output.prepareRecording(context, outputOptions).apply {
            if (configuration.enableAudio && hasAudioPermission) {
                withAudioEnabled()
            }
        }

        activeRecording = pendingRecording.start(
            ContextCompat.getMainExecutor(context),
        ) { event ->
            when (event) {
                is VideoRecordEvent.Finalize -> {
                    val file = recordingOutputFile
                    if (event.hasError()) {
                        recordingFinalizeChannel.trySend(
                            VideoCaptureResult.Error(
                                Exception("Recording error code: ${event.error}"),
                            ),
                        )
                    } else {
                        val path = file?.absolutePath ?: ""
                        Log.d(
                            "CameraK",
                            "[RindleVideoDbg] phase=RECORD_FINALIZE_RAW | ${VideoFileDbgProbe.describe(path)}",
                        )
                        recordingFinalizeChannel.trySend(
                            VideoCaptureResult.Success(
                                filePath = path,
                                durationMs = event.recordingStats.recordedDurationNanos / 1_000_000,
                            ),
                        )
                    }
                    recordingOutputFile = null
                }
            }
        }

        cont.resume(outputFile.absolutePath)
        }
    }

    actual suspend fun stopRecording(): VideoCaptureResult {
        val recording = activeRecording ?: return VideoCaptureResult.Error(
            IllegalStateException("No active recording"),
        )
        // Handed to applyRecordingPostProcessing(): the lens can already have been switched by the
        // time the finished file is post-processed.
        stoppedRecordingUsedFrontLens = recordingUsedFrontLens
        recordingUsedFrontLens = false
        recording.stop()
        activeRecording = null
        return recordingFinalizeChannel.receive()
    }

    actual suspend fun applyRecordingPostProcessing(result: VideoCaptureResult): VideoCaptureResult {
        val recordedWithFrontLens = stoppedRecordingUsedFrontLens
        stoppedRecordingUsedFrontLens = false
        if (!recordedWithFrontLens || result !is VideoCaptureResult.Success) {
            return result
        }
        val mirrored = withContext(NonCancellable) {
            FrontCameraVideoExportHelper.mirrorToMatchPreviewInPlace(
                context = context,
                filePath = result.filePath,
            )
        }
        if (!mirrored) {
            Log.w(
                "CameraK",
                "[RindleVideoDbg] phase=RECORD_MIRROR_EXPORT_FAILED path=${result.filePath}",
            )
        } else {
            Log.d(
                "CameraK",
                "[RindleVideoDbg] phase=RECORD_MIRROR_EXPORT_OK | ${VideoFileDbgProbe.describe(result.filePath)}",
            )
        }
        return result
    }

    actual suspend fun pauseRecording() {
        activeRecording?.pause()
    }

    actual suspend fun resumeRecording() {
        activeRecording?.resume()
    }

    private fun createVideoOutputFile(config: VideoConfiguration): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val dir = if (config.outputDirectory != null) {
            File(config.outputDirectory).also { it.mkdirs() }
        } else {
            val storageDir = when (directory) {
                Directory.PICTURES, Directory.DCIM -> Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_MOVIES,
                )
                Directory.DOCUMENTS -> context.getExternalFilesDir(null) ?: context.filesDir
            }
            File(storageDir, "CameraK").also { it.mkdirs() }
        }
        return File(dir, "${config.filePrefix}_$timeStamp.mp4")
    }

    /**
     * Clean up resources when no longer needed
     * Should be called when the controller is being destroyed
     */
    actual fun cleanup() {
        activeRecording?.stop()
        activeRecording = null
        registeredAnalyzers.clear()
        recordingFinalizeChannel.close()
        imageProcessingExecutor.shutdown()
        memoryManager.clearBufferPools()
    }
}

/**
 * Dispatches each camera frame to multiple [ImageAnalysis.Analyzer] instances.
 * Uses [RefCountedImageProxy] so each analyzer can call `close()` independently;
 * the underlying buffer is released only when all analyzers have finished.
 */
private class MultiplexingAnalyzer(
    private val analyzers: List<ImageAnalysis.Analyzer>,
) : ImageAnalysis.Analyzer {
    override fun analyze(imageProxy: ImageProxy) {
        if (analyzers.isEmpty()) {
            imageProxy.close()
            return
        }
        if (analyzers.size == 1) {
            analyzers[0].analyze(imageProxy)
            return
        }
        val refCount = java.util.concurrent.atomic.AtomicInteger(analyzers.size)
        for (analyzer in analyzers) {
            analyzer.analyze(RefCountedImageProxy(imageProxy, refCount))
        }
    }
}

/**
 * Wraps an [ImageProxy] with reference-counted close semantics.
 * The real proxy is closed only when the last holder calls [close].
 */
private class RefCountedImageProxy(
    private val delegate: ImageProxy,
    private val refCount: java.util.concurrent.atomic.AtomicInteger,
) : ImageProxy by delegate {
    override fun close() {
        if (refCount.decrementAndGet() <= 0) {
            delegate.close()
        }
    }
}
