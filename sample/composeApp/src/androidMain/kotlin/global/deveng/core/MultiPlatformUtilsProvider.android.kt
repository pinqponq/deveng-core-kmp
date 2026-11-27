package global.deveng.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import core.util.multiplatform.MultiPlatformUtils

@Composable
actual fun rememberSampleMultiPlatformUtils(): MultiPlatformUtils? {
    val applicationContext = LocalContext.current.applicationContext
    return remember(applicationContext) { MultiPlatformUtils(applicationContext) }
}
