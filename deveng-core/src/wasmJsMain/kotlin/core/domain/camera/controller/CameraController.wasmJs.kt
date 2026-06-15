package core.domain.camera.controller

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import core.domain.camera.ensureWasmCameraBootstrap
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
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.js.JsAny
import kotlin.time.ExperimentalTime
import kotlinx.browser.document
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.skia.Image
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLVideoElement

/**
 * Browser camera via [getUserMedia](https://developer.mozilla.org/en-US/docs/Web/API/MediaDevices/getUserMedia).
 *
 * ## Preview rendering — BlendMode.Clear DOM overlay
 *
 * The `<video>` element is mounted on `document.documentElement` (behind the Compose
 * canvas at `z-index: -1`). [core.domain.camera.compose.CameraPreviewView] draws a
 * `BlendMode.Clear` rectangle that erases Skia's opaque-white pixels in its bounds, and
 * the browser then composites the DOM video natively through the hole. No per-frame work
 * happens in WASM during steady-state preview; the browser's media decoder + compositor
 * handles everything.
 *
 * Mounting on `document.documentElement` rather than `document.body` is required because
 * `ComposeViewport` attaches a shadow root to `<body>` with no `<slot>`, which hides any
 * direct children of `body`.
 *
 * ## Photo capture
 *
 * Capture uses a non-DOM canvas as a scratch surface: `drawImage(video, …)` →
 * `canvas.toDataURL('image/jpeg')` → Base64 → `ByteArray`. The same canvas is exposed
 * `internal` so module-internal plugins (e.g. QR scanner) can sample frames without
 * owning their own canvas.
 *
 * ## Video recording
 *
 * WebM-via-MediaRecorder is not implemented yet; `startRecording` throws and
 * `stopRecording` returns an error result.
 */
@OptIn(ExperimentalTime::class)
actual class CameraController internal constructor(
    internal var plugins: MutableList<CameraPlugin>,
    private val imageFormat: ImageFormat,
    @Suppress("unused")
    private val directory: Directory,
    private val qualityPriority: QualityPrioritization,
    initialLens: CameraLens,
) {
    actual val usesPhotoCaptureForVideoThumbnail: Boolean = false

    private var currentLens: CameraLens = initialLens
    private var mediaStream: JsAny? = null

    /**
     * Full-viewport `<video>` mounted behind the Compose canvas on
     * `document.documentElement`. Exposed `internal` so module-internal plugins (e.g. the
     * QR scanner) can read frames at full resolution without owning their own DOM nodes.
     */
    internal val video: HTMLVideoElement =
        document.createElement("video").unsafeCast<HTMLVideoElement>()

    /** Scratch canvas for plugins that need to sample frames. */
    internal val canvas: HTMLCanvasElement =
        document.createElement("canvas").unsafeCast<HTMLCanvasElement>()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private var listener: (ByteArray) -> Unit = { }

    init {
        ensureWasmCameraBootstrap()
        video.id = "deveng-wasm-camera-video"
        video.setAttribute("playsinline", "")
        video.setAttribute("webkit-playsinline", "")
        video.setAttribute("autoplay", "")
        video.setAttribute(
            "style",
            "position:fixed;top:0;left:0;width:100vw;height:100vh;" +
                "object-fit:cover;z-index:-1;pointer-events:none;background:#000;",
        )
        video.muted = true
    }

    actual var onPreviewTapListener: ((Float, Float) -> Unit)? = null
    actual var onPreviewDoubleTapListener: (() -> Unit)? = null
    actual var shouldSuppressTapToFocus: ((Float, Float) -> Boolean)? = null

    @Deprecated(
        message = "Use takePictureToFile() instead for better performance",
        replaceWith = ReplaceWith("takePictureToFile()"),
        level = DeprecationLevel.WARNING,
    )
    actual suspend fun takePicture(): ImageCaptureResult = takePictureToFile()

    @OptIn(ExperimentalEncodingApi::class)
    actual suspend fun takePictureToFile(): ImageCaptureResult = withContext(Dispatchers.Default) {
        try {
            val bytes = captureFrameJpeg()
            if (bytes.isEmpty()) {
                return@withContext ImageCaptureResult.Error(
                    IllegalStateException("No camera frame (is the stream ready?)"),
                )
            }
            listener(bytes)
            val bmp = Image.makeFromEncoded(bytes).toComposeImageBitmap()
            ImageCaptureResult.Success(byteArray = bytes, bitmap = bmp)
        } catch (e: Exception) {
            ImageCaptureResult.Error(e)
        }
    }

    actual fun toggleFlashMode() {}
    actual fun setFlashMode(mode: FlashMode) {}
    actual fun getFlashMode(): FlashMode? = FlashMode.OFF
    actual fun toggleTorchMode() {}
    actual fun setTorchMode(mode: TorchMode) {}
    actual fun getTorchMode(): TorchMode? = null

    actual fun toggleCameraLens() {
        currentLens = if (currentLens == CameraLens.BACK) CameraLens.FRONT else CameraLens.BACK
        scope.launch {
            runCatching { openCamera() }
        }
    }

    actual fun getCameraLens(): CameraLens? = currentLens
    actual fun getImageFormat(): ImageFormat = imageFormat
    actual fun getQualityPrioritization(): QualityPrioritization = qualityPriority
    actual fun getPreferredCameraDeviceType(): CameraDeviceType = CameraDeviceType.DEFAULT
    actual fun setPreferredCameraDeviceType(deviceType: CameraDeviceType) {}
    actual fun setZoom(zoomRatio: Float) {}
    actual fun getZoom(): Float = 1f
    actual fun getMaxZoom(): Float = 1f
    actual fun setFocusPoint(normalizedX: Float, normalizedY: Float) {}
    actual fun getExposureCompensationRange(): Pair<Int, Int> = 0 to 0
    actual fun setExposureCompensationIndex(index: Int) {}
    actual fun getExposureCompensationIndex(): Int = 0

    actual fun startSession() {
        if (video.parentNode == null) {
            document.documentElement?.appendChild(video)
        }
        scope.launch {
            runCatching { openCamera() }
        }
    }

    actual fun stopSession() {
        stopTracks()
        video.unsafeCast<WasmHtmlVideoSrc>().srcObject = null
        try {
            video.parentNode?.removeChild(video)
        } catch (_: Throwable) {
        }
    }

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
        plugins.forEach { it.initialize(this) }
    }

    actual fun cleanup() {
        stopSession()
        scope.cancel()
    }

    actual suspend fun captureRecordingThumbnailFrame(): ImageBitmap? = null
    actual suspend fun extractVideoThumbnailFromFile(
        filePath: String,
        isFrontCamera: Boolean,
    ): ImageBitmap? = null

    actual suspend fun startRecording(configuration: VideoConfiguration): String {
        throw UnsupportedOperationException(
            "Video recording on WASM is not implemented yet; use photo mode.",
        )
    }

    actual suspend fun stopRecording(): VideoCaptureResult =
        VideoCaptureResult.Error(IllegalStateException("Not recording"))

    actual suspend fun pauseRecording() {}
    actual suspend fun resumeRecording() {}

    private suspend fun openCamera() {
        stopTracks()
        val stream = getUserMediaSwitchingLens(currentLens)
        mediaStream = stream
        wasmSetVideoSrcObject(video, stream)
        runCatching { awaitVoidPromise(wasmVideoPlayPromise(video)) }
    }

    @OptIn(ExperimentalEncodingApi::class)
    private fun captureFrameJpeg(): ByteArray {
        if (!wasmDrawVideoOnCanvas(canvas, video)) return ByteArray(0)
        val dataUrl = when (imageFormat) {
            ImageFormat.PNG -> canvas.toDataURL("image/png")
            ImageFormat.JPEG -> canvas.toDataURL("image/jpeg")
        }
        val prefix = when (imageFormat) {
            ImageFormat.PNG -> "data:image/png;base64,"
            ImageFormat.JPEG -> "data:image/jpeg;base64,"
        }
        if (!dataUrl.startsWith(prefix)) return ByteArray(0)
        val b64 = dataUrl.substring(prefix.length)
        return Base64.decode(b64)
    }

    private fun stopTracks() {
        val stream = (video.unsafeCast<WasmHtmlVideoSrc>().srcObject ?: mediaStream)
            ?: return
        wasmStopAllTracks(stream)
        mediaStream = null
    }

    private suspend fun getUserMediaSwitchingLens(lens: CameraLens): JsAny = suspendCoroutine { cont ->
        val md = wasmNavigatorMediaDevices()
        if (md == null) {
            cont.resumeWithException(
                IllegalStateException("Camera API unavailable. Use HTTPS or localhost."),
            )
            return@suspendCoroutine
        }
        val constraints = when (lens) {
            CameraLens.FRONT -> wasmConstraintsUserFacing()
            else -> wasmConstraintsEnvironmentFacing()
        }
        val p = md.getUserMedia(constraints)
        p.then(
            onFulfilled = { stream: JsAny? ->
                if (stream == null) {
                    cont.resumeWithException(Exception("getUserMedia returned null"))
                } else {
                    cont.resume(stream)
                }
                null
            },
            onRejected = { err: JsAny? ->
                cont.resumeWithException(Exception(wasmErrorMessage(err)))
                null
            },
        )
    }

    private suspend fun awaitVoidPromise(p: WasmJsPromiseLike?) = suspendCoroutine { cont ->
        if (p == null) {
            cont.resume(Unit)
            return@suspendCoroutine
        }
        p.then(
            onFulfilled = { _: JsAny? ->
                cont.resume(Unit)
                null
            },
            onRejected = { err: JsAny? ->
                cont.resumeWithException(Exception(wasmErrorMessage(err)))
                null
            },
        )
    }
}

/** Stops every track on a [MediaStream] passed as [JsAny] (interop). */
private fun wasmStopAllTracks(stream: JsAny) {
    TracksOwner.stop(stream)
}

/** Nested object so we can use a single top-level [js] getter for the stop function. */
private object TracksOwner {
    fun stop(stream: JsAny): Unit = wasmStopTracksFn()(stream)
}

private fun wasmStopTracksFn(): (JsAny) -> Unit =
    js("(stream) => { try { stream.getTracks().forEach(t => t.stop()); } catch (e) {} }")
