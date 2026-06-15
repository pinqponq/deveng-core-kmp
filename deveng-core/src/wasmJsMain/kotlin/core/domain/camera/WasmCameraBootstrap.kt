@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package core.domain.camera

/**
 * Installs the CSS rules the BlendMode.Clear hole-punch needs to reveal the DOM `<video>`
 * mounted behind the Compose canvas:
 *
 * - `html { background: #000 }` — black backdrop visible if no video is playing yet
 * - `body { background: transparent }` — let the Skia canvas show through to the video
 * - `canvas { background: transparent !important }` — let `BlendMode.Clear` pass through
 *
 * Idempotent via a `window.__devengWasmCameraBootstrapped` guard so repeated calls are
 * no-ops. [core.domain.camera.controller.CameraController] invokes this in its init block
 * so downstream apps don't need to edit their `index.html` — adding the Gradle dependency
 * is enough.
 *
 * Plugins that need additional JS (e.g. the QR scanner's jsQR loader) install their own
 * idempotent bootstrap on top of this one.
 */
@JsFun(
    """() => {
    if (window.__devengWasmCameraBootstrapped) return;
    window.__devengWasmCameraBootstrapped = true;

    var style = document.createElement('style');
    style.textContent =
        'html{background:#000;}' +
        'body{background:transparent;}' +
        'canvas{background:transparent !important;}';
    document.head.appendChild(style);
}""",
)
private external fun ensureWasmCameraBootstrapImpl()

internal fun ensureWasmCameraBootstrap() = ensureWasmCameraBootstrapImpl()
