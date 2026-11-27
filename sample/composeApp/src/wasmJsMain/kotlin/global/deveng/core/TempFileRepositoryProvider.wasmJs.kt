package global.deveng.core

import core.domain.temp.TempFileRepository

/**
 * WASM/JS: temp file storage isn't available in the browser sandbox, so the sample app
 * can't provide a working repository here. The function throws if called; the sample
 * currently doesn't call it on WASM.
 */
actual fun getTempFileRepository(applicationContext: Any?): TempFileRepository {
    throw UnsupportedOperationException("Temp file storage not supported on WASM")
}
