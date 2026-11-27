package core.crash.data

import core.crash.domain.CrashReporter
import core.crash.domain.PlatformCrashReporter
import core.crash.domain.model.CrashReportingOptions

/**
 * Builds a [CrashReporter] without Koin. [platformCrashReporter] defaults to Firebase Crashlytics on
 * Android, the Swift bridge on iOS and a no-op reporter on desktop and web.
 */
fun createCrashReporter(
    crashReportingOptions: CrashReportingOptions,
    installIdProvider: () -> String,
    platformCrashReporter: PlatformCrashReporter = createPlatformCrashReporter(),
): CrashReporter {
    if (!crashReportingOptions.isEnabled) return NoOpCrashReporter

    return CrashReporterImpl(
        crashReportingOptions = crashReportingOptions,
        platformCrashReporter = platformCrashReporter,
        installIdProvider = installIdProvider,
    )
}
