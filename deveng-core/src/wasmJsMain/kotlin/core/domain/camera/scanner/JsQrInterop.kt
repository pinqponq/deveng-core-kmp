@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package core.domain.camera.scanner

import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLVideoElement

/**
 * Single-call JS helper that:
 *
 * 1. Draws the current `<video>` frame onto `<canvas>`.
 * 2. Reads the canvas's `ImageData`.
 * 3. Runs `jsQR` over it.
 * 4. Returns the decoded string, or `null` if no QR code is visible.
 *
 * Keeping the drawImage → getImageData → jsQR pipeline inside one JS call avoids the cost of
 * marshalling a `Uint8ClampedArray` back to Kotlin/Wasm on every frame.
 *
 * The backing `window.cameraKScanQr` helper and the jsQR `<script>` tag are installed by
 * [ensureWasmCameraBootstrap], so no host page setup is required.
 */
@JsFun("(video, canvas) => window.cameraKScanQr(video, canvas)")
private external fun jsScanQrImpl(
    video: HTMLVideoElement,
    canvas: HTMLCanvasElement,
): JsString?

internal fun jsScanQr(
    video: HTMLVideoElement,
    canvas: HTMLCanvasElement,
): JsString? = jsScanQrImpl(video, canvas)
