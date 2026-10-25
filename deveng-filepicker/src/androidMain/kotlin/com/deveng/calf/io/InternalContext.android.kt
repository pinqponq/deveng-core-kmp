package com.deveng.calf.io

import com.deveng.calf.core.PlatformContext

internal actual fun resolveContext(): PlatformContext = AndroidContextHolder.context
