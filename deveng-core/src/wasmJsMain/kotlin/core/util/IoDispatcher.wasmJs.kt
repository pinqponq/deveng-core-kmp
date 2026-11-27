package core.util

import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.Dispatchers

actual val IoDispatcher: CoroutineContext = Dispatchers.Default
