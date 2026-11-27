package global.deveng.core

import androidx.compose.runtime.Composable
import core.util.multiplatform.MultiPlatformUtils

/**
 * Creates the platform-specific [MultiPlatformUtils] instance for the current composition.
 * Returns null on platforms where the sample does not create one.
 */
@Composable
expect fun rememberSampleMultiPlatformUtils(): MultiPlatformUtils?
