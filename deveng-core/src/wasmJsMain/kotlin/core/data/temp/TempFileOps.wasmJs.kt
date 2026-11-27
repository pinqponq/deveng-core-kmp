package core.data.temp

/**
 * WASM/JS: in-memory temp "files" keyed by full path so [TempFileRepositoryImpl] works for camera roll / triage.
 * Data is lost on full page reload (same class of persistence as typical web session storage usage elsewhere).
 */
actual class TempFileOps actual constructor() {

    actual fun ensureDir(path: String): Unit = Unit

    actual fun writeFile(path: String, bytes: ByteArray) {
        wasmTempFileStore[keyFor(path)] = bytes
    }

    actual fun readFile(path: String): ByteArray? = wasmTempFileStore[keyFor(path)]

    actual fun deleteFile(path: String) {
        wasmTempFileStore.remove(keyFor(path))
    }

    actual fun fileExists(path: String): Boolean = wasmTempFileStore.containsKey(keyFor(path))

    private fun keyFor(path: String): String = path.replace('\\', '/')
}

private val wasmTempFileStore = mutableMapOf<String, ByteArray>()
