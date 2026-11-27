package core.util

import kotlin.coroutines.CoroutineContext

/**
 * Dispatcher for I/O or other off-main-thread work. Use with [kotlinx.coroutines.withContext].
 * - Android, Desktop, Native: [kotlinx.coroutines.Dispatchers.IO] (Native requires `import kotlinx.coroutines.IO`)
 * - WASM: [kotlinx.coroutines.Dispatchers.Default] (IO not available)
 */
expect val IoDispatcher: CoroutineContext
