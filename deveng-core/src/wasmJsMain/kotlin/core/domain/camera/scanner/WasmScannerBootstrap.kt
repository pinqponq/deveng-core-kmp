@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package core.domain.camera.scanner

/**
 * Installs the jsQR-specific JS the QR scanner plugin needs on top of the general
 * [core.domain.camera.ensureWasmCameraBootstrap]:
 *
 * - A `<script>` tag loading `jsQR` from jsDelivr.
 * - `window.cameraKScanQr` helper consumed by [jsScanQr], which runs the
 *   drawImage → getImageData → jsQR pipeline inside one JS call to avoid marshalling a
 *   `Uint8ClampedArray` back to Kotlin/Wasm on every frame.
 *
 * Idempotent via a `window.__cameraKScannerBootstrapped` guard. The jsQR load is async;
 * `cameraKScanQr` returns `null` until `jsQR` is defined on `window`, which the scanner's
 * 150 ms polling loop tolerates. An `onerror` handler logs to the console if the CDN
 * request fails (e.g. offline or blocked by a firewall).
 */
@JsFun(
    """() => {
    if (window.__cameraKScannerBootstrapped) return;
    window.__cameraKScannerBootstrapped = true;

    var script = document.createElement('script');
    script.src = 'https://cdn.jsdelivr.net/npm/jsqr@1.4.0/dist/jsQR.js';
    script.async = true;
    script.onerror = function() {
        console.error('[deveng-core] Failed to load jsQR from CDN; QR scanning will not work.');
    };
    document.head.appendChild(script);

    window.cameraKScanQr = function(video, canvas) {
        if (typeof jsQR !== 'function') return null;
        if (video.readyState < 2) return null;
        var w = video.videoWidth, h = video.videoHeight;
        if (!w || !h) return null;
        canvas.width = w;
        canvas.height = h;
        var ctx = canvas.getContext('2d');
        ctx.drawImage(video, 0, 0, w, h);
        var imageData = ctx.getImageData(0, 0, w, h);
        var code = jsQR(imageData.data, w, h, { inversionAttempts: 'dontInvert' });
        return code ? code.data : null;
    };
}""",
)
private external fun ensureWasmScannerBootstrapImpl()

internal fun ensureWasmScannerBootstrap() = ensureWasmScannerBootstrapImpl()
