package core.domain.camera.ios

/**
 * iOS camera preview diagnostics — filter Xcode with **RindlePreviewDbg**.
 * println only (avoid NSLog %@ interop traps on Kotlin/Native).
 */
internal object IosPreviewDbgLog {
    fun log(message: String) {
        println("[RindlePreviewDbg] $message")
    }

    inline fun logSafe(buildMessage: () -> String) {
        val line = runCatching { buildMessage() }.getOrElse { error ->
            "probeError=${error.message ?: error::class.simpleName}"
        }
        log(line)
    }
}
