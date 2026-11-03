package core.crash.data

import core.crash.domain.PlatformCrashReporter

internal actual fun createPlatformCrashReporter(): PlatformCrashReporter = NoOpPlatformCrashReporter
