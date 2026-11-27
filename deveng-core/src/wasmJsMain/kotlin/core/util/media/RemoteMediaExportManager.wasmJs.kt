package core.util.media

import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.js.JsAny
import kotlin.js.unsafeCast
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.HTMLAnchorElement
import org.w3c.dom.url.URL
import org.w3c.files.Blob

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual class RemoteMediaExportManager {

    actual suspend fun shareSingleFileFromUrl(
        fileUrl: String,
        fileName: String,
        mimeType: String,
    ): Boolean {
        return false
    }

    actual suspend fun shareMultipleFilesFromUrls(
        files: List<RemoteMediaFile>,
    ): Boolean {
        return false
    }

    actual suspend fun saveSingleFileFromUrl(
        fileUrl: String,
        fileName: String,
        mimeType: String,
    ): Boolean {
        if (fileUrl.isBlank()) return false
        return runCatching {
            val blob = fetchUrlAsBlobSuspend(fileUrl = fileUrl)
            val objectUrl = URL.createObjectURL(blob)
            try {
                triggerBrowserDownload(objectUrl = objectUrl, fileName = fileName.ifBlank { "download" })
                true
            } finally {
                URL.revokeObjectURL(objectUrl)
            }
        }.getOrDefault(false)
    }

    actual suspend fun saveMultipleFilesFromUrls(
        files: List<RemoteMediaFile>,
    ): Int {
        if (files.isEmpty()) return 0
        var count = 0
        for (remote in files) {
            if (saveSingleFileFromUrl(
                    fileUrl = remote.fileUrl,
                    fileName = remote.fileName,
                    mimeType = remote.mimeType,
                )
            ) {
                count++
            }
        }
        return count
    }
}

private external interface WasmPromiseLike : JsAny {
    fun then(
        onFulfilled: (JsAny?) -> JsAny?,
        onRejected: (JsAny?) -> JsAny?,
    ): WasmPromiseLike?
}

private external interface WasmWindowFetch : JsAny {
    fun fetch(input: String): WasmPromiseLike?
}

private external interface WasmFetchResponse : JsAny {
    fun blob(): WasmPromiseLike?
}

private fun windowFetchPromise(url: String): WasmPromiseLike? =
    window.unsafeCast<WasmWindowFetch>().fetch(url)

private suspend fun fetchUrlAsBlobSuspend(fileUrl: String): Blob = suspendCoroutine { cont ->
    val fetchPromise = windowFetchPromise(fileUrl)
    if (fetchPromise == null) {
        cont.resumeWithException(IllegalStateException("fetch unavailable"))
        return@suspendCoroutine
    }
    fetchPromise.then(
        onFulfilled = { response: JsAny? ->
            if (response == null) {
                cont.resumeWithException(IllegalStateException("null response"))
            } else {
                val blobPromise = response.unsafeCast<WasmFetchResponse>().blob()
                if (blobPromise == null) {
                    cont.resumeWithException(IllegalStateException("null blob promise"))
                } else {
                    blobPromise.then(
                        onFulfilled = { blob: JsAny? ->
                            if (blob == null) {
                                cont.resumeWithException(IllegalStateException("null blob"))
                            } else {
                                cont.resume(blob.unsafeCast<Blob>())
                            }
                            null
                        },
                        onRejected = { err: JsAny? ->
                            cont.resumeWithException(Exception(err?.toString() ?: "blob failed"))
                            null
                        },
                    )
                }
            }
            null
        },
        onRejected = { err: JsAny? ->
            cont.resumeWithException(Exception(err?.toString() ?: "fetch failed"))
            null
        },
    )
}

private fun triggerBrowserDownload(objectUrl: String, fileName: String) {
    val anchor = document.createElement("a").unsafeCast<HTMLAnchorElement>()
    anchor.href = objectUrl
    anchor.download = fileName
    anchor.rel = "noopener"
    val body = document.body ?: return
    body.appendChild(anchor)
    anchor.click()
    body.removeChild(anchor)
}
