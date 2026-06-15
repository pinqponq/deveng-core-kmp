@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package core.util.image

/**
 * Triggers a browser "Save As" / auto-download of the given bytes by:
 *
 * 1. Decoding the base64 payload via `atob` into a `Uint8Array`.
 * 2. Wrapping it in a `Blob` with `image/jpeg`.
 * 3. Creating a synthetic `<a download="...">` element, clicking it, and removing it.
 * 4. Revoking the object URL after a 5-second delay so the download has time to start.
 *
 * Why base64 transit: Kotlin/Wasm doesn't yet have a clean `ByteArray → Uint8Array`
 * bridge in this codebase. Base64-encode on the Kotlin side, `atob` on the JS side; ~33%
 * transfer overhead is negligible for one-shot per-save use.
 *
 * @return `null` on success, an error message string on failure (e.g. browser blocked the
 *   download, or the synthetic anchor click was suppressed).
 */
@JsFun(
    """(filename, base64) => {
    try {
        var binary = atob(base64);
        var len = binary.length;
        var bytes = new Uint8Array(len);
        for (var i = 0; i < len; i++) bytes[i] = binary.charCodeAt(i);
        var blob = new Blob([bytes], { type: 'image/jpeg' });
        var url = URL.createObjectURL(blob);
        var a = document.createElement('a');
        a.href = url;
        a.download = filename;
        a.style.display = 'none';
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        setTimeout(function() { URL.revokeObjectURL(url); }, 5000);
        return null;
    } catch (e) {
        return (e && e.message) ? e.message : String(e);
    }
}""",
)
private external fun jsTriggerPhotoDownloadImpl(filename: String, base64: String): JsString?

internal fun jsTriggerPhotoDownload(filename: String, base64: String): JsString? =
    jsTriggerPhotoDownloadImpl(filename, base64)
