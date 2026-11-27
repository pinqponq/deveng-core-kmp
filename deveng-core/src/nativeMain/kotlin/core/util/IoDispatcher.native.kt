package core.util

import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

actual val IoDispatcher: CoroutineContext = Dispatchers.IO
