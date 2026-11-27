package core.domain.camera.controller

import kotlin.js.JsAny
import kotlin.js.unsafeCast
import kotlinx.browser.window
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLVideoElement
import org.w3c.dom.CanvasRenderingContext2D

/** Constraints object for front camera (must be single top-level [js] expression). */
internal fun wasmConstraintsUserFacing(): JsAny = js("""({ video: { facingMode: 'user', width: { ideal: 1280 }, height: { ideal: 720 } }, audio: false })""")

/** Constraints object for back camera. */
internal fun wasmConstraintsEnvironmentFacing(): JsAny = js("""({ video: { facingMode: 'environment', width: { ideal: 1920 }, height: { ideal: 1080 } }, audio: false })""")

internal external interface WasmJsPromiseLike : JsAny {
    fun then(
        onFulfilled: (JsAny?) -> JsAny?,
        onRejected: (JsAny?) -> JsAny?,
    ): WasmJsPromiseLike?
}

internal external interface WasmJsMediaDevices : JsAny {
    fun getUserMedia(constraints: JsAny): WasmJsPromiseLike
}

internal external interface WasmNavigatorWithMedia : JsAny {
    val mediaDevices: WasmJsMediaDevices?
}

internal external interface WasmHtmlVideoSrc : JsAny {
    var srcObject: JsAny?
    fun play(): WasmJsPromiseLike?
}

internal external interface WasmJsErrorLike : JsAny {
    val message: String?
}

internal fun wasmNavigatorMediaDevices(): WasmJsMediaDevices? =
    window.navigator.unsafeCast<WasmNavigatorWithMedia>().mediaDevices

internal fun wasmSetVideoSrcObject(video: HTMLVideoElement, stream: JsAny) {
    video.unsafeCast<WasmHtmlVideoSrc>().srcObject = stream
}

internal fun wasmVideoPlayPromise(video: HTMLVideoElement): WasmJsPromiseLike? =
    video.unsafeCast<WasmHtmlVideoSrc>().play()

internal fun wasmDrawVideoOnCanvas(canvas: HTMLCanvasElement, video: HTMLVideoElement): Boolean {
    val w = video.videoWidth
    val h = video.videoHeight
    if (w <= 0 || h <= 0) return false
    canvas.width = w
    canvas.height = h
    val ctxRaw = canvas.getContext("2d") ?: return false
    @Suppress("USELESS_IS_CHECK")
    val ctx = ctxRaw as? CanvasRenderingContext2D ?: return false
    ctx.drawImage(video, 0.0, 0.0)
    return true
}

internal fun wasmErrorMessage(err: JsAny?): String {
    if (err == null) return "Unknown error"
    return err.unsafeCast<WasmJsErrorLike>().message?.takeIf { it.isNotBlank() }
        ?: "Error"
}
