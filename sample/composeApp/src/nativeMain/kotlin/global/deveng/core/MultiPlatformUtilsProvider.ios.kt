package global.deveng.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import core.util.multiplatform.MultiPlatformUtils

@Composable
actual fun rememberSampleMultiPlatformUtils(): MultiPlatformUtils? = remember { MultiPlatformUtils() }
