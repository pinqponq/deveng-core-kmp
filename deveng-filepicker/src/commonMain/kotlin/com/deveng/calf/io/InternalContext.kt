package com.deveng.calf.io

import com.deveng.calf.core.PlatformContext

/**
 * Resolves the platform context internally.
 * On Android, returns the context from [AndroidContextHolder].
 * On all other platforms, returns [PlatformContext.INSTANCE].
 */
internal expect fun resolveContext(): PlatformContext
